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
package nl.knaw.dans.easy.properties.app.register

import cats.data.Validated
import cats.instances.list._
import cats.instances.option._
import cats.syntax.apply._
import cats.syntax.traverse._
import cats.syntax.validated._
import nl.knaw.dans.easy.properties.app.model.contentType.InputContentType
import nl.knaw.dans.easy.properties.app.model.curator.InputCurator
import nl.knaw.dans.easy.properties.app.model.identifier.{ IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.InputIsCurationPerformed
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.InputIsCurationRequired
import nl.knaw.dans.easy.properties.app.model.isnewversion.InputIsNewVersion
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DoiAction, DoiActionEvent, DoiRegisteredEvent, Origin, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.{ QueryErrorOr, Repository }
import org.apache.commons.configuration.{ ConversionException, PropertiesConfiguration, PropertyConverter }
import org.joda.time.DateTime

import scala.reflect.ClassTag

object DepositPropertiesValidator {

  val creationTimestampKey = "creation.timestamp"
  val bagNameKey = "bag-store.bag-name"
  val depositorKey = "depositor.userId"
  val originKey = "deposit.origin"
  val stateLabelKey = "state.label"
  val stateDescriptionKey = "state.description"
  val ingestStepKey = "deposit.ingest.current-step"
  val fedoraIdentifierKey = "identifier.fedora"
  val urnIdentifierKey = "identifier.urn"
  val doiIdentifierKey = "identifier.doi"
  val bagStoreIdentifierKey = "bag-store.bag-id"
  val dansDoiActionKey = "identifier.dans-doi.action"
  val dansDoiRegisteredKey = "identifier.dans-doi.registered"
  val isNewVersionKey = "curation.is-new-version"
  val isCurationRequiredKey = "curation.required"
  val isCurationPerformedKey = "curation.performed"
  val datamanagerUserIdKey = "curation.datamanager.userId"
  val datamanagerEmailKey = "curation.datamanager.email"
  val springfieldDomainKey = "springfield.domain"
  val springfieldUserKey = "springfield.user"
  val springfieldCollectionKey = "springfield.collection"
  val springfieldPlaymodeKey = "springfield.playmode"
  val contentTypeKey = "easy-sword2.client-message.content-type"

  def validateDepositProperties(depositId: DepositId)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[DepositProperties] = {
    validateCreationTimestamp
      .andThen(implicit timestamp => {
        // @formatter:off
        (
          validateDeposit(depositId),
          validateState,
          validateIngestStep,
          validateIdentifiers,
          validateDoiAction,
          validateDoiRegistered,
          validateCurator,
          validateIsNewVersion,
          validateIsCurationRequired,
          validateIsCurationPerformed,
          validateSpringfield,
          validateContentType,
        ).mapN(DepositProperties)
        // @formatter:on
      })
  }

  private def getMandatoryStringProp(key: String)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[String] = {
    Option(props.getString(key))
      .map(_.validNec)
      .getOrElse(PropertyNotFoundError(key).invalidNec)
  }

  private def getMandatoryProp[T, E >: Null <: Throwable : ClassTag](key: String)(parser: String => T)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[T] = {
    getMandatoryStringProp(key)
      .andThen(s => {
        Validated.catchOnly[E] { parser(s) }
          .leftMap(PropertyParseError(key, _))
          .toValidatedNec
      })
  }

  private def getMandatoryEnumProp[T <: Enumeration](key: String)(enum: T)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[enum.Value] = {
    getMandatoryProp[enum.Value, NoSuchElementException](key)(s => enum withName s)
  }

  private def getOptionalStringProp(key: String)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[Option[String]] = {
    Option(props.getString(key)).validNec
  }

  private def getOptionalProp[T, E >: Null <: Throwable : ClassTag](key: String)(parser: String => T)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[Option[T]] = {
    Option(props.getString(key))
      .traverse[ValidationImportErrorsOr, T](s => {
        Validated.catchOnly[E] { parser(s) }
          .leftMap(PropertyParseError(key, _))
          .toValidatedNec
      })
  }

  private def getOptionalEnumProp[T <: Enumeration](key: String)(enum: T)(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[Option[enum.Value]] = {
    getOptionalProp[enum.Value, NoSuchElementException](key)(s => enum withName s)
  }

  private def requireAllOrNone(xs: (String, Option[_])*): ValidationImportErrorsOr[Unit] = {
    val (defined, empty) = xs.partition { case (_, v) => v.isDefined }
    if (empty.isEmpty || defined.isEmpty)
      ().validNec
    else
      MissingPropertiesError(empty.map(_._1), defined.map(_._1)).invalidNec
  }

  private def validateCreationTimestamp(implicit props: PropertiesConfiguration): ValidationImportErrorsOr[Timestamp] = {
    getMandatoryProp[DateTime, IllegalArgumentException](creationTimestampKey)(DateTime.parse)
  }

  private def validateDeposit(depositId: DepositId)(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Deposit] = {
    // @formatter:off
    (
      getOptionalStringProp(bagNameKey),
      getMandatoryStringProp(depositorKey),
      getMandatoryEnumProp(originKey)(Origin),
    ).mapN(Deposit(depositId, _, timestamp, _, _))
    // @formatter:on
  }

  private def validateState(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputState]] = {
    // @formatter:off
    (
      getOptionalEnumProp(stateLabelKey)(StateLabel),
      getOptionalStringProp(stateDescriptionKey),
    ).tupled
      .andThen {
        case tuple @ (label, description) =>
          requireAllOrNone(
            stateLabelKey -> label,
            stateDescriptionKey -> description
          ).map(_ => tuple)
      }
      .map(_.mapN(InputState(_, _, timestamp)))
    // @formatter:on
  }

  private def validateIngestStep(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputIngestStep]] = {
    getOptionalEnumProp(ingestStepKey)(IngestStepLabel)
      .map(_.map(InputIngestStep(_, timestamp)))
  }

  private def validateIdentifiers(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Seq[InputIdentifier]] = {
    val fedoraIdentifier: ValidationImportErrorsOr[Option[InputIdentifier]] = getOptionalStringProp(fedoraIdentifierKey).map(_.map(InputIdentifier(IdentifierType.FEDORA, _, timestamp)))
    val doiIdentifier: ValidationImportErrorsOr[Option[InputIdentifier]] = getOptionalStringProp(doiIdentifierKey).map(_.map(InputIdentifier(IdentifierType.DOI, _, timestamp)))
    val urnIdentifier: ValidationImportErrorsOr[Option[InputIdentifier]] = getOptionalStringProp(urnIdentifierKey).map(_.map(InputIdentifier(IdentifierType.URN, _, timestamp)))
    val bagStoreIdentifier: ValidationImportErrorsOr[Option[InputIdentifier]] = getOptionalStringProp(bagStoreIdentifierKey).map(_.map(InputIdentifier(IdentifierType.BAG_STORE, _, timestamp)))

    val ids: List[ValidationImportErrorsOr[Option[InputIdentifier]]] = List(fedoraIdentifier, doiIdentifier, urnIdentifier, bagStoreIdentifier)
    ids.sequence.map(_.flatten)
  }

  private def validateDoiAction(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[DoiActionEvent]] = {
    getOptionalEnumProp(dansDoiActionKey)(DoiAction)
      .map(_.map(DoiActionEvent(_, timestamp)))
  }

  private def validateDoiRegistered(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[DoiRegisteredEvent]] = {
    getOptionalProp[Boolean, ConversionException](dansDoiRegisteredKey)(PropertyConverter.toBoolean)
      .map(_.map(DoiRegisteredEvent(_, timestamp)))
  }

  private def validateCurator(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputCurator]] = {
    // @formatter:off
    (
      getOptionalStringProp(datamanagerUserIdKey),
      getOptionalStringProp(datamanagerEmailKey),
    ).tupled
      .andThen {
        case tuple @ (userId, email) => requireAllOrNone(
          datamanagerUserIdKey -> userId,
          datamanagerEmailKey -> email,
        ).map(_ => tuple.mapN(InputCurator(_, _, timestamp)))
      }
    // @formatter:on
  }

  private def validateIsNewVersion(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputIsNewVersion]] = {
    // @formatter:off
    getOptionalProp[Boolean, ConversionException](isNewVersionKey)(PropertyConverter.toBoolean)
      .map(_.map(InputIsNewVersion(_, timestamp)))
    // @formatter:on
  }

  private def validateIsCurationRequired(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputIsCurationRequired]] = {
    // @formatter:off
    getOptionalProp[Boolean, ConversionException](isCurationRequiredKey)(PropertyConverter.toBoolean)
      .map(_.map(InputIsCurationRequired(_, timestamp)))
    // @formatter:on
  }

  private def validateIsCurationPerformed(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputIsCurationPerformed]] = {
    // @formatter:off
    getOptionalProp[Boolean, ConversionException](isCurationPerformedKey)(PropertyConverter.toBoolean)
      .map(_.map(InputIsCurationPerformed(_, timestamp)))
    // @formatter:on
  }

  private def validateSpringfield(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputSpringfield]] = {
    // @formatter:off
    (
      getOptionalStringProp(springfieldDomainKey),
      getOptionalStringProp(springfieldUserKey),
      getOptionalStringProp(springfieldCollectionKey),
      getOptionalEnumProp(springfieldPlaymodeKey)(SpringfieldPlayMode),
    ).tupled
      .andThen {
        case tuple @ (domain, user, collection, playmode) =>
          requireAllOrNone(
            springfieldDomainKey -> domain,
            springfieldUserKey -> user,
            springfieldCollectionKey -> collection,
            springfieldPlaymodeKey -> playmode
          ).map(_ => tuple)
      }
      .map(_.mapN(InputSpringfield(_, _, _, _, timestamp)))
    // @formatter:on
  }

  private def validateContentType(implicit props: PropertiesConfiguration, timestamp: Timestamp): ValidationImportErrorsOr[Option[InputContentType]] = {
    getOptionalStringProp(contentTypeKey)
      .map(_.map(InputContentType(_, timestamp)))
  }

  def depositExists(depositId: DepositId)(implicit repo: Repository): QueryErrorOr[Boolean] = {
    repo.deposits
      .find(Seq(depositId))
      .map(_.nonEmpty)
  }
}
