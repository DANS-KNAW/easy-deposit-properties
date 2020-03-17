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
package nl.knaw.dans.easy.properties

import java.net.URL
import java.util.{ TimeZone, UUID }

import better.files.File
import cats.instances.list._
import cats.scalatest.{ EitherMatchers, EitherValues }
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.legacyImport.{ ImportProps, Interactor, NoDepositIdError, NoSuchPropertiesFileError }
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentType, ContentTypeValue, InputContentType }
import nl.knaw.dans.easy.properties.app.model.curation.{ Curation, InputCuration }
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, State, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DoiAction, DoiActionEvent, DoiRegisteredEvent, Origin, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CurationDao, DepositDao, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, Repository, SpringfieldDao, StateDao }
import nl.knaw.dans.easy.properties.fixture.{ FileSystemSupport, TestSupportFixture }
import nl.knaw.dans.easy.{ DataciteService, DataciteServiceConfiguration, DataciteServiceException }
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalamock.scalatest.MockFactory

import scala.collection.JavaConverters._

class ImportPropsSpec extends TestSupportFixture
  with FileSystemSupport
  with MockFactory
  with EitherMatchers with EitherValues {

  override def beforeEach(): Unit = {
    super.beforeEach()

    File(getClass.getResource("/legacy-import").toURI) copyTo testDir
  }

  private class MockDataciteService extends DataciteService(new DataciteServiceConfiguration() {
    setDatasetResolver(new URL("http://does.not.exist.dans.knaw.nl"))
  })

  private val depositDao = mock[DepositDao]
  private val stateDao = mock[StateDao]
  private val ingestStepDao = mock[IngestStepDao]
  private val identifierDao = mock[IdentifierDao]
  private val doiRegisteredDao = mock[DoiRegisteredDao]
  private val doiActionDao = mock[DoiActionDao]
  private val curationDao = mock[CurationDao]
  private val springfieldDao = mock[SpringfieldDao]
  private val contentTypeDao = mock[ContentTypeDao]
  private val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curationDao, springfieldDao, contentTypeDao)
  private val interactor = mock[Interactor]
  private val datacite = mock[MockDataciteService]
  private val timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"))
  private val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
  private val importProps = new ImportProps(repo, interactor, datacite, testMode = false)

  "loadDepositProperties" should "read the given deposit.properties file and call the repository on it" in {
    val file = testDir / "readProps" / "bf729483-5d9b-4509-a8f2-91db639fb52f" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "find the bag name based on the file system when it isn't given in deposit.properties" in {
    val file = testDir / "file_system_bag_name" / "ae618372-4c8a-33f8-97e1-80ca528ea41e" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "my-bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "my-bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "bag-store.bag-name" -> "my-bag"
    )
  }

  it should "don't find the bag name based on the file system when it isn't given in deposit.properties and multiple directories are siblings of this file" in {
    val file = testDir / "file_system_bag_name" / "9d507261-3b79-22e7-86d0-6fb9417d930d" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, none, time, "user001", Origin.API))) returning Deposit(depositId, none, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file).toMap should not contain key
    "bag-store.bag-name"
  }

  it should "interact with the user when necessary values don't exist" in {
    val file = testDir / "interact_max" / "0eb8c353-4b41-4db7-9b1c-15e06a69c143" / "deposit.properties"
    val (depositId, creationTime, lastModified) = fileProps(file)

    inSequence {
      expectInteractString("user001")
      expectInteractEnum(Origin)(_.API)
      depositDao.store _ expects where(isDeposit(Deposit(depositId, none, creationTime, "user001", Origin.API))) returning Deposit(depositId, none, creationTime, "user001", Origin.API).asRight
      expectInteractEnum(StateLabel)(_.SUBMITTED)
      expectInteractString("my description")
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my-description", lastModified).asRight
      // no ingestStepDao.store
      expectInteractString("my-doi-value")
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      expectInteractString("my-urn-value")
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      expectInteractString("my-fedora-value")
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, depositId.toString, lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, depositId.toString, lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.CREATE, lastModified)) returning DoiActionEvent(DoiAction.CREATE, lastModified).asRight
      // no curationDao.store
      // no springfieldDao.store
      // no contentTypeDao.store
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain allOf(
      "depositor.userId" -> "user001",
      "state.label" -> "SUBMITTED",
      "state.description" -> "my description",
      "identifier.doi" -> "my-doi-value",
      "identifier.urn" -> "my-urn-value",
      "identifier.fedora" -> "my-fedora-value",
      "bag-store.bag-id" -> depositId.toString,
      "identifier.dans-doi.registered" -> "yes",
      "identifier.dans-doi.action" -> "create",
    )
  }

  it should "interact with the user when the state label is invalid" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "invalid_state_label" / "667ece00-2083-4930-a1ab-f265aca41021" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      expectInteractEnum(StateLabel)(_.SUBMITTED)
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "state.label" -> "SUBMITTED",
    )
  }

  it should "choose ingest step COMPLETED when it is not provided and state = ARCHIVED" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "ingest_step_completed" / "0e00e954-8236-403f-80d3-e67792164b26" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.ARCHIVED, "my description", lastModified)) returning State("my-id", StateLabel.ARCHIVED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.COMPLETED, lastModified)) returning IngestStep("my-id", IngestStepLabel.COMPLETED, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "deposit.ingest.current-step" -> "COMPLETED",
    )
  }

  it should "interact with the user when an invalid value is given for creation.timestamp" in {
    val file = testDir / "invalid_creation_timestamp" / "f2a5c8b6-90ce-783c-dc25-c50e96d2e862" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      expectInteractFunc(time)
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.COMPLETED, lastModified)) returning IngestStep("my-id", IngestStepLabel.COMPLETED, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "creation.timestamp" -> time.toString(),
    )
  }

  it should "find the creation.timestamp on the file itself when it is not given as a property" in {
    val file = testDir / "missing_creation_timestamp" / "f2a5c8b6-90ce-783c-dc25-c50e96d2e862" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)
    val time = new DateTime(file.attributes.creationTime().toMillis)

    props(file).map(_._1) should not contain "creation.timestamp"

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.COMPLETED, lastModified)) returning IngestStep("my-id", IngestStepLabel.COMPLETED, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "creation.timestamp" -> time.toString(),
    )
  }

  it should "interact with the user when an invalid value is given for deposit.ingest.current-step" in {
    val file = testDir / "invalid_ingest_step" / "e194b7a5-7fbd-672b-cb14-b3fd85c1d751" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      expectInteractEnum(IngestStepLabel)(_.VALIDATE)
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.VALIDATE, lastModified)) returning IngestStep("my-id", IngestStepLabel.VALIDATE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "deposit.ingest.current-step" -> "VALIDATE",
    )
  }

  it should "interact with DataCite when the identifier.dans-doi.registered property is not set" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "doi_registered_datacite" / "caa9e50a-a6a9-4cc1-a415-33d8384d4df5" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      datacite.doiExists _ expects "my-doi-value" returning true
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "identifier.dans-doi.registered" -> "yes",
    )
  }

  it should "interact with the user when interaction with DataCite fails" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "doi_registered_interact" / "7eb43c8d-0565-427d-9e6c-1c9c05d8a3f7" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      datacite.doiExists _ expects "my-doi-value" throws new DataciteServiceException("FAIL!!!", 418)
      expectInteractFunc(true)
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "identifier.dans-doi.registered" -> "yes",
    )
  }

  it should "interact with the user when an invalid value is given for identifier.dans-doi.action" in {
    val file = testDir / "invalid_doi_action" / "d083a694-6eac-561a-ba03-a2ec74b0c640" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      expectInteractEnum(DoiAction)(_.CREATE)
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.CREATE, lastModified)) returning DoiActionEvent(DoiAction.CREATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "identifier.dans-doi.action" -> "create",
    )
  }

  it should "interact with the user when an invalid value is given for springfield.playmode" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "invalid_springfield_playmode" / "26d3763e-566c-4860-9abe-ca161d40cd1f" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      expectInteractEnum(SpringfieldPlayMode)(_.MENU)
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.MENU, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.MENU, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "springfield.playmode" -> "menu",
    )
  }

  it should "interact with the user when an invalid value is given for easy-sword2.client-message.content-type" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "invalid_content_type" / "d317ff0d-842f-49f4-8a18-cb396ce85a27" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      expectInteractEnum(ContentTypeValue)(_.ZIP)
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "easy-sword2.client-message.content-type" -> "application/zip",
    )
  }

  it should "find the content type in the 'contentType' property for legacy deposits" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "legacy_content_type" / "d317ff0d-842f-49f4-8a18-cb396ce85a27" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    props(file) should contain(
      "contentType" -> "application/zip"
    )

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain(
      "easy-sword2.client-message.content-type" -> "application/zip",
    )
    props(file) should not contain(
      "contentType" -> "application/zip"
    )
  }

  it should "find the datamanager in the 'datamanager' property for legacy deposits" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "legacy_datamanager" / "bf729483-5d9b-4509-a8f2-91db639fb52f" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    props(file) should contain allOf (
      "datamanager.userId" -> "archie001",
      "datamanager.email" -> "does.not.exists@dans.knaw.nl",
    )

    inSequence {
      depositDao.store _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001", Origin.API))) returning Deposit(depositId, "bag".some, time, "user001", Origin.API).asRight
      stateDao.store _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      ingestStepDao.store _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      identifierDao.store _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      doiRegisteredDao.store _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      doiActionDao.store _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      curationDao.store _ expects(depositId, InputCuration(isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curation("my-id", isNewVersion = none, isRequired = false, isPerformed = false, "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      springfieldDao.store _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      contentTypeDao.store _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right

    props(file) should contain allOf(
      "curation.datamanager.userId" -> "archie001",
      "curation.datamanager.email" -> "does.not.exists@dans.knaw.nl",
    )
    props(file) should contain noneOf(
      "datamanager.userId" -> "archie001",
      "datamanager.email" -> "does.not.exists@dans.knaw.nl",
    )
  }

  it should "fail when the properties file doesn't exist" in {
    val file = (testDir / "no_properties_file" / UUID.randomUUID().toString).createDirectoryIfNotExists(createParents = true) / "deposit.properties"
    file shouldNot exist

    importProps.loadDepositProperties(file).leftValue shouldBe NoSuchPropertiesFileError(file)
  }

  it should "fail when the parent directory is not a valid depositId" in {
    val file = testDir / "invalid_depositId" / "invalid-depositId" / "deposit.properties"

    importProps.loadDepositProperties(file).leftValue shouldBe NoDepositIdError("invalid-depositId")
  }

  private def isDeposit(deposit: Deposit): Deposit => Boolean = d => {
    d.id == deposit.id &&
      d.depositorId == deposit.depositorId &&
      d.bagName == deposit.bagName &&
      d.creationTimestamp.getMillis == deposit.creationTimestamp.getMillis
  }

  private def fileProps(file: File): (DepositId, Timestamp, Timestamp) = {
    (
      UUID.fromString(file.parent.name),
      new DateTime(file.attributes.creationTime().toMillis),
      new DateTime(file.attributes.lastModifiedTime().toMillis),
    )
  }

  private def expectInteractString(s: String) = {
    (interactor.ask(_: String)) expects * returning s
  }

  private def expectInteractEnum(enum: Enumeration)(returnValue: enum.type => enum.Value) = {
    (interactor.ask(_: Enumeration)(_: String)) expects(enum, *) returning returnValue(enum)
  }

  private def expectInteractFunc[T](t: T) = {
    (interactor.ask(_: String => T)(_: String)) expects(*, *) returning t
  }

  private def props(file: File): List[(String, String)] = {
    val props = new PropertiesConfiguration() {
      setDelimiterParsingDisabled(true)
      load(file.toJava)
    }

    props.getKeys.asScala.toList.fproduct(props.getString)
  }
}
