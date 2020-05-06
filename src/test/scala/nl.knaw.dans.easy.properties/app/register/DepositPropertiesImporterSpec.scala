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

import cats.scalatest.{ EitherMatchers, EitherValues }
import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesImporter._
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesValidator.{ depositorKey, _ }
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CuratorDao, DepositDao, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, IsCurationPerformedDao, IsCurationRequiredDao, IsNewVersionDao, MutationError, Repository, SpringfieldDao, StateDao }
import nl.knaw.dans.easy.properties.fixture.{ RegistrationTestData, TestSupportFixture }
import org.scalamock.scalatest.MockFactory

import scala.collection.JavaConverters._

class DepositPropertiesImporterSpec extends TestSupportFixture
  with EitherValues
  with EitherMatchers
  with MockFactory
  with RegistrationTestData {

  "readDepositProperties" should "read the inputstream and produce a PropertiesConfiguration object" in {
    val props = readDepositProperties(validDepositPropertiesBody).value
    val expectedResult = Map(
      bagNameKey -> "bag",
      creationTimestampKey -> "2019-01-01T00:00:00.000Z",
      depositorKey -> "user001",
      originKey -> "SWORD2",
      stateLabelKey -> "SUBMITTED",
      stateDescriptionKey -> "my description",
      ingestStepKey -> "BAGSTORE",
      fedoraIdentifierKey -> "my-fedora-value",
      urnIdentifierKey -> "my-urn-value",
      doiIdentifierKey -> "my-doi-value",
      bagStoreIdentifierKey -> "my-bag-store-value",
      dansDoiActionKey -> "update",
      dansDoiRegisteredKey -> "yes",
      isNewVersionKey -> "yes",
      isCurationRequiredKey -> "no",
      isCurationPerformedKey -> "no",
      datamanagerUserIdKey -> "archie001",
      datamanagerEmailKey -> "does.not.exists@dans.knaw.nl",
      springfieldDomainKey -> "domain",
      springfieldUserKey -> "user",
      springfieldCollectionKey -> "collection",
      springfieldPlaymodeKey -> "continuous",
      contentTypeKey -> "application/zip",
    )

    props.getKeys.asScala.toList should contain theSameElementsAs expectedResult.keySet

    forEvery(expectedResult) {
      case (key, value) => props.getString(key) shouldBe value
    }
  }

  "importDepositProperties" should "store the properties in the database" in {
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

    // @formatter:off
    val input @ DepositProperties(
      deposit, Some(state), Some(ingestStep), Seq(fedora, doi, urn, bagstore), Some(doiAction),
      Some(doiRegistered), Some(curator), Some(isNewVersion), Some(isCurationRequired),
      Some(isCurationPerformed), Some(springfield), Some(contentType),
    ) = validDepositProperties
    // @formatter:on
    val depositId = deposit.id
    depositDao.store _ expects deposit once() returning deposit.asRight
    stateDao.store _ expects(depositId, state) once() returning state.toOutput("abc").asRight
    ingestStepDao.store _ expects(depositId, ingestStep) once() returning ingestStep.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, fedora) once() returning fedora.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, doi) once() returning doi.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, urn) once() returning urn.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, bagstore) once() returning bagstore.toOutput("abc").asRight
    doiRegisteredDao.store _ expects(depositId, doiRegistered) once() returning doiRegistered.asRight
    doiActionDao.store _ expects(depositId, doiAction) once() returning doiAction.asRight
    curatorDao.store _ expects(depositId, curator) once() returning curator.toOutput("abc").asRight
    isNewVersionDao.store _ expects(depositId, isNewVersion) once() returning isNewVersion.toOutput("abc").asRight
    isCurationRequiredDao.store _ expects(depositId, isCurationRequired) once() returning isCurationRequired.toOutput("abc").asRight
    isCurationPerformedDao.store _ expects(depositId, isCurationPerformed) once() returning isCurationPerformed.toOutput("abc").asRight
    springfieldDao.store _ expects(depositId, springfield) once() returning springfield.toOutput("abc").asRight
    contentTypeDao.store _ expects(depositId, contentType) once() returning contentType.toOutput("abc").asRight

    importDepositProperties(input, repo) shouldBe right
  }

  it should "store minimal properties in the database" in {
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

    val input = minimalDepositProperties
    depositDao.store _ expects input.deposit once() returning input.deposit.asRight
    stateDao.store _ expects(*, *) never()
    ingestStepDao.store _ expects(*, *) never()
    identifierDao.store _ expects(*, *) never()
    doiRegisteredDao.store _ expects(*, *) never()
    doiActionDao.store _ expects(*, *) never()
    curatorDao.store _ expects(*, *) never()
    isNewVersionDao.store _ expects(*, *) never()
    isCurationRequiredDao.store _ expects(*, *) never()
    isCurationPerformedDao.store _ expects(*, *) never()
    springfieldDao.store _ expects(*, *) never()
    contentTypeDao.store _ expects(*, *) never()

    importDepositProperties(input, repo) shouldBe right
  }

  it should "fail when one of the store functions returns an error" in {
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

    val input @ DepositProperties(deposit, Some(state), _, _, _, _, _, _, _, _, _, _) = validDepositProperties
    val depositId = deposit.id
    val error = MutationError("error")

    depositDao.store _ expects input.deposit once() returning input.deposit.asRight
    stateDao.store _ expects(depositId, state) once() returning error.asLeft
    ingestStepDao.store _ expects(*, *) never()
    identifierDao.store _ expects(*, *) never()
    doiRegisteredDao.store _ expects(*, *) never()
    doiActionDao.store _ expects(*, *) never()
    curatorDao.store _ expects(*, *) never()
    isNewVersionDao.store _ expects(*, *) never()
    isCurationRequiredDao.store _ expects(*, *) never()
    isCurationPerformedDao.store _ expects(*, *) never()
    springfieldDao.store _ expects(*, *) never()
    contentTypeDao.store _ expects(*, *) never()

    importDepositProperties(input, repo).leftValue shouldBe error
  }
}
