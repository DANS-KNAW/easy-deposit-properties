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

import java.util.UUID

import better.files.StringOps
import cats.data.Validated.Invalid
import cats.scalatest.{ EitherMatchers, EitherValues, ValidatedMatchers, ValidatedValues }
import cats.syntax.either._
import cats.syntax.foldable._
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesImporter._
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesValidator._
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CurationDao, DepositDao, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, InvalidValueError, Repository, SpringfieldDao, StateDao }
import nl.knaw.dans.easy.properties.fixture.{ ImportTestData, TestSupportFixture }
import org.apache.commons.configuration.ConversionException
import org.scalamock.scalatest.MockFactory

class DepositPropertiesValidatorSpec extends TestSupportFixture
  with EitherValues
  with EitherMatchers
  with ValidatedValues
  with ValidatedMatchers
  with MockFactory
  with ImportTestData {

  "validateDepositProperties" should "parse the properties file into an object structure" in {
    val props = readDepositProperties(validDepositPropertiesBody.inputStream).value
    validateDepositProperties(props).value shouldBe validDepositProperties
  }

  it should "parse the minimal example" in {
    val props = readDepositProperties(minimalDepositPropertiesBody.inputStream).value
    validateDepositProperties(props).value shouldBe minimalDepositProperties
  }

  it should "fail when creation.timestamp cannot be parsed" in {
    val props = readDepositProperties(
      """depositId = 9d507261-3b79-22e7-86d0-6fb9417d930d
        |creation.timestamp = invalid
        |depositor.userId = user001
        |deposit.origin = SWORD2""".stripMargin.inputStream
    ).value
    inside(validateDepositProperties(props).leftMap(_.toList)) {
      case Invalid(error :: Nil) =>
        error should matchPattern { case PropertyParseError("creation.timestamp", _: IllegalArgumentException) => }
    }
  }

  it should "fail when mandatory fields are not present" in {
    val props = readDepositProperties(
      """creation.timestamp = 2019-01-01T00:00:00.000+01:00""".stripMargin.inputStream
    ).value
    inside(validateDepositProperties(props).leftMap(_.toList)) {
      case Invalid(depositIdError :: depositorIdError :: originError :: Nil) =>
        depositIdError should matchPattern { case PropertyNotFoundError("depositId") => }
        depositorIdError should matchPattern { case PropertyNotFoundError("depositor.userId") => }
        originError should matchPattern { case PropertyNotFoundError("deposit.origin") => }
    }
  }

  it should "fail when the depositId value cannot be parsed" in {
    val props = readDepositProperties(
      """depositId = invalid-uuid
        |creation.timestamp = 2019-01-01T00:00:00.000+01:00
        |depositor.userId = user001
        |deposit.origin = SWORD2""".stripMargin.inputStream
    ).value

    inside(validateDepositProperties(props).leftMap(_.toList)) {
      case Invalid(depositIdError :: Nil) =>
        depositIdError should matchPattern { case PropertyParseError("depositId", _: IllegalArgumentException) => }
    }
  }

  it should "fail when enum values cannot be parsed" in {
    val props = readDepositProperties(
      """depositId = 9d507261-3b79-22e7-86d0-6fb9417d930d
        |creation.timestamp = 2019-01-01T00:00:00.000+01:00
        |depositor.userId = user001
        |deposit.origin = invalid-origin
        |
        |state.label = invalid-state-label
        |state.description = my description
        |
        |deposit.ingest.current-step = invalid-ingest-step
        |
        |identifier.dans-doi.action = invalid-doi-action
        |
        |springfield.domain = domain
        |springfield.user = user
        |springfield.collection = collection
        |springfield.playmode = invalid-playmode
        |
        |easy-sword2.client-message.content-type = invalid-content-type""".stripMargin.inputStream
    ).value

    inside(validateDepositProperties(props).leftMap(_.toList)) {
      case Invalid(originError :: stateLabelError :: ingestStepError :: doiActionError :: playmodeError :: contentTypeError :: Nil) =>
        originError should matchPattern { case PropertyParseError("deposit.origin", _: NoSuchElementException) => }
        stateLabelError should matchPattern { case PropertyParseError("state.label", _: NoSuchElementException) => }
        ingestStepError should matchPattern { case PropertyParseError("deposit.ingest.current-step", _: NoSuchElementException) => }
        doiActionError should matchPattern { case PropertyParseError("identifier.dans-doi.action", _: NoSuchElementException) => }
        playmodeError should matchPattern { case PropertyParseError("springfield.playmode", _: NoSuchElementException) => }
        contentTypeError should matchPattern { case PropertyParseError("easy-sword2.client-message.content-type", _: NoSuchElementException) => }
    }
  }

  it should "fail when boolean values cannot be parsed" in {
    val props = readDepositProperties(
      """depositId = 9d507261-3b79-22e7-86d0-6fb9417d930d
        |creation.timestamp = 2019-01-01T00:00:00.000+01:00
        |depositor.userId = user001
        |deposit.origin = SWORD2
        |
        |identifier.dans-doi.registered = invalid-value
        |
        |curation.datamanager.userId = archie001
        |curation.datamanager.email = does.not.exists@dans.knaw.nl
        |
        |curation.is-new-version = invalid-value
        |curation.required = invalid-value
        |curation.performed = invalid-value""".stripMargin.inputStream
    ).value

    inside(validateDepositProperties(props).leftMap(_.toList)) {
      case Invalid(dansDoiRegisteredError :: isNewVersionError :: isRequiredError :: isPerformedError :: Nil) =>
        dansDoiRegisteredError should matchPattern { case PropertyParseError("identifier.dans-doi.registered", _: ConversionException) => }
        isNewVersionError should matchPattern { case PropertyParseError("curation.is-new-version", _: ConversionException) => }
        isRequiredError should matchPattern { case PropertyParseError("curation.required", _: ConversionException) => }
        isPerformedError should matchPattern { case PropertyParseError("curation.performed", _: ConversionException) => }
    }
  }

  "validateDepositDoesNotExist" should "succeed if the deposit does not yet exist" in {
    val depositDao = mock[DepositDao]
    val stateDao = mock[StateDao]
    val ingestStepDao = mock[IngestStepDao]
    val identifierDao = mock[IdentifierDao]
    val doiRegisteredDao = mock[DoiRegisteredDao]
    val doiActionDao = mock[DoiActionDao]
    val curationDao = mock[CurationDao]
    val springfieldDao = mock[SpringfieldDao]
    val contentTypeDao = mock[ContentTypeDao]
    val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curationDao, springfieldDao, contentTypeDao)

    val depositId = UUID.randomUUID()
    depositDao.find _ expects Seq(depositId) once() returning Seq.empty.asRight

    validateDepositDoesNotExist(depositId)(repo).value shouldBe right
  }

  it should "fail if the deposit already exists" in {
    val depositDao = mock[DepositDao]
    val stateDao = mock[StateDao]
    val ingestStepDao = mock[IngestStepDao]
    val identifierDao = mock[IdentifierDao]
    val doiRegisteredDao = mock[DoiRegisteredDao]
    val doiActionDao = mock[DoiActionDao]
    val curationDao = mock[CurationDao]
    val springfieldDao = mock[SpringfieldDao]
    val contentTypeDao = mock[ContentTypeDao]
    val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curationDao, springfieldDao, contentTypeDao)

    val depositId = UUID.randomUUID()
    depositDao.find _ expects Seq(depositId) once() returning Seq(validDepositProperties.deposit).asRight

    validateDepositDoesNotExist(depositId)(repo).value.leftValue shouldBe DepositAlreadyExistsError(depositId)
  }

  it should "fail if the database returns an error" in {
    val depositDao = mock[DepositDao]
    val stateDao = mock[StateDao]
    val ingestStepDao = mock[IngestStepDao]
    val identifierDao = mock[IdentifierDao]
    val doiRegisteredDao = mock[DoiRegisteredDao]
    val doiActionDao = mock[DoiActionDao]
    val curationDao = mock[CurationDao]
    val springfieldDao = mock[SpringfieldDao]
    val contentTypeDao = mock[ContentTypeDao]
    val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curationDao, springfieldDao, contentTypeDao)

    val depositId = UUID.randomUUID()
    val error = InvalidValueError("abc")
    depositDao.find _ expects Seq(depositId) once() returning error.asLeft

    validateDepositDoesNotExist(depositId)(repo).leftValue shouldBe error
  }
}
