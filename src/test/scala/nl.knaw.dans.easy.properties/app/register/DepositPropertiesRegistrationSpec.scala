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

import cats.scalatest.{ EitherMatchers, EitherValues }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseFixture, FileSystemSupport, RegistrationTestData, TestSupportFixture }

class DepositPropertiesRegistrationSpec extends TestSupportFixture
  with EitherValues
  with EitherMatchers
  with FileSystemSupport
  with DatabaseFixture
  with RegistrationTestData {

  val registration = new DepositPropertiesRegistration(repository)

  "register" should "import the data from the deposit.properties in the inputstream" in {
    val props = validDepositPropertiesBody
    // @formatter:off
    val DepositProperties(
      deposit, Some(state), Some(ingestStep), Seq(fedora, doi, urn, bagstore), Some(doiAction),
      Some(doiRegistered), Some(curation), Some(springfield), Some(contentType),
    ) = validDepositProperties
    // @formatter:on
    val depositId = deposit.id

    registration.register(depositId, props).value shouldBe depositId

    repository.deposits.find(Seq(depositId)).value should contain only deposit
    repository.states.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only state.toOutput("0")
    repository.ingestSteps.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only ingestStep.toOutput("0")
    repository.identifiers.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only(
      fedora.toOutput("0"),
      doi.toOutput("1"),
      urn.toOutput("2"),
      bagstore.toOutput("3"),
    )
    repository.doiAction.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only doiAction
    repository.doiRegistered.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only doiRegistered
    repository.curation.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only curation.toOutput("0")
    repository.springfield.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only springfield.toOutput("0")
    repository.contentType.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only contentType.toOutput("3")
  }

  it should "import the minimal example" in {
    val props = minimalDepositPropertiesBody
    val DepositProperties(deposit, None, None, Seq(), None, None, None, None, None) = minimalDepositProperties
    val depositId = deposit.id

    registration.register(depositId, props).value shouldBe depositId

    repository.deposits.find(Seq(depositId)).value should contain only deposit
    repository.states.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.ingestSteps.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.identifiers.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.doiAction.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.doiRegistered.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.curation.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.springfield.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.contentType.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
  }

  it should "not import data into the database when the deposit.properties is not valid" in {
    val invalidProps = """creation.timestamp = 2019-01-01T00:00:00.000Z""".stripMargin

    inside(registration.register(UUID.randomUUID(), invalidProps).leftValue) {
      case ValidationImportErrors(userIdError :: originError :: Nil) =>
        userIdError shouldBe PropertyNotFoundError("depositor.userId")
        originError shouldBe PropertyNotFoundError("deposit.origin")
    }

    repository.deposits.getAll.value shouldBe empty
  }

  it should "not import data into the database when the deposit is already registered in the database" in {
    val props1 =
      """creation.timestamp = 2019-01-01T01:01:01.000Z
        |depositor.userId = user001
        |deposit.origin = SWORD2""".stripMargin
    val props2 =
      """creation.timestamp = 2019-02-02T02:02:02.000Z
        |depositor.userId = user002
        |deposit.origin = SMD""".stripMargin

    registration.register(depositId, props1) shouldBe right
    registration.register(depositId, props2).leftValue shouldBe DepositAlreadyExistsError(depositId)
  }
}
