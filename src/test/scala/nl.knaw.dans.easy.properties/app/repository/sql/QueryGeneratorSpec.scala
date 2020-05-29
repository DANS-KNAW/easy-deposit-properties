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
import nl.knaw.dans.easy.properties.app.model.sort.{ DepositOrder, DepositOrderField, OrderDirection }
import nl.knaw.dans.easy.properties.app.model.state.{ DepositStateFilter, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ AtTime, Between, DepositId, EarlierThan, LaterThan, NotBetween, Origin, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.{ DepositFilters, DepositorIdFilters }
import nl.knaw.dans.easy.properties.fixture.TestSupportFixture
import org.joda.time.DateTime
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

  def setTimestampMock(filler: PrepStatementResolver, expectedValue: java.sql.Timestamp): Unit = {
    val ps = mock[PreparedStatement]

    (ps.setTimestamp(_: Int, _: java.sql.Timestamp)) expects(1, expectedValue) once()

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

  it should "render a query that sorts the deposits in descending order on origin" in {
    val filters = DepositFilters(sort = Option(DepositOrder(DepositOrderField.ORIGIN, OrderDirection.DESC)))
    val (query, values) = QueryGenerator.searchDeposits(filters)

    query shouldBe "SELECT * FROM Deposit ORDER BY origin DESC;"
    values shouldBe empty
  }

  it should "render a query that selects all deposits created earlier than the given timestamp" in {
    val dt = DateTime.parse("2019-03-03T00:00:00+01:00")
    val filter = DepositFilters(
      creationTimeFilter = Some(EarlierThan(dt))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE creationTimestamp < ?::timestamp with time zone;""".stripMargin
    val expectedValues = List(dt)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setTimestampMock(value, expectedValue)
    }
  }

  it should "render a query that selects all deposits last modified earlier than the given timestamp" in {
    val dt = DateTime.parse("2019-03-03T00:00:00+01:00")
    val filter = DepositFilters(
      lastModifiedTimeFilter = Some(EarlierThan(dt))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN LastModified
        |ON Deposit.depositId = LastModified.depositId
        |WHERE max_timestamp < ?::timestamp with time zone;""".stripMargin
    val expectedValues = List(dt)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setTimestampMock(value, expectedValue)
    }
  }

  it should "render a query that selects all deposits last modified between the given timestamps" in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val filter = DepositFilters(
      lastModifiedTimeFilter = Some(Between(dt1, dt2))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN LastModified
        |ON Deposit.depositId = LastModified.depositId
        |WHERE max_timestamp < ?::timestamp with time zone
        |AND max_timestamp > ?::timestamp with time zone;""".stripMargin
    val expectedValues = List(dt1, dt2)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setTimestampMock(value, expectedValue)
    }
  }

  it should "render a query that selects all deposits last modified not between the given timestamps" in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val filter = DepositFilters(
      lastModifiedTimeFilter = Some(NotBetween(dt1, dt2))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN LastModified
        |ON Deposit.depositId = LastModified.depositId
        |WHERE (max_timestamp > ?::timestamp with time zone OR max_timestamp < ?::timestamp with time zone);""".stripMargin
    val expectedValues = List(dt2, dt1)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setTimestampMock(value, expectedValue)
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor
      |and created later than the given timestamp
      |and sorts the deposits in ascending order on bagName""".stripMargin in {
    val dt = DateTime.parse("2019-03-03T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      creationTimeFilter = Some(LaterThan(dt)),
      sort = Some(DepositOrder(DepositOrderField.BAG_NAME, OrderDirection.ASC))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |WHERE depositorId = ?
        |AND creationTimestamp > ?::timestamp with time zone
        |ORDER BY bagName ASC;""".stripMargin
    val expectedValues = List("user001", dt)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor
      |and last modified later than the given timestamp
      |and sorts the deposits in ascending order on bagName""".stripMargin in {
    val dt = DateTime.parse("2019-03-03T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      lastModifiedTimeFilter = Some(LaterThan(dt)),
      sort = Some(DepositOrder(DepositOrderField.BAG_NAME, OrderDirection.ASC))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN LastModified
        |ON Deposit.depositId = LastModified.depositId
        |WHERE max_timestamp > ?::timestamp with time zone
        |AND depositorId = ?
        |ORDER BY bagName ASC;""".stripMargin
    val expectedValues = List("user001", dt)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
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
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List("ingest-step", "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for deposits that sometime had this certain ingest step and created at the given timestamp" in {
    val dt = DateTime.parse("2019-03-03T00:00:00+01:00")
    val filter = DepositFilters(
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      creationTimeFilter = Some(AtTime(dt)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM (
        |  SELECT *
        |  FROM Deposit
        |  WHERE creationTimestamp = ?::timestamp with time zone
        |) AS SelectedDeposits
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List(dt, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should "render a query that searches for deposits that sometime had this certain ingest step and last modified at the given timestamp" in {
    val dt = DateTime.parse("2019-03-03T00:00:00+01:00")
    val filter = DepositFilters(
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      lastModifiedTimeFilter = Some(AtTime(dt)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId
        |INNER JOIN LastModified
        |ON Deposit.depositId = LastModified.depositId
        |WHERE max_timestamp = ?::timestamp with time zone;""".stripMargin
    val expectedValue = List("ingest-step", IngestStepLabel.FEDORA.toString, dt)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should "render a query that searches for deposits that sometime had this certain ingest step and sort them in descending order on creation timestamp" in {
    val filter = DepositFilters(
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      sort = Some(DepositOrder(DepositOrderField.CREATION_TIMESTAMP, OrderDirection.DESC))
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM Deposit
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId
        |ORDER BY creationTimestamp DESC;""".stripMargin
    val expectedValue = List("ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
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
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List(StateLabel.SUBMITTED.toString, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List("user001", "my-bag", StateLabel.SUBMITTED.toString, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor,
      |with a certain bagName,
      |with a certain 'latest state',
      |that sometime had this certain ingest step,
      |was created between two given timestamps
      |and sorted in descending order of depositId""".stripMargin in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      creationTimeFilter = Some(Between(dt1, dt2)),
      sort = Option(DepositOrder(DepositOrderField.DEPOSIT_ID, OrderDirection.DESC)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM (
        |  SELECT *
        |  FROM Deposit
        |  WHERE depositorId = ?
        |  AND bagName = ?
        |  AND creationTimestamp < ?::timestamp with time zone
        |  AND creationTimestamp > ?::timestamp with time zone
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId
        |ORDER BY depositId DESC;""".stripMargin
    val expectedValue = List(
      "user001",
      "my-bag",
      dt1,
      dt2,
      StateLabel.SUBMITTED.toString,
      "ingest-step",
      IngestStepLabel.FEDORA.toString,
    )

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor,
      |with a certain bagName,
      |with a certain 'latest state',
      |that sometime had this certain ingest step,
      |was last modified between two given timestamps
      |and sorted in descending order of depositId""".stripMargin in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      lastModifiedTimeFilter = Some(Between(dt1, dt2)),
      sort = Option(DepositOrder(DepositOrderField.DEPOSIT_ID, OrderDirection.DESC)),
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId
        |INNER JOIN LastModified
        |ON SelectedDeposits.depositId = LastModified.depositId
        |WHERE max_timestamp < ?::timestamp with time zone
        |AND max_timestamp > ?::timestamp with time zone
        |ORDER BY depositId DESC;""".stripMargin
    val expectedValue = List(
      "user001",
      "my-bag",
      StateLabel.SUBMITTED.toString,
      "ingest-step",
      IngestStepLabel.FEDORA.toString,
      dt1,
      dt2,
    )

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor,
      |with a certain bagName,
      |with a certain 'latest state',
      |that sometime had this certain ingest step,
      |was created before and after two given timestamps
      |and sorted in descending order of depositId""".stripMargin in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      creationTimeFilter = Some(NotBetween(dt1, dt2)),
      sort = Option(DepositOrder(DepositOrderField.DEPOSIT_ID, OrderDirection.DESC)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM (
        |  SELECT *
        |  FROM Deposit
        |  WHERE depositorId = ?
        |  AND bagName = ?
        |  AND (creationTimestamp > ?::timestamp with time zone OR creationTimestamp < ?::timestamp with time zone)
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId
        |ORDER BY depositId DESC;""".stripMargin
    val expectedValue = List(
      "user001",
      "my-bag",
      dt2,
      dt1,
      StateLabel.SUBMITTED.toString,
      "ingest-step",
      IngestStepLabel.FEDORA.toString,
    )

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor,
      |with a certain bagName,
      |with a certain 'latest state',
      |that sometime had this certain ingest step,
      |was last modified before and after two given timestamps
      |and sorted in descending order of depositId""".stripMargin in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      lastModifiedTimeFilter = Some(NotBetween(dt1, dt2)),
      sort = Option(DepositOrder(DepositOrderField.DEPOSIT_ID, OrderDirection.DESC)),
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId
        |INNER JOIN LastModified
        |ON SelectedDeposits.depositId = LastModified.depositId
        |WHERE (max_timestamp > ?::timestamp with time zone OR max_timestamp < ?::timestamp with time zone)
        |ORDER BY depositId DESC;""".stripMargin
    val expectedValue = List(
      "user001",
      "my-bag",
      StateLabel.SUBMITTED.toString,
      "ingest-step",
      IngestStepLabel.FEDORA.toString,
      dt2,
      dt1,
    )

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  it should
    """render a query that searches for deposits of a certain depositor,
      |with a certain bagName,
      |with a certain 'latest state',
      |that sometime had this certain ingest step,
      |was created before and after two given timestamps
      |was last modified before and after two given timestamps
      |and sorted in descending order of depositId""".stripMargin in {
    val dt1 = DateTime.parse("2019-03-03T00:00:00+01:00")
    val dt2 = DateTime.parse("2019-04-04T00:00:00+01:00")
    val dt3 = DateTime.parse("2019-05-05T00:00:00+01:00")
    val dt4 = DateTime.parse("2019-06-06T00:00:00+01:00")
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("my-bag"),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
      creationTimeFilter = Some(NotBetween(dt1, dt2)),
      lastModifiedTimeFilter = Some(NotBetween(dt3, dt4)),
      sort = Option(DepositOrder(DepositOrderField.DEPOSIT_ID, OrderDirection.DESC)),
    )
    val (query, values) = QueryGenerator.searchDeposits(filter)

    val expectedQuery =
      """SELECT *
        |FROM (
        |  SELECT *
        |  FROM Deposit
        |  WHERE depositorId = ?
        |  AND bagName = ?
        |  AND (creationTimestamp > ?::timestamp with time zone OR creationTimestamp < ?::timestamp with time zone)
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId
        |INNER JOIN LastModified
        |ON SelectedDeposits.depositId = LastModified.depositId
        |WHERE (max_timestamp > ?::timestamp with time zone OR max_timestamp < ?::timestamp with time zone)
        |ORDER BY depositId DESC;""".stripMargin
    val expectedValue = List(
      "user001",
      "my-bag",
      dt2,
      dt1,
      StateLabel.SUBMITTED.toString,
      "ingest-step",
      IngestStepLabel.FEDORA.toString,
      dt4,
      dt3,
    )

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) {
      case (value, expectedValue: String) => setStringMock(value, expectedValue)
      case (value, expectedValue: DateTime) => setTimestampMock(value, expectedValue)
      case fallback => fail(s"unexpected values $fallback")
    }
  }

  "searchDepositors" should "render a query that selects all depositors when no filters are set" in {
    val filter = DepositorIdFilters()
    val (query, values) = QueryGenerator.searchDepositors(filter)

    query shouldBe "SELECT DISTINCT depositorId FROM Deposit;"
    values shouldBe empty
  }

  it should "render a query that searches for depositors that have deposited data via a certain origin" in {
    val filter = DepositorIdFilters(originFilter = Some(Origin.SWORD2))
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
        |FROM Deposit
        |WHERE origin = ?;""".stripMargin
    val expectedValues = List(Origin.SWORD2.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for depositors that deposited deposits with a certain 'latest state'" in {
    val filter = DepositorIdFilters(stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)))
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
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

  it should "render a query that searches for depositors that deposited deposits that at some time had this certain state label" in {
    val filter = DepositorIdFilters(stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.ALL)))
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
        |FROM Deposit
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM State
        |  WHERE label = ?
        |) AS StateSearchResult
        |ON Deposit.depositId = StateSearchResult.depositId;""".stripMargin
    val expectedValues = List(StateLabel.SUBMITTED.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for depositors that deposited deposits with a certain origin and with a certain 'latest state'" in {
    val filter = DepositorIdFilters(
      originFilter = Some(Origin.API),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
    )
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
        |FROM (
        |  SELECT depositId, depositorId
        |  FROM Deposit
        |  WHERE origin = ?
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
    val expectedValues = List(Origin.API.toString, StateLabel.SUBMITTED.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValues.size
    forEvery(values zip expectedValues) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for depositors that deposited deposits with a certain 'latest ingest step'" in {
    val filter = DepositorIdFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.LATEST)))
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
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
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List("ingest-step", "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for depositors that deposited deposits that sometime had this certain ingest step" in {
    val filter = DepositorIdFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)))
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
        |FROM Deposit
        |INNER JOIN (
        |  SELECT DISTINCT depositId
        |  FROM SimpleProperties
        |  WHERE key = ?
        |  AND value = ?
        |) AS SimplePropertiesSearchResult
        |ON Deposit.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List("ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for depositors that deposited deposits with a certain 'latest state' and that sometime had this certain ingest step" in {
    val filter = DepositorIdFilters(
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
    )
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
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
    val expectedValue = List(StateLabel.SUBMITTED.toString, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  it should "render a query that searches for depositors that deposited deposits, with a certain origin, with a certain 'latest state' and that sometime had this certain ingest step" in {
    val filter = DepositorIdFilters(
      originFilter = Some(Origin.SMD),
      stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
    )
    val (query, values) = QueryGenerator.searchDepositors(filter)

    val expectedQuery =
      """SELECT DISTINCT depositorId
        |FROM (
        |  SELECT depositId, depositorId
        |  FROM Deposit
        |  WHERE origin = ?
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
        |) AS SimplePropertiesSearchResult
        |ON SelectedDeposits.depositId = SimplePropertiesSearchResult.depositId;""".stripMargin
    val expectedValue = List(Origin.SMD.toString, StateLabel.SUBMITTED.toString, "ingest-step", IngestStepLabel.FEDORA.toString)

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size expectedValue.size
    forEvery(values zip expectedValue) { case (value, expectedValue) =>
      setStringMock(value, expectedValue)
    }
  }

  "getLastModifiedDate" should "generate a UNION query" in {
    val depositIds = NonEmptyList.fromListUnsafe((1 to 5).map(_ => UUID.randomUUID()).toList)
    val (query, values) = QueryGenerator.getLastModifiedDate(depositIds)

    val expectedQuery =
      s"""SELECT *
         |FROM LastModified
         |WHERE depositId IN (?, ?, ?, ?, ?);""".stripMargin

    query should equal(expectedQuery)(after being whiteSpaceNormalised)
    values should have size depositIds.size
    forEvery(values zip depositIds.map(_.toString).toList) { case (value, expectedValue) =>
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
    val expectedValues = ids.toList.flatMap { case (depositId, idType) => List(depositId.toString, idType) }

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
    val expectedValues = ids.toList.flatMap { case (depositId, idType) => List(depositId.toString, idType) }

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

  //TODO replace with storeCurator/storeIsNewVersion/storeIsCurationRequired/storeIsCurationPerformed
//  "storeCuration" should "yield the query for inserting a Curation into the database if isNewVersion is defined" ignore {
//    QueryGenerator.storeCuration(true) shouldBe "INSERT INTO Curation (depositId, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp, isNewVersion) VALUES (?, ?, ?, ?, ?, ?, ?);"
//  }
//
//  it should "yield the query for inserting a Curation into the database if isNewVersion is not defined" ignore {
//    QueryGenerator.storeCuration(false) shouldBe "INSERT INTO Curation (depositId, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp) VALUES (?, ?, ?, ?, ?, ?);"
//  }

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

  "storeCurator" should "yield the query for inserting a Curator into the database" in {
    QueryGenerator.storeCurator shouldBe "INSERT INTO Curator (depositId, datamanagerUserId, datamanagerEmail, timestamp) VALUES (?, ?, ?, ?);"
  }

  "deleteByDepositId" should "yield the query for deleting a State from the database" in {
    QueryGenerator.deleteByDepositId(tableName = "State")(NonEmptyList.of(UUID.randomUUID(), UUID.randomUUID())) shouldBe
      "DELETE FROM State WHERE depositId IN (?, ?);"
  }

  it should "yield the query for deleting a ContentType from the database" in {
    QueryGenerator.deleteByDepositId(tableName = "SimpleProperty", key = "contentType")(NonEmptyList.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())) shouldBe
      "DELETE FROM SimpleProperty WHERE key = 'contentType' AND depositId IN (?, ?, ?);"
  }
}
