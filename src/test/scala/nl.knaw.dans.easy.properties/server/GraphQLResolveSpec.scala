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

import java.util.UUID

import cats.syntax.option._
import better.files.File
import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentType, ContentTypeValue, DepositContentTypeFilter }
import nl.knaw.dans.easy.properties.app.model.curation.Curation
import nl.knaw.dans.easy.properties.app.model.curator.DepositCuratorFilter
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ DepositIngestStepFilter, IngestStep, IngestStepLabel }
import nl.knaw.dans.easy.properties.app.model.springfield.{ Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ DepositStateFilter, State, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositCurationPerformedFilter, DepositCurationRequiredFilter, DepositDoiActionFilter, DepositDoiRegisteredFilter, DepositId, DepositIsNewVersionFilter, DoiAction, DoiActionEvent, DoiRegisteredEvent, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CurationDao, DepositDao, DepositFilters, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, Repository, SpringfieldDao, StateDao }
import nl.knaw.dans.easy.properties.fixture.{ FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.json4s.{ DefaultFormats, Formats }
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.EmbeddedJettyContainer
import org.scalatra.test.scalatest.ScalatraSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

trait GraphQLResolveSpecTestObjects {
  val depositId1: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val depositId2: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val depositId3: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000003")
  val deposit1 = Deposit(
    id = depositId1,
    bagName = Some("bag1"),
    creationTimestamp = DateTime.now(),
    depositorId = "user001",
  )
  val deposit2 = Deposit(
    id = depositId2,
    bagName = Some("bag2"),
    creationTimestamp = DateTime.now(),
    depositorId = "user002",
  )
  val deposit3 = Deposit(
    id = depositId3,
    bagName = Some("bag3"),
    creationTimestamp = DateTime.now(),
    depositorId = "user002",
  )
  val state1 = State(
    id = "1",
    label = StateLabel.ARCHIVED,
    description = "your deposit is archived",
    timestamp = DateTime.now(),
  )
  val state2 = State(
    id = "2",
    label = StateLabel.DRAFT,
    description = "your deposit is in draft",
    timestamp = DateTime.now(),
  )
  val state3 = State(
    id = "3",
    label = StateLabel.SUBMITTED,
    description = "your deposit is submitted",
    timestamp = DateTime.now(),
  )
  val step1 = IngestStep(
    id = "1",
    step = IngestStepLabel.VALIDATE,
    timestamp = DateTime.now(),
  )
  val step2 = IngestStep(
    id = "2",
    step = IngestStepLabel.FEDORA,
    timestamp = DateTime.now(),
  )
  val step3 = IngestStep(
    id = "3",
    step = IngestStepLabel.COMPLETED,
    timestamp = DateTime.now(),
  )
  val identifier1 = Identifier(
    id = "1",
    idType = IdentifierType.URN,
    idValue = "abcdef",
    timestamp = DateTime.now(),
  )
  val identifier2 = Identifier(
    id = "2",
    idType = IdentifierType.DOI,
    idValue = "123456",
    timestamp = DateTime.now(),
  )
  val identifier3 = Identifier(
    id = "3",
    idType = IdentifierType.FEDORA,
    idValue = "easy-dataset:1",
    timestamp = DateTime.now(),
  )
  val doiRegisteredEvent1 = DoiRegisteredEvent(
    value = true,
    timestamp = DateTime.now(),
  )
  val doiRegisteredEvent2 = DoiRegisteredEvent(
    value = false,
    timestamp = DateTime.now(),
  )
  val doiActionEvent1 = DoiActionEvent(
    value = DoiAction.CREATE,
    timestamp = DateTime.now(),
  )
  val doiActionEvent2 = DoiActionEvent(
    value = DoiAction.UPDATE,
    timestamp = DateTime.now(),
  )
  val curation1 = Curation(
    id = "1",
    isNewVersion = none,
    isRequired = true,
    isPerformed = true,
    datamanagerUserId = "archie001",
    datamanagerEmail = "does-not-exists1@dans.knaw.nl",
    timestamp = DateTime.now(),
  )
  val curation2 = Curation(
    id = "2",
    isNewVersion = false.some,
    isRequired = false,
    isPerformed = false,
    datamanagerUserId = "archie002",
    datamanagerEmail = "does-not-exists2@dans.knaw.nl",
    timestamp = DateTime.now(),
  )
  val springfield1 = Springfield(
    id = "1",
    domain = "domain1",
    user = "user1",
    collection = "collection1",
    playmode = SpringfieldPlayMode.CONTINUOUS,
    timestamp = DateTime.now(),
  )
  val springfield2 = Springfield(
    id = "2",
    domain = "domain2",
    user = "user2",
    collection = "collection2",
    playmode = SpringfieldPlayMode.MENU,
    timestamp = DateTime.now(),
  )
  val contentType1 = ContentType(
    id = "1",
    value = ContentTypeValue.ZIP,
    timestamp = DateTime.now(),
  )
  val contentType2 = ContentType(
    id = "2",
    value = ContentTypeValue.OCTET,
    timestamp = DateTime.now(),
  )
}

class GraphQLResolveSpec extends TestSupportFixture
  with MockFactory
  with FileSystemSupport
  with GraphQLResolveSpecTestObjects
  with EmbeddedJettyContainer
  with ScalatraSuite {

  private val graphqlExamplesDir = File(getClass.getResource("/graphql-examples"))
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
  private val servlet = DepositPropertiesGraphQLServlet[Unit](
    connGen = _ (()),
    repository = _ => repository,
    authenticationConfig = Auth("", ""),
  )
  private implicit val jsonFormats: Formats = new DefaultFormats {}

  addServlet(servlet, "/*")

  "graphql" should "resolve 'deposit/findDeposit/plain.graphql' with 19 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "findDeposit" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      inAnyOrder {
        depositDao.lastModified _ expects Seq(depositId1) once() returning Seq(depositId1 -> DateTime.now()).asRight
        stateDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> state1).asRight
        ingestStepDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> step1).asRight
        identifierDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(identifier1, identifier2),
        ).asRight
        doiRegisteredDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> doiRegisteredEvent1).asRight
        doiRegisteredDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(doiRegisteredEvent1, doiRegisteredEvent2),
        ).asRight
        doiActionDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> doiActionEvent1).asRight
        doiActionDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(doiActionEvent1, doiActionEvent2),
        ).asRight
        curationDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> curation1).asRight
        curationDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(curation1, curation2),
        ).asRight
        springfieldDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> springfield1).asRight
        springfieldDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(springfield1, springfield2),
        ).asRight
        contentTypeDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> contentType1).asRight
        contentTypeDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(contentType1, contentType2),
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'deposit/findIdentifierWithType/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "findIdentifierWithType" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      (identifierDao.getByType(_: Seq[(DepositId, IdentifierType)])) expects Seq((depositId1, IdentifierType.DOI), (depositId1, IdentifierType.BAG_STORE)) once() returning Seq(
        (depositId1, IdentifierType.DOI) -> identifier2,
        (depositId1, IdentifierType.BAG_STORE) -> identifier1,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/ingestStepPagination/firstPage.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "ingestStepPagination" / "firstPage.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      ingestStepDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(step1, step2, step3, step1, step2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/ingestStepPagination/secondPage.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "ingestStepPagination" / "secondPage.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      ingestStepDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(step1, step2, step3, step1, step2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/ingestStepPagination/thirdPage.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "ingestStepPagination" / "thirdPage.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      ingestStepDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(step1, step2, step3, step1, step2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listAllContentTypesOfDeposit/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listAllContentTypesOfDeposit" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      contentTypeDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(contentType2, contentType1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listAllCuratorsOfDeposit/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listAllCuratorsOfDeposit" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      curationDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(curation1, curation2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listAllIngestStepsOfDeposit/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listAllIngestStepsOfDeposit" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      ingestStepDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(step1, step2, step3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listAllSpringfieldsOfDeposit/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listAllSpringfieldsOfDeposit" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId2) once() returning Seq(deposit2).asRight
      springfieldDao.getAll _ expects Seq(depositId2) once() returning Seq(
        depositId2 -> Seq(springfield1, springfield2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listAllStatesOfDeposit/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listAllStatesOfDeposit" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      stateDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(state1, state2, state3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithContentTypeFilterAll/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithContentTypeFilterAll" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId2) once() returning Seq(deposit2).asRight
      contentTypeDao.getCurrent _ expects Seq(depositId2) once() returning Seq(depositId2 -> contentType2).asRight
      val filters = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(ContentTypeValue.OCTET, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithCuratorFilterAll/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithCuratorFilterAll" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      curationDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(curation1),
      ).asRight
      val filters = DepositFilters(curatorFilter = Some(DepositCuratorFilter("archie001", SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithIngestStepFilterAll/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithIngestStepFilterAll" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      ingestStepDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> step1).asRight
      val filters = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithSameContentType/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithSameContentType" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId2) once() returning Seq(deposit2).asRight
      contentTypeDao.getCurrent _ expects Seq(depositId2) once() returning Seq(depositId2 -> contentType2).asRight
      val filters = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(ContentTypeValue.OCTET)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit3, deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithSameCurator/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithSameCurator" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      curationDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(curation2),
      ).asRight
      val filters = DepositFilters(curatorFilter = Some(DepositCuratorFilter("archie002")))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithSameDepositor/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithSameDepositor" / "plain.graphql"
    assume(input.exists, s"input file does not exist: $input")
    val query = input.contentAsString.replace(depositId1.toString, depositId2.toString)

    inSequence {
      depositDao.find _ expects Seq(depositId2) once() returning Seq(deposit2).asRight
      val filters = DepositFilters(depositorId = Some("user002"))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId2, depositId3) once() returning Seq(
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(query)
  }

  it should "resolve 'deposit/listDepositsWithSameIngestStep/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithSameIngestStep" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      ingestStepDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> step1).asRight
      val filters = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithSameState/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithSameState" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      stateDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> state1).asRight
      val filters = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/listDepositsWithStateFilterAll/plain.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "listDepositsWithStateFilterAll" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      stateDao.getCurrent _ expects Seq(depositId1) once() returning Seq(depositId1 -> state1).asRight
      val filters = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/statePagination/firstPage.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "statePagination" / "firstPage.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      stateDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(state1, state2, state3, state1, state2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/statePagination/secondPage.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "statePagination" / "secondPage.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      stateDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(state1, state2, state3, state1, state2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/statePagination/thirdPage.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "statePagination" / "thirdPage.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      stateDao.getAll _ expects Seq(depositId1) once() returning Seq(
        depositId1 -> Seq(state1, state2, state3, state1, state2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposit/timebasedSearch/plain.graphql' with 6 calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "timebasedSearch" / "plain.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId1) once() returning Seq(deposit1).asRight
      inAnyOrder {
        stateDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(state1, state2, state3, state1, state2),
        ).asRight
        ingestStepDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(step1, step2, step3, step1, step2),
        ).asRight
        curationDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(curation1, curation2, curation1),
        ).asRight
        springfieldDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(springfield1, springfield2, springfield1),
        ).asRight
        contentTypeDao.getAll _ expects Seq(depositId1) once() returning Seq(
          depositId1 -> Seq(contentType1, contentType2, contentType1),
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'deposit/nested.graphql' with n calls to the repository" in {
    val input = graphqlExamplesDir / "deposit" / "nested.graphql"

    val filters = DepositFilters()
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit1, deposit2, deposit3),
    ).asRight
    (stateDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
      depositId1 -> Seq(state1, state2),
      depositId2 -> Seq(state2, state3),
      depositId3 -> Seq.empty,
    ).asRight
    val filters1 = DepositFilters(stateFilter = Some(DepositStateFilter(state1.label)))
    val filters2 = DepositFilters(stateFilter = Some(DepositStateFilter(state2.label)))
    val filters3 = DepositFilters(stateFilter = Some(DepositStateFilter(state3.label)))
    depositDao.search _ expects Seq(filters3, filters1, filters2) once() returning Seq(
      filters1 -> Seq(deposit1, deposit2, deposit3),
      filters2 -> Seq(deposit1, deposit2, deposit3),
      filters3 -> Seq(deposit1, deposit2, deposit3),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithContentTypeAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithContentTypeAndDepositor" / "plain.graphql"

    val filters = DepositFilters(depositorId = Some("user001"), contentTypeFilter = Some(DepositContentTypeFilter(ContentTypeValue.ZIP)))
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit2, deposit3),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithCurationPerformedAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithCurationPerformedAndDepositor" / "plain.graphql"

    val filters = DepositFilters(depositorId = Some("user001"), curationPerformedFilter = Some(DepositCurationPerformedFilter(curationPerformed = true)))
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit2, deposit3),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithCurationRequiredAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithCurationRequiredAndDepositor" / "plain.graphql"

    val filters = DepositFilters(depositorId = Some("user001"), curationRequiredFilter = Some(DepositCurationRequiredFilter(curationRequired = true)))
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit2, deposit3),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithCuratorAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithCuratorAndDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user001"), curatorFilter = Some(DepositCuratorFilter("archie001")))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithDepositor/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user002"))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId2, depositId3) once() returning Seq(
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithDoiActionAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithDoiActionAndDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user001"), doiActionFilter = Some(DepositDoiActionFilter(DoiAction.CREATE)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithDoiRegisteredAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithDoiRegisteredAndDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user001"), doiRegisteredFilter = Some(DepositDoiRegisteredFilter(value = true)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithIngestStepAndDepositor/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithIngestStepAndDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user001"), ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.COMPLETED, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit2, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId2, depositId3) once() returning Seq(
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithIsNewVersionAndDepositor/plain.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithIsNewVersionAndDepositor" / "plain.graphql"

    val filters = DepositFilters(depositorId = Some("user001"), isNewVersionFilter = Some(DepositIsNewVersionFilter(isNewVersion = false)))
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit2, deposit3),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithStateAndDepositor/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithStateAndDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user001"), stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'depositor/listDepositsWithStateFilterAllAndDepositor/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "depositor" / "listDepositsWithStateFilterAllAndDepositor" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(depositorId = Some("user001"), stateFilter = Some(DepositStateFilter(StateLabel.DRAFT, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listAllDeposits/plain.graphql' with 19 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listAllDeposits" / "plain.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      inAnyOrder {
        (depositDao.lastModified(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> DateTime.now(),
          depositId2 -> DateTime.now(),
          depositId3 -> DateTime.now(),
        ).asRight
        (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> state1,
          depositId2 -> state2,
          depositId3 -> state3,
        ).asRight
        (ingestStepDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> step1,
          depositId2 -> step2,
          depositId3 -> step3,
        ).asRight
        (identifierDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> Seq(identifier1, identifier2),
          depositId2 -> Seq(identifier2, identifier3),
          depositId3 -> Seq.empty,
        ).asRight
        (doiRegisteredDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> doiRegisteredEvent1,
          depositId2 -> doiRegisteredEvent2,
        ).asRight
        (doiRegisteredDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> Seq(doiRegisteredEvent1, doiRegisteredEvent2),
          depositId2 -> Seq(doiRegisteredEvent2),
          depositId3 -> Seq.empty,
        ).asRight
        (doiActionDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> doiActionEvent1,
          depositId2 -> doiActionEvent2,
        ).asRight
        (doiActionDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> Seq(doiActionEvent1, doiActionEvent2),
          depositId2 -> Seq(doiActionEvent2),
          depositId3 -> Seq.empty,
        ).asRight
        (curationDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> curation1,
          depositId2 -> curation2,
        ).asRight
        (curationDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> Seq(curation1),
          depositId2 -> Seq(curation1, curation2),
          depositId3 -> Seq.empty,
        ).asRight
        (springfieldDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> springfield1,
          depositId2 -> springfield2,
        ).asRight
        (springfieldDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> Seq(springfield1),
          depositId2 -> Seq(springfield2, springfield1),
          depositId3 -> Seq.empty,
        ).asRight
        (contentTypeDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> contentType1,
          depositId2 -> contentType2,
        ).asRight
        (contentTypeDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
          depositId1 -> Seq(contentType1),
          depositId2 -> Seq(contentType2, contentType1),
          depositId3 -> Seq.empty,
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listAllDepositsWithAllCurators/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listAllDepositsWithAllCurators" / "plain.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (curationDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation2, curation1),
        depositId2 -> Seq(curation2, curation1),
        depositId3 -> Seq(curation2, curation1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listAllDepositsWithAllIngestSteps/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listAllDepositsWithAllIngestSteps" / "plain.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (ingestStepDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(step1, step2),
        depositId2 -> Seq(step2, step3),
        depositId3 -> Seq(step3, step1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listAllDepositsWithAllStates/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listAllDepositsWithAllStates" / "plain.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (stateDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(state1, state2),
        depositId2 -> Seq(state2, state3),
        depositId3 -> Seq(state3, state1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithContentType/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithContentType" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(ContentTypeValue.ZIP)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (contentTypeDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> contentType1,
        depositId2 -> contentType2,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithCurationPerformed/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithCurationPerformed" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(curationPerformedFilter = Some(DepositCurationPerformedFilter(curationPerformed = true)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (curationDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1),
        depositId2 -> Seq(curation2),
        depositId3 -> Seq.empty,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithCurationRequired/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithCurationRequired" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(curationRequiredFilter = Some(DepositCurationRequiredFilter(curationRequired = true)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (curationDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1),
        depositId2 -> Seq(curation2),
        depositId3 -> Seq.empty,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithCurator/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithCurator" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(curatorFilter = Some(DepositCuratorFilter("archie001")))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (curationDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1),
        depositId2 -> Seq(curation2),
        depositId3 -> Seq.empty,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithDoiAction/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithDoiAction" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(doiActionFilter = Some(DepositDoiActionFilter(DoiAction.CREATE, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (doiActionDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> doiActionEvent1,
        depositId2 -> doiActionEvent2,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithDoiRegistered/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithDoiRegistered" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(doiRegisteredFilter = Some(DepositDoiRegisteredFilter(value = true, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (doiRegisteredDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> doiRegisteredEvent1,
        depositId2 -> doiRegisteredEvent2,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithIngestStepFilterAll/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithIngestStepFilterAll" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithIsNewVersion/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithIsNewVersion" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(isNewVersionFilter = Some(DepositIsNewVersionFilter(isNewVersion = false)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (curationDao.getAll(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1),
        depositId2 -> Seq(curation2),
        depositId3 -> Seq.empty,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithState/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithState" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithStateFilterAll/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithStateFilterAll" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.DRAFT, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/listDepositsWithStateFilterAllAndIngestStepFilterAll/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "listDepositsWithStateFilterAllAndIngestStepFilterAll" / "plain.graphql"

    inSequence {
      val filters = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.REJECTED, SeriesFilter.ALL)), ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.ALL)))
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      (stateDao.getCurrent(_: Seq[DepositId])) expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaContentType.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaContentType.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      contentTypeDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> contentType1,
        depositId2 -> contentType2,
        depositId3 -> contentType1,
      ).asRight
      contentTypeDao.getDepositsById _ expects Seq(contentType1.id, contentType2.id) once() returning Seq(
        contentType1.id -> Some(deposit1),
        contentType2.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaContentTypes.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaContentTypes.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      contentTypeDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(contentType1, contentType2),
        depositId2 -> Seq(contentType2, contentType1),
        depositId3 -> Seq.empty,
      ).asRight
      contentTypeDao.getDepositsById _ expects Seq(contentType1.id, contentType2.id) once() returning Seq(
        contentType1.id -> Some(deposit1),
        contentType2.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaCurator.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaCurator.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      curationDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1),
        depositId2 -> Seq(curation2),
        depositId3 -> Seq(curation1),
      ).asRight
      curationDao.getDepositsById _ expects Seq(curation1.id, curation2.id) once() returning Seq(
        curation1.id -> Some(deposit1),
        curation2.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaCurators.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaCurators.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      curationDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1, curation2),
        depositId2 -> Seq(curation2, curation1),
        depositId3 -> Seq.empty,
      ).asRight
      curationDao.getDepositsById _ expects Seq(curation1.id, curation2.id) once() returning Seq(
        curation1.id -> Some(deposit1),
        curation2.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaIdentifier.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaIdentifier.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      identifierDao.getByType _ expects Seq(
        (depositId3, IdentifierType.DOI),
        (depositId2, IdentifierType.DOI),
        (depositId1, IdentifierType.DOI),
      ) once() returning Seq(
        (depositId1, IdentifierType.DOI) -> identifier1,
        (depositId2, IdentifierType.DOI) -> identifier2,
        (depositId3, IdentifierType.DOI) -> identifier3,
      ).asRight
      identifierDao.getDepositsById _ expects Seq(identifier3.id, identifier1.id, identifier2.id) once() returning Seq(
        identifier1.id -> Some(deposit1),
        identifier2.id -> Some(deposit2),
        identifier3.id -> Some(deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaIdentifiers.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaIdentifiers.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      identifierDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(identifier1, identifier2),
        depositId2 -> Seq(identifier2, identifier3),
        depositId3 -> Seq.empty,
      ).asRight
      identifierDao.getDepositsById _ expects Seq(identifier3.id, identifier1.id, identifier2.id) once() returning Seq(
        identifier1.id -> Some(deposit1),
        identifier2.id -> Some(deposit2),
        identifier3.id -> Some(deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaIngestStep.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaIngestStep.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      ingestStepDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> step1,
        depositId2 -> step2,
        depositId3 -> step3,
      ).asRight
      ingestStepDao.getDepositsById _ expects Seq(step3.id, step1.id, step2.id) once() returning Seq(
        step1.id -> Some(deposit1),
        step2.id -> Some(deposit2),
        step3.id -> Some(deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaIngestSteps.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaIngestSteps.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      ingestStepDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(step1, step2),
        depositId2 -> Seq(step2, step3),
        depositId3 -> Seq.empty,
      ).asRight
      ingestStepDao.getDepositsById _ expects Seq(step3.id, step1.id, step2.id) once() returning Seq(
        step1.id -> Some(deposit1),
        step2.id -> Some(deposit2),
        step3.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaSpringfield.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaSpringfield.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      springfieldDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> springfield1,
        depositId2 -> springfield2,
        depositId3 -> springfield1,
      ).asRight
      springfieldDao.getDepositsById _ expects Seq(springfield1.id, springfield2.id) once() returning Seq(
        springfield1.id -> Some(deposit1),
        springfield2.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaSpringfields.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaSpringfields.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      springfieldDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(springfield1, springfield2),
        depositId2 -> Seq(springfield2, springfield1),
        depositId3 -> Seq.empty,
      ).asRight
      springfieldDao.getDepositsById _ expects Seq(springfield1.id, springfield2.id) once() returning Seq(
        springfield1.id -> Some(deposit1),
        springfield2.id -> Some(deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaState.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaState.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      stateDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
      stateDao.getDepositsById _ expects Seq(state3.id, state1.id, state2.id) once() returning Seq(
        state1.id -> Some(deposit1),
        state2.id -> Some(deposit2),
        state3.id -> Some(deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposit/viaStates.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposit" / "viaStates.graphql"

    inSequence {
      val filters = DepositFilters()
      depositDao.search _ expects Seq(filters) once() returning Seq(
        filters -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      stateDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(state1, state2),
        depositId2 -> Seq(state2, state3),
        depositId3 -> Seq.empty,
      ).asRight
      stateDao.getDepositsById _ expects Seq(state3.id, state1.id, state2.id) once() returning Seq(
        state1.id -> Some(deposit1),
        state2.id -> Some(deposit2),
        state3.id -> Some(deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaContentType.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaContentType.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(contentType1.value, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(contentType2.value, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      contentTypeDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> contentType1,
        depositId2 -> contentType2,
        depositId3 -> contentType1,
      ).asRight
      depositDao.search _ expects Seq(filters3, filters2) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaContentTypes.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaContentTypes.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(contentType1.value, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(contentType2.value, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      contentTypeDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(contentType1, contentType2),
        depositId2 -> Seq(contentType2, contentType1),
        depositId3 -> Seq.empty,
      ).asRight
      depositDao.search _ expects Seq(filters3, filters2) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaCurator.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaCurator.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(curatorFilter = Some(DepositCuratorFilter(curation1.datamanagerUserId, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(curatorFilter = Some(DepositCuratorFilter(curation2.datamanagerUserId, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      curationDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1, curation2),
        depositId2 -> Seq(curation2, curation1),
        depositId3 -> Seq.empty,
      ).asRight
      depositDao.search _ expects Seq(filters3, filters2) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaCurators.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaCurators.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(curatorFilter = Some(DepositCuratorFilter(curation1.datamanagerUserId, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(curatorFilter = Some(DepositCuratorFilter(curation2.datamanagerUserId, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      curationDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(curation1, curation2),
        depositId2 -> Seq(curation2, curation1),
        depositId3 -> Seq.empty,
      ).asRight
      depositDao.search _ expects Seq(filters3, filters2) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaIngestStep.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaIngestStep.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(step1.step, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(step2.step, SeriesFilter.LATEST)))
      val filters4 = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(step3.step, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      ingestStepDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> step1,
        depositId2 -> step2,
        depositId3 -> step3,
      ).asRight
      depositDao.search _ expects Seq(filters2, filters3, filters4) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
        filters4 -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaIngestSteps.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaIngestSteps.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(step1.step, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(step2.step, SeriesFilter.LATEST)))
      val filters4 = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(step3.step, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      ingestStepDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(step1, step2),
        depositId2 -> Seq(step3, step1),
        depositId3 -> Seq.empty,
      ).asRight
      depositDao.search _ expects Seq(filters2, filters3, filters4) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
        filters4 -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaState.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaState.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(stateFilter = Some(DepositStateFilter(state1.label, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(stateFilter = Some(DepositStateFilter(state2.label, SeriesFilter.LATEST)))
      val filters4 = DepositFilters(stateFilter = Some(DepositStateFilter(state3.label, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      stateDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
      depositDao.search _ expects Seq(filters4, filters2, filters3) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
        filters4 -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/nestedDeposits/deposits/viaStates.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "nestedDeposits" / "deposits" / "viaStates.graphql"

    inSequence {
      val filters1 = DepositFilters()
      val filters2 = DepositFilters(stateFilter = Some(DepositStateFilter(state1.label, SeriesFilter.LATEST)))
      val filters3 = DepositFilters(stateFilter = Some(DepositStateFilter(state2.label, SeriesFilter.LATEST)))
      val filters4 = DepositFilters(stateFilter = Some(DepositStateFilter(state3.label, SeriesFilter.LATEST)))
      depositDao.search _ expects Seq(filters1) once() returning Seq(
        filters1 -> Seq(deposit1, deposit2, deposit3),
      ).asRight
      stateDao.getAll _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> Seq(state1, state2),
        depositId2 -> Seq(state3, state1),
        depositId3 -> Seq.empty,
      ).asRight
      depositDao.search _ expects Seq(filters4, filters2, filters3) once() returning Seq(
        filters2 -> Seq(deposit2, deposit3),
        filters3 -> Seq(deposit1, deposit2),
        filters4 -> Seq(deposit1, deposit3),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'deposits/pagination/firstPage.graphql' with 1 call to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "pagination" / "firstPage.graphql"

    val filters = DepositFilters()
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit1, deposit2, deposit3, deposit1, deposit2),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'deposits/pagination/secondPage.graphql' with 1 call to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "pagination" / "secondPage.graphql"

    val filters = DepositFilters()
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit1, deposit2, deposit3, deposit1, deposit2),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'deposits/pagination/thirdPage.graphql' with 1 call to the repository" in {
    val input = graphqlExamplesDir / "deposits" / "pagination" / "thirdPage.graphql"

    val filters = DepositFilters()
    depositDao.search _ expects Seq(filters) once() returning Seq(
      filters -> Seq(deposit1, deposit2, deposit3, deposit1, deposit2),
    ).asRight

    runQuery(input)
  }

  it should "resolve 'identifier/findWithTypeAndValue/plain.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "identifier" / "findWithTypeAndValue" / "plain.graphql"

    inSequence {
      identifierDao.getByTypesAndValues _ expects Seq(
        IdentifierType.DOI -> "10.5072/dans-a1b-cde2",
      ) once() returning Seq(
        (IdentifierType.DOI -> "10.5072/dans-a1b-cde2") -> identifier2,
      ).asRight
      identifierDao.getDepositsById _ expects Seq(identifier2.id) once() returning Seq(
        identifier2.id -> Some(deposit1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'node/onContentType.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onContentType.graphql"
    val contentType27 = contentType1.copy(id = "27")

    inSequence {
      contentTypeDao.getById _ expects Seq(contentType27.id) once() returning Seq(contentType27).asRight
      inAnyOrder {
        contentTypeDao.getDepositsById _ expects Seq(contentType27.id) once() returning Seq(
          contentType27.id -> Some(deposit1),
        ).asRight
        val filters = DepositFilters(contentTypeFilter = Some(DepositContentTypeFilter(ContentTypeValue.ZIP, SeriesFilter.ALL)))
        depositDao.search _ expects Seq(filters) once() returning Seq(
          filters -> Seq(deposit1, deposit2),
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'node/onCurator.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onCurator.graphql"
    val curation9 = curation1.copy(id = "9")

    inSequence {
      curationDao.getById _ expects Seq(curation9.id) once() returning Seq(curation9).asRight
      inAnyOrder {
        curationDao.getDepositsById _ expects Seq(curation9.id) once() returning Seq(
          curation9.id -> Some(deposit1),
        ).asRight
        val filters = DepositFilters(curatorFilter = Some(DepositCuratorFilter("archie001", SeriesFilter.ALL)))
        depositDao.search _ expects Seq(filters) once() returning Seq(
          filters -> Seq(deposit1, deposit2),
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'node/onDeposit.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onDeposit.graphql"

    inSequence {
      depositDao.find _ expects Seq(depositId2) once() returning Seq(deposit2).asRight
      stateDao.getCurrent _ expects Seq(depositId2) once() returning Seq(depositId2 -> state2).asRight
    }

    runQuery(input)
  }

  it should "resolve 'node/onIdentifier.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onIdentifier.graphql"

    inSequence {
      identifierDao.getById _ expects Seq(identifier1.id) once() returning Seq(identifier1).asRight
      identifierDao.getDepositsById _ expects Seq(identifier1.id) once() returning Seq(
        identifier1.id -> Some(deposit1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'node/onIngestStep.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onIngestStep.graphql"
    val step0 = step1.copy(id = "0")

    inSequence {
      ingestStepDao.getById _ expects Seq(step0.id) once() returning Seq(step0).asRight
      inAnyOrder {
        ingestStepDao.getDepositsById _ expects Seq(step0.id) once() returning Seq(
          step0.id -> Some(deposit1),
        ).asRight
        val filters = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.ALL)))
        depositDao.search _ expects Seq(filters) once() returning Seq(
          filters -> Seq(deposit1, deposit2),
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'node/onSpringfield.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onSpringfield.graphql"
    val springfield0 = springfield1.copy(id = "0")

    inSequence {
      springfieldDao.getById _ expects Seq(springfield0.id) once() returning Seq(springfield0).asRight
      springfieldDao.getDepositsById _ expects Seq(springfield0.id) once() returning Seq(
        springfield0.id -> Some(deposit1),
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'node/onState.graphql' with 3 calls to the repository" in {
    val input = graphqlExamplesDir / "node" / "onState.graphql"
    val state5 = state2.copy(id = "5")

    inSequence {
      stateDao.getById _ expects Seq(state5.id) once() returning Seq(state5).asRight
      inAnyOrder {
        stateDao.getDepositsById _ expects Seq(state5.id) once() returning Seq(
          state5.id -> Some(deposit1),
        ).asRight
        val filters = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.DRAFT, SeriesFilter.ALL)))
        depositDao.search _ expects Seq(filters) once() returning Seq(
          filters -> Seq(deposit1, deposit3),
        ).asRight
      }
    }

    runQuery(input)
  }

  it should "resolve 'nodes/onContentType.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onContentType.graphql"

    contentTypeDao.getById _ expects Seq("26", "27") once() returning Seq(contentType1, contentType2).asRight

    runQuery(input)
  }

  it should "resolve 'nodes/onCurator.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onCurator.graphql"

    curationDao.getById _ expects Seq("0", "1") once() returning Seq(curation1, curation1).asRight

    runQuery(input)
  }

  it should "resolve 'nodes/onDeposit.graphql' with 2 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onDeposit.graphql"

    val depositId4: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000004")
    val depositId5: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000005")

    inSequence {
      depositDao.find _ expects Seq(depositId1, depositId5, depositId2, depositId3, depositId4) once() returning Seq(
        deposit1,
        deposit2,
        deposit3,
        deposit1,
        deposit2,
      ).asRight
      stateDao.getCurrent _ expects Seq(depositId1, depositId2, depositId3) once() returning Seq(
        depositId1 -> state1,
        depositId2 -> state2,
        depositId3 -> state3,
      ).asRight
    }

    runQuery(input)
  }

  it should "resolve 'nodes/onIdentifier.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onIdentifier.graphql"

    identifierDao.getById _ expects Seq("3", "0", "1", "2") once() returning Seq(
      identifier1,
      identifier2,
      identifier3,
      identifier1,
    ).asRight

    runQuery(input)
  }

  it should "resolve 'nodes/onIngestStep.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onIngestStep.graphql"

    ingestStepDao.getById _ expects Seq("3", "0", "4", "1", "5", "2", "6") once() returning Seq(
      step1,
      step2,
      step3,
      step1,
      step2,
      step3,
      step1,
    ).asRight

    runQuery(input)
  }

  it should "resolve 'nodes/onSpringfield.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onSpringfield.graphql"

    springfieldDao.getById _ expects Seq("0", "1", "2") once() returning Seq(
      springfield1,
      springfield2,
      springfield1,
    ).asRight

    runQuery(input)
  }

  it should "resolve 'nodes/onState.graphql' with 1 calls to the repository" in {
    val input = graphqlExamplesDir / "nodes" / "onState.graphql"

    stateDao.getById _ expects Seq("3", "0", "4", "1", "5", "2") once() returning Seq(
      state1,
      state2,
      state3,
      state1,
      state2,
      state3,
    ).asRight

    runQuery(input)
  }

  private def runQuery(graphQLFile: File): Unit = {
    assume(graphQLFile.exists, s"input file does not exist: $graphQLFile")
    runQuery(graphQLFile.contentAsString)
  }

  private def runQuery(query: String): Unit = {
    val inputBody = compact(render("query" -> query))
    post(uri = "/", body = inputBody.getBytes) {
      parse(body) \ "errors" match {
        case JNothing => // do nothing, no errors
        case jsonErrors => read[Seq[Error]](compact(render(jsonErrors))) shouldBe empty
      }

      status shouldBe 200
    }
  }

  case class Location(line: Int, column: Int)
  case class Error(message: String, path: Seq[String], locations: Seq[Location])
}
