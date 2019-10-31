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
package nl.knaw.dans.easy.properties.app.repository.sql

import java.sql.PreparedStatement
import java.util.UUID

import cats.data.NonEmptyList
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ DepositIngestStepFilter, IngestStepLabel }
import nl.knaw.dans.easy.properties.app.model.state.{ DepositStateFilter, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ DepositId, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import nl.knaw.dans.easy.properties.fixture.TestSupportFixture
import org.scalactic.{ AbstractStringUniformity, Uniformity }
import org.scalamock.scalatest.MockFactory

class QueryGeneratorSpec extends TestSupportFixture with MockFactory {

  val whiteSpaceNormalised: Uniformity[String] = {
    new AbstractStringUniformity {
      def normalized(s: String): String = {
        s.replace("\n", " ")
          .replaceAll("\\s\\s+", " ")
          .replaceAll("\\( ", "(")
          .replaceAll(" \\)", ")")
      }

      override def toString: String = "whiteSpaceNormalised"
    }
  }

  def setStringMock(filler: PrepStatementResolver, expectedValue: String): Unit = {
    val ps = mock[PreparedStatement]

    ps.setString _ expects(1, expectedValue) once()

    filler(ps, 1)
  }

  def setDepositIdMock(filler: PrepStatementResolver, expectedValue: DepositId): Unit = {
    setStringMock(filler, expectedValue.toString)
  }

  def setIntMock(filler: PrepStatementResolver, expectedValue: Int): Unit = {
    val ps = mock[PreparedStatement]

    ps.setInt _ expects(1, expectedValue) once()

    filler(ps, 1)
  }

  "getAllDeposits" should "query for all deposits" in {
    QueryGenerator.getAllDeposits shouldBe "SELECT * FROM Deposit;"
  }

  "findDeposits" should "render the depositIds as comma separated question marks" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, values) = QueryGenerator.findDeposits(depositIds)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE depositId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size depositIds.size
    forEvery(values zip depositIds.toList) { case (value, expectedValue) =>
      setDepositIdMock(value, expectedValue)
    }
  }

  "searchDeposits" should "render a query that selects all deposits when no filters are set" in {
    val filter = DepositFilters()
    val (query, values) = QueryGenerator.searchDeposits(filter)

    query shouldBe "SELECT * FROM Deposit;"
    values shouldBe empty
  }

  it should "render a query that searches for deposits of a certain depositor" in {
    val filter = DepositFilters(depositorId = Some("user001"))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE depositorId = ?;""".stripMargin
    val expectedValues = List("user001")

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits with a null bag name" in {
    val filter = DepositFilters(bagName = Some(null))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE bagName IS NULL;""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values shouldBe empty
  }

  it should "render a query that searches for deposits with a certain bag name" in {
    val filter = DepositFilters(bagName = Some("my-bag"))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE bagName = ?;""".stripMargin
    val expectedValues = List("my-bag")

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits of a certain depositor and with a certain bag name" in {
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE depositorId = ?
        |AND bagName = ?;""".stripMargin
    val expectedValues = List("user001", "my-bag")

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits with a certain 'latest state'" in {
    val filter = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT State.depositId
        |  FROM State
        |  INNER JOIN (
        |    SELECT depositId, max(timestamp) AS max_timestamp
        |    FROM State
        |    GROUP BY depositId
        |  ) AS StateWithMaxTimestamp
        |  ON State.timestamp = StateWithMaxTimestamp.max_timestamp
        |  WHERE label = ?
        |) AS StateSearchResult
        |ON Deposit.depositId = StateSearchResult.depositId;""".stripMargin
    val expectedValues = List(StateLabel.ARCHIVED.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits that at some time had this certain state label" in {
    val filter = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.ALL)))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM State
        |  WHERE label = ?
        |) AS StateSearchResult ON Deposit.depositId = StateSearchResult.depositId;""".stripMargin
    val expectedValues = List(StateLabel.SUBMITTED.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits of a certain depositor, with a certain bagName and with a certain 'latest state'" in {
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM (
        |  SELECT *
        |  FROM Deposit
        |  WHERE depositorId = ?
        |  AND bagName = ?
        |) AS SelectedDeposits
        |INNER JOIN (
        |  SELECT State.depositId
        |  FROM State
        |  INNER JOIN (
        |    SELECT depositId, max(timestamp) AS max_timestamp
        |    FROM State
        |    GROUP BY depositId
        |  ) AS StateWithMaxTimestamp
        |  ON State.timestamp = StateWithMaxTimestamp.max_timestamp
        |  WHERE label = ?
        |) AS StateSearchResult
        |ON SelectedDeposits.depositId = StateSearchResult.depositId;""".stripMargin
    val expectedValues = List("user001", "my-bag", StateLabel.SUBMITTED.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits with a certain 'latest ingest step'" in {
    val filter = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.LATEST)))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT SimpleProperties.depositId
        |  FROM SimpleProperties
        |  INNER JOIN (
        |    SELECT depositId, max(timestamp) AS max_timestamp
        |    FROM SimpleProperties
        |    WHERE key = ?
        |    GROUP BY depositId
        |  ) AS SimplePropertiesWithMaxTimestamp
        |  ON SimpleProperties.timestamp = SimplePropertiesWithMaxTimestamp.max_timestamp
        |  WHERE value = ?
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedSize = List("ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedSize.size
    forEvery(values zip expectedSize) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits that sometime had this certain ingest step" in {
    val filter = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)))
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedSize = List("ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedSize.size
    forEvery(values zip expectedSize) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits with a certain 'latest state' and that sometime had this certain ingest step" in {
    val filter = DepositFilters(
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT State.depositId
        |  FROM State
        |  INNER JOIN (
        |    SELECT depositId, max(timestamp) AS max_timestamp
        |    FROM State
        |    GROUP BY depositId
        |  ) AS StateWithMaxTimestamp
        |  ON State.timestamp = StateWithMaxTimestamp.max_timestamp
        |  WHERE label = ?
        |) AS StateSearchResult
        |ON Deposit.depositId = StateSearchResult.depositId
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedSize = List(StateLabel.SUBMITTED.toString, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedSize.size
    forEvery(values zip expectedSize) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits of a certain depositor, with a certain bagName, with a certain 'latest state' and that sometime had this certain ingest step" in {
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM (
        |  SELECT *
        |  FROM Deposit
        |  WHERE depositorId = ?
        |  AND bagName = ?
        |) AS SelectedDeposits
        |INNER JOIN (
        |  SELECT State.depositId
        |  FROM State
        |  INNER JOIN (
        |    SELECT depositId, max(timestamp) AS max_timestamp
        |    FROM State
        |    GROUP BY depositId
        |  ) AS StateWithMaxTimestamp
        |  ON State.timestamp = StateWithMaxTimestamp.max_timestamp
        |  WHERE label = ?
        |) AS StateSearchResult
        |ON SelectedDeposits.depositId = StateSearchResult.depositId
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedSize = List("user001", "my-bag", StateLabel.SUBMITTED.toString, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedSize.size
    forEvery(values zip expectedSize) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  "getLastModifiedDate" should "generate a UNION query" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, values) = QueryGenerator.getLastModifiedDate(depositIds)

    val expectedQuery =
      s"""SELECT depositId, MAX(max) AS max_timestamp
         |FROM (
         |  (SELECT depositId, MAX(creationTimestamp) AS max FROM Deposit WHERE depositId IN (?, ?, ?, ?, ?) GROUP BY depositId) UNION ALL
         |  (SELECT depositId, MAX(timestamp) AS max FROM State WHERE depositId IN (?, ?, ?, ?, ?) GROUP BY depositId) UNION ALL
         |  (SELECT depositId, MAX(timestamp) AS max FROM Identifier WHERE depositId IN (?, ?, ?, ?, ?) GROUP BY depositId) UNION ALL
         |  (SELECT depositId, MAX(timestamp) AS max FROM Curation WHERE depositId IN (?, ?, ?, ?, ?) GROUP BY depositId) UNION ALL
         |  (SELECT depositId, MAX(timestamp) AS max FROM Springfield WHERE depositId IN (?, ?, ?, ?, ?) GROUP BY depositId) UNION ALL
         |  (SELECT depositId, MAX(timestamp) AS max FROM SimpleProperties WHERE depositId IN (?, ?, ?, ?, ?) GROUP BY depositId)
         |) AS max_timestamps
         |GROUP BY depositId;""".stripMargin
    val expectedSize = Seq.fill(6)(depositIds.map(_.toString).toList).flatten

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedSize.size
    forEvery(values zip expectedSize) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  "getElementsById" should "generate a query that, given a table name and id column name, finds the elements associated with the given ids" in {
    val ids = NonEmptyList.fromListUnsafe((1 to 5).map(_.toString).toList)
    val (query, values) = QueryGenerator.getElementsById("State", "stateId")(ids)

    val expectedQuery =
      """SELECT *
        |FROM State
        |WHERE stateId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size ids.size
    forEvery(values zip ids.map(_.toInt).toList) { case (value, expectedValue) =>
      setIntMock(value, expectedValue)
    }
  }

  "getCurrentElementByDepositId" should "generate a query that, given a table name, finds the element that is latest associated with the given depositIds" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, values) = QueryGenerator.getCurrentElementByDepositId("State")(depositIds)

    val expectedQuery =
      """SELECT *
        |FROM State
        |INNER JOIN (
        |  SELECT depositId, max(timestamp) AS max_timestamp
        |  FROM State
        |  WHERE depositId IN (?, ?, ?, ?, ?)
        |  GROUP BY depositId
        |) AS deposit_with_max_timestamp USING (depositId)
        |WHERE timestamp = max_timestamp;""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size depositIds.size
    forEvery(values zip depositIds.toList) { case (value, expectedValue) =>
      setDepositIdMock(value, expectedValue)
    }
  }

  "getAllElementsByDepositId" should "generate a query that, given a table name, finds all elements that are/were associated with the given depositIds" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, values) = QueryGenerator.getAllElementsByDepositId("State")(depositIds)

    val expectedQuery =
      """SELECT *
        |FROM State
        |WHERE depositId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size depositIds.size
    forEvery(values zip depositIds.toList) { case (value, expectedValue) =>
      setDepositIdMock(value, expectedValue)
    }
  }

  "getDepositsById" should "generate a query that, given a table name and id column name, finds deposits corresponding to the given ids" in {
    val ids = NonEmptyList.fromListUnsafe((1 to 5).map(_.toString).toList)
    val (query, values) = QueryGenerator.getDepositsById("State", "stateId")(ids)

    val expectedQuery =
      """SELECT stateId, Deposit.depositId, bagName, creationTimestamp, depositorId, origin
        |FROM Deposit
        |INNER JOIN State
        |ON Deposit.depositId = State.depositId
        |WHERE stateId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size ids.size
    forEvery(values zip ids.map(_.toInt).toList) { case (value, expectedValue) =>
      setIntMock(value, expectedValue)
    }
  }

  "getIdentifierByDepositIdAndType" should "produce a query that selects the identifiers that belong to the given deposits and have the given identifier types" in {
    val ids = NonEmptyList.of(
      (UUID.fromString("00000000-0000-0000-0000-000000000002"), IdentifierType.URN),
      (UUID.fromString("00000000-0000-0000-0000-000000000002"), IdentifierType.DOI),
      (UUID.fromString("00000000-0000-0000-0000-000000000004"), IdentifierType.FEDORA),
      (UUID.fromString("00000000-0000-0000-0000-000000000001"), IdentifierType.BAG_STORE),
    )
    val (query, values) = QueryGenerator.getIdentifierByDepositIdAndType(ids)

    val expectedQuery =
      """SELECT identifierId, depositId, identifierSchema, identifierValue, timestamp
        |FROM Identifier
        |WHERE (depositId = ? AND identifierSchema = ?)
        |OR (depositId = ? AND identifierSchema = ?)
        |OR (depositId = ? AND identifierSchema = ?)
        |OR (depositId = ? AND identifierSchema = ?);""".stripMargin
    val expectedValues = ids.toList.flatMap { case (depositId, idType) => List(depositId.toString, idType.toString) }

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "produce a query with no OR's in it when only one (depositId, idType) tuple is looked for" in {
    val ids = NonEmptyList.of(
      (UUID.fromString("00000000-0000-0000-0000-000000000001"), IdentifierType.BAG_STORE),
    )
    val (query, values) = QueryGenerator.getIdentifierByDepositIdAndType(ids)

    val expectedQuery =
      """SELECT identifierId, depositId, identifierSchema, identifierValue, timestamp
        |FROM Identifier
        |WHERE (depositId = ? AND identifierSchema = ?);""".stripMargin
    val expectedValues = ids.toList.flatMap { case (depositId, idType) => List(depositId.toString, idType.toString) }

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  "getIdentifierByTypeAndValue" should "produce a query that selects the identifiers according to their type and value" in {
    val ids = NonEmptyList.of(
      (IdentifierType.URN, "abc"),
      (IdentifierType.DOI, "foo"),
      (IdentifierType.FEDORA, "bar"),
      (IdentifierType.BAG_STORE, "def"),
    )
    val (query, values) = QueryGenerator.getIdentifierByTypeAndValue(ids)

    val expectedQuery =
      """SELECT identifierId, identifierSchema, identifierValue, timestamp
        |FROM Identifier
        |WHERE (identifierSchema = ? AND identifierValue = ?)
        |OR (identifierSchema = ? AND identifierValue = ?)
        |OR (identifierSchema = ? AND identifierValue = ?)
        |OR (identifierSchema = ? AND identifierValue = ?);""".stripMargin
    val expectedValues = ids.toList.flatMap { case (depositId, idType) => List(depositId.toString, idType.toString) }

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "produce a query with no OR's in it when only one (depositId, idType) tuple is looked for" in {
    val ids = NonEmptyList.of(
      (IdentifierType.BAG_STORE, "foobar"),
    )
    val (query, values) = QueryGenerator.getIdentifierByTypeAndValue(ids)

    val expectedQuery =
      """SELECT identifierId, identifierSchema, identifierValue, timestamp
        |FROM Identifier
        |WHERE (identifierSchema = ? AND identifierValue = ?);""".stripMargin
    val expectedValues = ids.toList.flatMap { case (depositId, idType) => List(depositId.toString, idType.toString) }

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  "getSimplePropsElementsById" should "produce a query that selects simple properties by their ID for the given key" in {
    val ids = NonEmptyList.fromListUnsafe((1 to 5).map(_.toString).toList)
    val (query, keyValue :: values) = QueryGenerator.getSimplePropsElementsById("my-key")(ids)
    
    val expectedQuery =
      """SELECT *
        |FROM SimpleProperties
        |WHERE key = ?
        |AND propertyId IN (?, ?, ?, ?, ?);""".stripMargin
    
    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size ids.size
    
    inSequence {
      setStringMock(keyValue, "my-key")
      forEvery(values zip ids.map(_.toInt).toList) { case (value, expectedValue) =>
        setIntMock(value, expectedValue)
      }
    }
  }

  "getSimplePropsCurrentElementByDepositId" should "produce a query that selects simple properties by their depositId for the given key" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, key1Value :: vs) = QueryGenerator.getSimplePropsCurrentElementByDepositId("my-key")(depositIds)
    val values = vs.init
    val key2Value = vs.last
    
    val expectedQuery =
      """SELECT *
        |FROM SimpleProperties
        |INNER JOIN (
        |  SELECT depositId, max(timestamp) AS max_timestamp
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND depositId IN (?, ?, ?, ?, ?)
        |  GROUP BY depositId
        |) AS deposit_with_max_timestamp USING (depositId)
        |WHERE timestamp = max_timestamp
        |AND key = ?;""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size depositIds.size
    
    inSequence {
      setStringMock(key1Value, "my-key")
      forEvery(values zip depositIds.map(_.toString).toList) { case (value, expectedValue) =>
        setStringMock(value, expectedValue)
      }
      setStringMock(key2Value, "my-key")
    }
  }

  "getSimplePropsAllElementsByDepositId" should "produce a query that selects all simple properties by their depositId for the given key" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, keyValue :: values) = QueryGenerator.getSimplePropsAllElementsByDepositId("my-key")(depositIds)

    val expectedQuery =
      """SELECT *
        |FROM SimpleProperties
        |WHERE key = ?
        |AND depositId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size depositIds.size

    inSequence {
      setStringMock(keyValue, "my-key")
      forEvery(values zip depositIds.map(_.toString).toList) { case (value, expectedValue) =>
        setStringMock(value, expectedValue)
      }
    }
  }

  "getSimplePropsDepositsById" should "produce a query that selects deposits based on the simple properties id for the given key" in {
    val ids = NonEmptyList.fromListUnsafe((1 to 5).map(_.toString).toList)
    val (query, keyValue :: values) = QueryGenerator.getSimplePropsDepositsById("my-key")(ids)

    val expectedQuery =
      """SELECT propertyId, Deposit.depositId, bagName, creationTimestamp, depositorId, origin
        |FROM Deposit
        |INNER JOIN SimpleProperties ON Deposit.depositId = SimpleProperties.depositId 
        |WHERE key = ?
        |AND propertyId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size ids.size

    inSequence {
      setStringMock(keyValue, "my-key")
      forEvery(values zip ids.map(_.toInt).toList) { case (value, expectedValue) =>
        setIntMock(value, expectedValue)
      }
    }
  }

  "storeDeposit" should "yield the query for inserting a deposit into the database" in {
    QueryGenerator.storeDeposit shouldBe "INSERT INTO Deposit (depositId, bagName, creationTimestamp, depositorId, origin) VALUES (?, ?, ?, ?, ?);"
  }

  "storeBagName" should "yield the query for inserting a deposit's bagName into the database" in {
    QueryGenerator.storeBagName shouldBe "UPDATE Deposit SET bagName = ? WHERE depositId = ? AND (bagName IS NULL OR bagName='');"
  }

  "storeCuration" should "yield the query for inserting a Curation into the database if isNewVersion is defined" in {
    QueryGenerator.storeCuration(true) shouldBe "INSERT INTO Curation (depositId, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp, isNewVersion) VALUES (?, ?, ?, ?, ?, ?, ?);"
  }

  it should "yield the query for inserting a Curation into the database if isNewVersion is not defined" in {
    QueryGenerator.storeCuration(false) shouldBe "INSERT INTO Curation (depositId, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp) VALUES (?, ?, ?, ?, ?, ?);"
  }

  "storeSimpleProperty" should "yield the query for inserting an Identifier into the database" in {
    QueryGenerator.storeSimpleProperty shouldBe "INSERT INTO SimpleProperties (depositId, key, value, timestamp) VALUES (?, ?, ?, ?);"
  }

  "storeIdentifier" should "yield the query for inserting an Identifier into the database" in {
    QueryGenerator.storeIdentifier shouldBe "INSERT INTO Identifier (depositId, identifierSchema, identifierValue, timestamp) VALUES (?, ?, ?, ?);"
  }

  "storeSpringfield" should "yield the query for inserting a Springfield configuration into the database" in {
    QueryGenerator.storeSpringfield shouldBe "INSERT INTO Springfield (depositId, domain, springfield_user, collection, playmode, timestamp) VALUES (?, ?, ?, ?, ?, ?);"
  }

  "storeState" should "yield the query for inserting a State into the database" in {
    QueryGenerator.storeState shouldBe "INSERT INTO State (depositId, label, description, timestamp) VALUES (?, ?, ?, ?);"
  }

  "deleteByDepositId" should "yield the query for deleting a State from the database" in {
    QueryGenerator.deleteByDepositId(tableName = "State")(NonEmptyList.of(UUID.randomUUID(), UUID.randomUUID())) shouldBe
      "DELETE FROM State WHERE depositId IN (?, ?);"
  }

  it should "yield the query for deleting a ContentType from the database" in {
    QueryGenerator.deleteByDepositId(tableName = "SimpleProperty", key ="contentType")(NonEmptyList.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())) shouldBe
      "DELETE FROM SimpleProperty WHERE key = 'contentType' AND depositId IN (?, ?, ?);"
  }
}
