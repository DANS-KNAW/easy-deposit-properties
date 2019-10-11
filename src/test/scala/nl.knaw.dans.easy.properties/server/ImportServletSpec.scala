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
package nl.knaw.dans.easy.properties.server

import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.app.register.{ DepositProperties, DepositPropertiesRegistration }
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CurationDao, DepositDao, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, InvalidValueError, Repository, SpringfieldDao, StateDao }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseFixture, FileSystemSupport, ImportTestData, TestSupportFixture }
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.EmbeddedJettyContainer
import org.scalatra.test.scalatest.ScalatraSuite

class ImportServletSpec extends TestSupportFixture
  with MockFactory
  with DatabaseFixture
  with FileSystemSupport
  with GraphQLResolveSpecTestObjects
  with EmbeddedJettyContainer
  with ScalatraSuite
  with ImportTestData {

  private val authHeader = "Authorization" -> "Basic bXktdXNlcm5hbWU6bXktcGFzc3dvcmQ="
  // TODO replace mocks with real DAOs and database
  private val depositDao = mock[DepositDao]
  private val stateDao = mock[StateDao]
  private val ingestStepDao = mock[IngestStepDao]
  private val identifierDao = mock[IdentifierDao]
  private val doiRegisteredDao = mock[DoiRegisteredDao]
  private val doiActionDao = mock[DoiActionDao]
  private val curationDao = mock[CurationDao]
  private val springfieldDao = mock[SpringfieldDao]
  private val contentTypeDao = mock[ContentTypeDao]
  private val repository = Repository(depositDao, stateDao, ingestStepDao, identifierDao, doiRegisteredDao, doiActionDao, curationDao, springfieldDao, contentTypeDao)
  private val servlet = new ImportServlet(
    registrator = new DepositPropertiesRegistration(
      database = databaseAccess,
      repository = _ => repository,
    ),
    expectedAuth = Auth("my-username", "my-password"),
  )

  addServlet(servlet, "/*")

  "post /" should "register the provided deposit properties in the database" in {
    val testBody = validDepositPropertiesBody

    // @formatter:off
    val DepositProperties(
      deposit, Some(state), Some(ingestStep), Seq(fedora, doi, urn, bagstore), Some(doiAction),
      Some(doiRegistered), Some(curation), Some(springfield), Some(contentType),
    ) = validDepositProperties
    // @formatter:on
    val depositId = deposit.id
    depositDao.find _ expects Seq(depositId) once() returning Seq.empty.asRight
    depositDao.store _ expects deposit once() returning deposit.asRight
    stateDao.store _ expects(depositId, state) once() returning state.toOutput("abc").asRight
    ingestStepDao.store _ expects(depositId, ingestStep) once() returning ingestStep.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, fedora) once() returning fedora.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, doi) once() returning doi.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, urn) once() returning urn.toOutput("abc").asRight
    identifierDao.store _ expects(depositId, bagstore) once() returning bagstore.toOutput("abc").asRight
    doiRegisteredDao.store _ expects(depositId, doiRegistered) once() returning doiRegistered.asRight
    doiActionDao.store _ expects(depositId, doiAction) once() returning doiAction.asRight
    curationDao.store _ expects(depositId, curation) once() returning curation.toOutput("abc").asRight
    springfieldDao.store _ expects(depositId, springfield) once() returning springfield.toOutput("abc").asRight
    contentTypeDao.store _ expects(depositId, contentType) once() returning contentType.toOutput("abc").asRight

    post("/", body = testBody, headers = Seq(authHeader)) {
      body shouldBe s"Deposit $depositId has been registered"
      status shouldBe 200
    }
  }

  it should "return 401 when no credentials are provided" in {
    post("/", body = validDepositPropertiesBody) {
      body shouldBe "Unauthenticated"
      header.get("WWW-Authenticate").value shouldBe """Basic realm="easy-deposit-properties""""
      status shouldBe 401
    }
  }

  it should "return 400 when something else than basic auth credentials are provided" in {
    post("/", body = validDepositPropertiesBody, headers = Seq("Authorization" -> "Bearer cn389ncoiwuencr")) {
      body shouldBe "Bad Request"
      status shouldBe 400
    }
  }

  it should "return 401 when wrong credentials are prodived" in {
    post("/", body = validDepositPropertiesBody, headers = Seq("Authorization" -> "Basic invalid=")) {
      body shouldBe "Unauthenticated"
      header.get("WWW-Authenticate").value shouldBe """Basic realm="easy-deposit-properties""""
      status shouldBe 401
    }
  }

  it should "return 400 when no 'creation.timestamp' property is provided" in {
    val testBody =
      """depositId = 9d507261-3b79-22e7-86d0-6fb9417d930d
        |depositor.userId = user001
        |deposit.origin = SWORD2""".stripMargin
    post("/", body = testBody, headers = Seq(authHeader)) {
      body shouldBe
        """Invalid input:
          | - Mandatory property 'creation.timestamp' was not found.""".stripMargin
      status shouldBe 400
    }
  }

  it should "return 400 when mandatory properties are not provided" in {
    val testBody =
      """creation.timestamp = 2019-01-01T00:00:00.000+01:00""".stripMargin
    post("/", body = testBody, headers = Seq(authHeader)) {
      body shouldBe
        """Invalid input:
          | - Mandatory property 'depositId' was not found.
          | - Mandatory property 'depositor.userId' was not found.
          | - Mandatory property 'deposit.origin' was not found.""".stripMargin
      status shouldBe 400
    }
  }

  it should "return 409 for a deposit that already exists in the database" in {
    val testBody = validDepositPropertiesBody

    val DepositProperties(deposit, _, _, _, _, _, _, _, _) = validDepositProperties
    val depositId = deposit.id
    depositDao.find _ expects Seq(depositId) once() returning Seq(deposit).asRight

    post("/", body = testBody, headers = Seq(authHeader)) {
      body shouldBe s"Deposit $depositId already exists"
      status shouldBe 409
    }
  }

  it should "return 500 when the database is not available" in {
    val testBody = validDepositPropertiesBody

    val DepositProperties(deposit, _, _, _, _, _, _, _, _) = validDepositProperties
    val depositId = deposit.id
    depositDao.find _ expects Seq(depositId) once() returning InvalidValueError("some obscure database error").asLeft

    post("/", body = testBody, headers = Seq(authHeader)) {
      body shouldBe "some obscure database error"
      status shouldBe 500
    }
  }
}
