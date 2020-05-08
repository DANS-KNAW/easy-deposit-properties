/**
 * Copyright (C) 2019 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.properties.app.legacyImport

import better.files.File
import cats.instances.either._
import cats.instances.option._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.traverse._
import nl.knaw.dans.easy.properties.ApplicationErrorOr
import nl.knaw.dans.easy.properties.Command.FeedBackMessage
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentType, InputContentType }
import nl.knaw.dans.easy.properties.app.model.curator.{ Curator, InputCurator }
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.{ InputIsCurationPerformed, IsCurationPerformed }
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.{ InputIsCurationRequired, IsCurationRequired }
import nl.knaw.dans.easy.properties.app.model.isnewversion.{ InputIsNewVersion, IsNewVersion }
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.StateLabel.StateLabel
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, State, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DoiAction, DoiActionEvent, DoiRegisteredEvent, Origin, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.{ MutationErrorOr, Repository }
import nl.knaw.dans.easy.{ DataciteService, DataciteServiceException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.string._
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.lang.BooleanUtils
import org.joda.time.DateTime

class ImportProps(repository: Repository, interactor: Interactor, datacite: DataciteService, testMode: Boolean) extends DebugEnhancedLogging {

  private var newPropertiesProvided = false

  def loadDepositProperties(file: File): ApplicationErrorOr[FeedBackMessage] = {
    newPropertiesProvided = false
    for {
      _ <- propsFileExists(file)
      _ <- propsFileReadable(file)
      _ <- propsFileWritable(file)
      properties = readDepositProperties(file)
      depositId <- getDepositId(file)
      creationTime = () => new DateTime(file.attributes.creationTime().toMillis)
      lastModifiedTime = new DateTime(file.attributes.lastModifiedTime().toMillis)
      _ <- storeDeposit(loadDeposit(file.parent, depositId, creationTime, properties))
      state <- storeState(depositId, loadState(depositId, lastModifiedTime, properties))
      _ <- loadIngestStep(depositId, lastModifiedTime, properties, state.label).traverse(storeIngestStep(depositId))
      doi <- storeIdentifier(depositId, loadDoi(depositId, lastModifiedTime, properties))
      _ <- storeIdentifier(depositId, loadUrn(depositId, lastModifiedTime, properties))
      _ <- storeIdentifier(depositId, loadFedoraIdentifier(depositId, lastModifiedTime, properties))
      _ <- storeIdentifier(depositId, loadBagStoreIdentifier(depositId, lastModifiedTime, properties))
      _ <- storeDoiRegistered(depositId, loadDoiRegistered(depositId, lastModifiedTime, properties, doi.idValue))
      _ <- storeDoiAction(depositId, loadDoiAction(depositId, lastModifiedTime, properties))
      _ <- loadCurator(depositId, lastModifiedTime, properties).traverse(storeCurator(depositId, _))
      _ <- loadIsNewVersion(lastModifiedTime, properties).traverse(storeIsNewVersion(depositId, _))
      _ <- loadIsCurationRequired(lastModifiedTime, properties).traverse(storeIsCurationRequired(depositId, _))
      _ <- loadIsCurationPerformed(lastModifiedTime, properties).traverse(storeIsCurationPerformed(depositId, _))
      _ <- loadSpringfield(depositId, lastModifiedTime, properties).traverse(storeSpringfield(depositId, _))
      _ <- loadContentType(depositId, lastModifiedTime, properties).traverse(storeContentType(depositId, _))
      _ = savePropertiesIfChanged(depositId, properties)
    } yield s"Loading properties for deposit $depositId succeeded."
  }

  private def propsFileExists(file: File): LoadPropsErrorOr[Unit] = {
    Either.catchOnly[IllegalArgumentException] { require(file.exists) }
      .leftMap(_ => NoSuchPropertiesFileError(file))
  }

  private def propsFileReadable(file: File): LoadPropsErrorOr[Unit] = {
    Either.catchOnly[IllegalArgumentException] { require(file.isReadable) }
      .leftMap(_ => PropertiesFileNotReadableError(file))
  }

  private def propsFileWritable(file: File): LoadPropsErrorOr[Unit] = {
    Either.catchOnly[IllegalArgumentException] { require(file.isWriteable) }
      .leftMap(_ => PropertiesFileNotWritableError(file))
  }

  private def readDepositProperties(file: File): PropertiesConfiguration = {
    new PropertiesConfiguration() {
      setDelimiterParsingDisabled(true)
      setFile(file.toJava)
      load(file.toJava)
    }
  }

  private def getDepositId(file: File): LoadPropsErrorOr[DepositId] = {
    file.parentOption
      .map(_.name.asRight)
      .getOrElse { NoSuchParentDirError(file).asLeft }
      .flatMap(s => s.toUUID.leftMap(_ => NoDepositIdError(s)))
  }

  private def storeProp[T](depositId: DepositId, props: PropertiesConfiguration, key: String)(value: T): T = {
    if (testMode) logger.info(s"[TESTMODE] store property $key -> $value")
    else {
      logger.info(s"[$depositId] store property $key -> $value")
      props.setProperty(key, value)
    }

    newPropertiesProvided = true
    value
  }

  private def storeProp[T](depositId: DepositId, props: PropertiesConfiguration, key: String, transform: T => String)(value: T): T = {
    storeProp(depositId, props, key)(transform(value))

    value
  }

  private def renameProp[T](depositId: DepositId, props: PropertiesConfiguration, oldKey: String, newKey: String)(value: T): T = {
    if (testMode) logger.info(s"[TESTMODE] rename property $oldKey to $newKey; value $value stays the same")
    else {
      logger.info(s"[$depositId] rename property $oldKey to $newKey; value $value stays the same")
      props.setProperty(newKey, value)
      props.clearProperty(oldKey)
    }

    newPropertiesProvided = true
    value
  }

  private def loadDeposit(deposit: File, depositId: DepositId, defaultCreationTime: () => Timestamp, props: PropertiesConfiguration): Deposit = {
    val bagName = getProp("bag-store.bag-name")(props).orElse {
      retrieveBagNameFromFilesystem(deposit)
        .map(storeProp(depositId, props, "bag-store.bag-name"))
    }
    val creationTimestamp = getProp("creation.timestamp")(props)
      .map(s => Either.catchOnly[IllegalArgumentException] { DateTime.parse(s) }
        .getOrElse {
          storeProp(depositId, props, "creation.timestamp") {
            interactor.ask(s => DateTime.parse(s))(s"Invalid value for creation timestamp for deposit $depositId. What value should this be?")
          }
        })
      .getOrElse {
        storeProp(depositId, props, "creation.timestamp")(defaultCreationTime())
      }
    val depositorId = getProp("depositor.userId")(props)
      .getOrElse {
        storeProp(depositId, props, "depositor.userId") {
          interactor.ask(s"Could not find the depositor for deposit $depositId. What value should this be?")
        }
      }
    val origin = getOrAskEnumProp(Origin, "deposit.origin", "origin", props, depositId)

    Deposit(depositId, bagName, creationTimestamp, depositorId, origin)
  }

  private def retrieveBagNameFromFilesystem(deposit: File): Option[String] = {
    val directories = deposit.children.filter(_.isDirectory).toList
    if (directories.size == 1)
      directories.headOption.map(_.name)
    else
      none
  }

  private def loadState(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): InputState = {
    val label = getOrAskEnumProp(StateLabel, "state.label", "state label", props, depositId)
    val description = getProp("state.description")(props)
      .getOrElse {
        storeProp(depositId, props, "state.description") {
          interactor.ask(s"Could not find the state description for deposit $depositId. What value should this be?")
        }
      }

    InputState(label, description, timestamp)
  }

  private def getOrAskEnumProp(enum: Enumeration, propertyKey: FeedBackMessage, propertyDescription: FeedBackMessage, props: PropertiesConfiguration, depositId: DepositId): enum.Value = {
    getEnumProp(propertyKey)(enum)(props)
      .getOrElse {
        storeProp(depositId, props, propertyKey) {
          interactor.ask(enum)(s"Invalid $propertyDescription found for deposit $depositId. What value should this be?")
        }.some
      }
      .getOrElse {
        storeProp(depositId, props, propertyKey) {
          interactor.ask(enum)(s"Could not find the $propertyDescription for deposit $depositId. What value should this be?")
        }
      }
  }

  private def loadIngestStep(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration, stateLabel: StateLabel): Option[InputIngestStep] = {
    getEnumProp("deposit.ingest.current-step")(IngestStepLabel)(props)
      .getOrElse {
        storeProp(depositId, props, "deposit.ingest.current-step") {
          interactor.ask(IngestStepLabel)(s"Invalid current-step label found for deposit $depositId. What value should this be?")
        }.some
      }
      .orElse {
        if (stateLabel == StateLabel.ARCHIVED)
          storeProp(depositId, props, "deposit.ingest.current-step")(IngestStepLabel.COMPLETED).some
        else none
      }
      .map(InputIngestStep(_, timestamp))
  }

  private def loadDoi(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): InputIdentifier = {
    val doi = getProp("identifier.doi")(props)
      .getOrElse {
        storeProp(depositId, props, "identifier.doi") {
          interactor.ask(s"Could not find DOI for deposit $depositId. What value should this be?")
        }
      }

    InputIdentifier(IdentifierType.DOI, doi, timestamp)
  }

  private def loadUrn(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): InputIdentifier = {
    val urn = getProp("identifier.urn")(props)
      .getOrElse {
        storeProp(depositId, props, "identifier.urn") {
          interactor.ask(s"Could not find URN for deposit $depositId. What value should this be?")
        }
      }

    InputIdentifier(IdentifierType.URN, urn, timestamp)
  }

  private def loadFedoraIdentifier(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): InputIdentifier = {
    val fedoraId = getProp("identifier.fedora")(props)
      .getOrElse {
        storeProp(depositId, props, "identifier.fedora") {
          interactor.ask(s"Could not find Fedora identifier for deposit $depositId. What value should this be?")
        }
      }

    InputIdentifier(IdentifierType.FEDORA, fedoraId, timestamp)
  }

  private def loadBagStoreIdentifier(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): InputIdentifier = {
    val bagId = getProp("bag-store.bag-id")(props).getOrElse {
      storeProp(depositId, props, "bag-store.bag-id")(depositId).toString
    }

    InputIdentifier(IdentifierType.BAG_STORE, bagId, timestamp)
  }

  private def loadDoiRegistered(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration, doi: String): DoiRegisteredEvent = {
    val registered = getProp("identifier.dans-doi.registered")(props)
      .flatMap(s => Option(BooleanUtils.toBoolean(s)))
      .getOrElse {
        storeProp[Boolean](depositId, props, "identifier.dans-doi.registered", (b: Boolean) => BooleanUtils.toStringYesNo(b)) {
          Either.catchOnly[DataciteServiceException] { datacite.doiExists(doi) }
            .getOrElse {
              interactor.ask(s => BooleanUtils.toBoolean(s))(s"Could not find whether doi '$doi' is registered for deposit $depositId, neither could DataCite be contacted. What value should this be?")
            }
        }
      }

    DoiRegisteredEvent(registered, timestamp)
  }

  private def loadDoiAction(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): DoiActionEvent = {
    val doiAction = getEnumProp("identifier.dans-doi.action")(DoiAction)(props)
      .getOrElse {
        storeProp(depositId, props, "identifier.dans-doi.action") {
          interactor.ask(DoiAction)(s"Invalid dans-doi action found for deposit $depositId. What value should this be?")
        }.some
      }
      .getOrElse {
        storeProp(depositId, props, "identifier.dans-doi.action")(DoiAction.CREATE) // if not set, use 'create' as default
      }

    DoiActionEvent(doiAction, timestamp)
  }

  private def loadCurator(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): Option[InputCurator] = {
    val userId = getProp("curation.datamanager.userId")(props)
      .orElse {
        getProp("datamanager.userId")(props)
          .map(renameProp(depositId, props, "datamanager.userId", "curation.datamanager.userId"))
      }
    val email = getProp("curation.datamanager.email")(props)
      .orElse {
        getProp("datamanager.email")(props)
          .map(renameProp(depositId, props, "datamanager.email", "curation.datamanager.email"))
      }

    (userId, email).mapN(InputCurator(_, _, timestamp))
  }

  private def loadIsNewVersion(timestamp: Timestamp, props: PropertiesConfiguration): Option[InputIsNewVersion] = {
    for {
      isNewVersionString <- Option(props.getString("curation.is-new-version"))
      isNewVersion = BooleanUtils.toBoolean(isNewVersionString)
    } yield InputIsNewVersion(isNewVersion, timestamp)
  }

  private def loadIsCurationRequired(timestamp: Timestamp, props: PropertiesConfiguration): Option[InputIsCurationRequired] = {
    for {
      curationRequiredString <- Option(props.getString("curation.required"))
      curationRequired = BooleanUtils.toBoolean(curationRequiredString)
    } yield InputIsCurationRequired(curationRequired, timestamp)
  }

  private def loadIsCurationPerformed(timestamp: Timestamp, props: PropertiesConfiguration): Option[InputIsCurationPerformed] = {
    for {
      curationPerformedString <- Option(props.getString("curation.performed"))
      curationPerformed = BooleanUtils.toBoolean(curationPerformedString)
    } yield InputIsCurationPerformed(curationPerformed, timestamp)
  }

  private def loadSpringfield(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): Option[InputSpringfield] = {
    for {
      domain <- getProp("springfield.domain")(props)
      user <- getProp("springfield.user")(props)
      collection <- getProp("springfield.collection")(props)
      playMode <- getEnumProp("springfield.playmode")(SpringfieldPlayMode)(props)
        .getOrElse {
          storeProp(depositId, props, "springfield.playmode") {
            interactor.ask(SpringfieldPlayMode)(s"Invalid play mode found for deposit $depositId. What value should this be?")
          }.some
        }
    } yield InputSpringfield(domain, user, collection, playMode, timestamp)
  }

  private def loadContentType(depositId: DepositId, timestamp: Timestamp, props: PropertiesConfiguration): Option[InputContentType] = {
    getProp("easy-sword2.client-message.content-type")(props)
      .orElse {
        getProp("contentType")(props)
          .map(renameProp(depositId, props, "contentType", "easy-sword2.client-message.content-type"))
      }
      .map(InputContentType(_, timestamp))
  }

  private def getProp(key: String)(props: PropertiesConfiguration): Option[String] = {
    Option(props.getString(key))
  }

  private def getEnumProp(key: String)(enum: Enumeration)(props: PropertiesConfiguration): LoadPropsErrorOr[Option[enum.Value]] = {
    getProp(key)(props).traverse(s => parseEnumValue(enum)(s))
  }

  private def parseEnumValue(enum: Enumeration)(s: String): LoadPropsErrorOr[enum.Value] = {
    Either.catchOnly[NoSuchElementException] { enum.withName(s) }
      .leftMap(_ => IllegalValueError(s, enum))
  }

  private def savePropertiesIfChanged(depositId: DepositId, props: PropertiesConfiguration): Unit = {
    if (newPropertiesProvided) {
      if (testMode) logger.info("[TESTMODE] save deposit properties")
      else {
        logger.info(s"[$depositId] save deposit properties")
        props.save()
      }
    }
  }

  private def storeDeposit(deposit: Deposit): MutationErrorOr[Deposit] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store deposit $deposit")
      deposit.asRight
    }
    else {
      logger.info(s"[${ deposit.id }] store deposit $deposit")
      repository.deposits.store(deposit)
    }
  }

  private def storeState(depositId: DepositId, state: InputState): MutationErrorOr[State] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store state $state")
      state.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store state $state")
      repository.states.store(depositId, state)
    }
  }

  private def storeIngestStep(depositId: DepositId)(ingestStep: InputIngestStep): MutationErrorOr[IngestStep] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store ingest state $ingestStep")
      ingestStep.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store ingest step $ingestStep")
      repository.ingestSteps.store(depositId, ingestStep)
    }
  }

  private def storeIdentifier(depositId: DepositId, identifier: InputIdentifier): MutationErrorOr[Identifier] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store identifier $identifier")
      identifier.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store identifier $identifier")
      repository.identifiers.store(depositId, identifier)
    }
  }

  private def storeDoiRegistered(depositId: DepositId, doiRegisteredEvent: DoiRegisteredEvent): MutationErrorOr[DoiRegisteredEvent] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store doi register event $doiRegisteredEvent")
      doiRegisteredEvent.asRight
    }
    else {
      logger.info(s"[$depositId] store doi register event $doiRegisteredEvent")
      repository.doiRegistered.store(depositId, doiRegisteredEvent)
    }
  }

  private def storeDoiAction(depositId: DepositId, doiActionEvent: DoiActionEvent): MutationErrorOr[DoiActionEvent] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store doi action event $doiActionEvent")
      doiActionEvent.asRight
    }
    else {
      logger.info(s"[$depositId] store doi action event $doiActionEvent")
      repository.doiAction.store(depositId, doiActionEvent)
    }
  }

  private def storeCurator(depositId: DepositId, curator: InputCurator): MutationErrorOr[Curator] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store curator $curator")
      curator.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store curator $curator")
      repository.curator.store(depositId, curator)
    }
  }

  private def storeIsNewVersion(depositId: DepositId, isNewVersion: InputIsNewVersion): MutationErrorOr[IsNewVersion] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store is-new-version event $isNewVersion")
      isNewVersion.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store is-new-version event $isNewVersion")
      repository.isNewVersion.store(depositId, isNewVersion)
    }
  }

  private def storeIsCurationRequired(depositId: DepositId, isCurationRequired: InputIsCurationRequired): MutationErrorOr[IsCurationRequired] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store is-curation-required event $isCurationRequired")
      isCurationRequired.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store is-curation-required event $isCurationRequired")
      repository.isCurationRequired.store(depositId, isCurationRequired)
    }
  }

  private def storeIsCurationPerformed(depositId: DepositId, isCurationPerformed: InputIsCurationPerformed): MutationErrorOr[IsCurationPerformed] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store is-curation-performed event $isCurationPerformed")
      isCurationPerformed.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store is-curation-performed event $isCurationPerformed")
      repository.isCurationPerformed.store(depositId, isCurationPerformed)
    }
  }

  private def storeSpringfield(depositId: DepositId, springfield: InputSpringfield): MutationErrorOr[Springfield] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store springfield $springfield")
      springfield.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store springfield $springfield")
      repository.springfield.store(depositId, springfield)
    }
  }

  private def storeContentType(depositId: DepositId, contentType: InputContentType): MutationErrorOr[ContentType] = {
    if (testMode) {
      logger.info(s"[TESTMODE] store content type $contentType")
      contentType.toOutput("id").asRight
    }
    else {
      logger.info(s"[$depositId] store content type $contentType")
      repository.contentType.store(depositId, contentType)
    }
  }
}
