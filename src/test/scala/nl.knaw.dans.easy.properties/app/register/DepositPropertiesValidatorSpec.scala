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

import cats.data.Validated.Invalid
import cats.scalatest.{ EitherMatchers, EitherValues, ValidatedMatchers, ValidatedValues }
import cats.syntax.either._
import cats.syntax.foldable._
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesImporter._
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesValidator._
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CuratorDao, DepositDao, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, InvalidValueError, IsCurationPerformedDao, IsCurationRequiredDao, IsNewVersionDao, Repository, SpringfieldDao, StateDao }
import nl.knaw.dans.easy.properties.fixture.{ RegistrationTestData, TestSupportFixture }
import org.apache.commons.configuration.ConversionException
import org.scalamock.scalatest.MockFactory

class DepositPropertiesValidatorSpec extends TestSupportFixture
  with EitherValues
  with EitherMatchers
  with ValidatedValues
  with ValidatedMatchers
  with MockFactory
  with RegistrationTestData {

  "validateDepositProperties" should "parse the properties file into an object structure" in {
    val props = readDepositProperties(validDepositPropertiesBody).value
    validateDepositProperties(depositId)(props).value shouldBe validDepositProperties
  }

  it should "parse the minimal example" in {
    val props = readDepositProperties(minimalDepositPropertiesBody).value
    validateDepositProperties(depositId)(props).value shouldBe minimalDepositProperties
  }

  it should "fail when creation.timestamp cannot be parsed" in {
    val props = readDepositProperties(
      """creation.timestamp = invalid
        |depositor.userId = user001
        |deposit.origin = SWORD2""".stripMargin
    ).value
    inside(validateDepositProperties(depositId)(props).leftMap(_.toList)) {
      case Invalid(error :: Nil) =>
        error should matchPattern { case PropertyParseError("creation.timestamp", _: IllegalArgumentException) => }
    }
  }

  it should "fail when mandatory fields are not present" in {
    val props = readDepositProperties(
      """creation.timestamp = 2019-01-01T00:00:00.000+01:00""".stripMargin
    ).value
    inside(validateDepositProperties(depositId)(props).leftMap(_.toList)) {
      case Invalid(depositorIdError :: originError :: Nil) =>
        depositorIdError shouldBe PropertyNotFoundError("depositor.userId")
        originError shouldBe PropertyNotFoundError("deposit.origin")
    }
  }

  it should "fail when enum values cannot be parsed" in {
    val props = readDepositProperties(
      """creation.timestamp = 2019-01-01T00:00:00.000+01:00
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
        |easy-sword2.client-message.content-type = invalid-content-type""".stripMargin
    ).value

    inside(validateDepositProperties(depositId)(props).leftMap(_.toList)) {
      case Invalid(originError :: stateLabelError :: ingestStepError :: doiActionError :: playmodeError :: contentTypeError :: Nil) =>
        originError should matchPattern { case PropertyParseError("deposit.origin", _: NoSuchElementException) => }
        stateLabelError should matchPattern { case PropertyParseError("state.label", _: NoSuchElementException) => }
        ingestStepError should matchPattern { case PropertyParseError("deposit.ingest.current-step", _: NoSuchElementException) => }
        doiActionError should matchPattern { case PropertyParseError("identifier.dans-doi.action", _: NoSuchElementException) => }
        playmodeError should matchPattern { case PropertyParseError("springfield.playmode", _: NoSuchElementException) => }
        contentTypeError should matchPattern { case PropertyParseError("easy-sword2.client-message.content-type", _: NoSuchElementException) => }
    }
  }

  it should "fail when required combinations of values are missing" in {
    val props = readDepositProperties(
      """creation.timestamp = 2019-01-01T00:00:00.000+01:00
        |depositor.userId = user001
        |deposit.origin = SMD
        |
        |state.label = ARCHIVED
        |
        |curation.datamanager.userId = user001
        |
        |springfield.domain = foobar
        |springfield.user = barfoo""".stripMargin
    ).value

    inside(validateDepositProperties(depositId)(props).leftMap(_.toList)) {
      case Invalid(stateError :: datamanagerError :: springfieldError :: Nil) =>
        stateError should matchPattern { case MissingPropertiesError(Seq("state.description"), Seq("state.label")) => }
        datamanagerError should matchPattern { case MissingPropertiesError(Seq("curation.datamanager.email"), Seq("curation.datamanager.userId")) => }
        springfieldError should matchPattern { case MissingPropertiesError(Seq("springfield.collection", "springfield.playmode"), Seq("springfield.domain", "springfield.user")) => }
    }
  }

  it should "fail when boolean values cannot be parsed" in {
    val props = readDepositProperties(
      """creation.timestamp = 2019-01-01T00:00:00.000+01:00
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
        |curation.performed = invalid-value""".stripMargin
    ).value

    inside(validateDepositProperties(depositId)(props).leftMap(_.toList)) {
      case Invalid(dansDoiRegisteredError :: isNewVersionError :: isRequiredError :: isPerformedError :: Nil) =>
        dansDoiRegisteredError should matchPattern { case PropertyParseError("identifier.dans-doi.registered", _: ConversionException) => }
        isNewVersionError should matchPattern { case PropertyParseError("curation.is-new-version", _: ConversionException) => }
        isRequiredError should matchPattern { case PropertyParseError("curation.required", _: ConversionException) => }
        isPerformedError should matchPattern { case PropertyParseError("curation.performed", _: ConversionException) => }
    }
  }

  "depositExists" should "return false if the deposit does not yet exist" in {
    val depositDao = mock[DepositDao]
    val stateDao = mock[StateDao]
    val ingestStepDao = mock[IngestStepDao]
    val identifierDao = mock[IdentifierDao]
    val doiRegisteredDao = mock[DoiRegisteredDao]
    val doiActionDao = mock[DoiActionDao]
    val curatorDao = mock[CuratorDao]
    val isNewVersionDao = mock[IsNewVersionDao]
    val isCurationRequiredDao = mock[IsCurationRequiredDao]
    val isCurationPerformedDao = mock[IsCurationPerformedDao]
    val springfieldDao = mock[SpringfieldDao]
    val contentTypeDao = mock[ContentTypeDao]
    val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curatorDao, isNewVersionDao, isCurationRequiredDao, isCurationPerformedDao, springfieldDao, contentTypeDao)

    val depositId = UUID.randomUUID()
    depositDao.find _ expects Seq(depositId) once() returning Seq.empty.asRight

    depositExists(depositId)(repo).value shouldBe false
  }

  it should "return true if the deposit already exists" in {
    val depositDao = mock[DepositDao]
    val stateDao = mock[StateDao]
    val ingestStepDao = mock[IngestStepDao]
    val identifierDao = mock[IdentifierDao]
    val doiRegisteredDao = mock[DoiRegisteredDao]
    val doiActionDao = mock[DoiActionDao]
    val curatorDao = mock[CuratorDao]
    val isNewVersionDao = mock[IsNewVersionDao]
    val isCurationRequiredDao = mock[IsCurationRequiredDao]
    val isCurationPerformedDao = mock[IsCurationPerformedDao]
    val springfieldDao = mock[SpringfieldDao]
    val contentTypeDao = mock[ContentTypeDao]
    val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curatorDao, isNewVersionDao, isCurationRequiredDao, isCurationPerformedDao, springfieldDao, contentTypeDao)

    val depositId = UUID.randomUUID()
    depositDao.find _ expects Seq(depositId) once() returning Seq(validDepositProperties.deposit).asRight

    depositExists(depositId)(repo).value shouldBe true
  }

  it should "fail if the database returns an error" in {
    val depositDao = mock[DepositDao]
    val stateDao = mock[StateDao]
    val ingestStepDao = mock[IngestStepDao]
    val identifierDao = mock[IdentifierDao]
    val doiRegisteredDao = mock[DoiRegisteredDao]
    val doiActionDao = mock[DoiActionDao]
    val curatorDao = mock[CuratorDao]
    val isNewVersionDao = mock[IsNewVersionDao]
    val isCurationRequiredDao = mock[IsCurationRequiredDao]
    val isCurationPerformedDao = mock[IsCurationPerformedDao]
    val springfieldDao = mock[SpringfieldDao]
    val contentTypeDao = mock[ContentTypeDao]
    val repo = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curatorDao, isNewVersionDao, isCurationRequiredDao, isCurationPerformedDao, springfieldDao, contentTypeDao)

    val depositId = UUID.randomUUID()
    val error = InvalidValueError("abc")
    depositDao.find _ expects Seq(depositId) once() returning error.asLeft

    depositExists(depositId)(repo).leftValue shouldBe error
  }
}
