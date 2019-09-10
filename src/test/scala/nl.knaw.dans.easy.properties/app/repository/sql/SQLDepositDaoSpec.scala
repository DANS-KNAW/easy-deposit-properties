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

import java.util.UUID

import cats.scalatest.EitherValues
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ DepositIngestStepFilter, IngestStepLabel }
import nl.knaw.dans.easy.properties.app.model.state.{ DepositStateFilter, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.{ BagNameAlreadySetError, DepositAlreadyExistsError, DepositFilters, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLDepositDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues {

  "getAll" should "return all deposits that are in the database" in {
    val deposits = new SQLDepositDao

    deposits.getAll.value should contain inOrderOnly(
      deposit1,
      deposit2,
      deposit3,
      deposit4,
      deposit5,
    )
  }

  it should "fail when the depositId column doesn't contain a UUID" in {
    prepareTest {
      """INSERT INTO Deposit
        |VALUES ('abcdefgh-ijkl-mnop-qrst-uvwxyzabcdef', 'bag1', '2019-01-01 00:00:00.000000+1:00', 'user001');""".stripMargin
    }

    val deposits = new SQLDepositDao
    deposits.getAll.leftValue shouldBe InvalidValueError("Invalid depositId value: 'abcdefgh-ijkl-mnop-qrst-uvwxyzabcdef'")
  }

  "find" should "return the requested deposits" in {
    val deposits = new SQLDepositDao

    deposits.find(Seq(depositId1, depositId4)).value should contain inOrderOnly(
      deposit1,
      deposit4,
    )
  }

  it should "return a None if one of the depositIds is unknown" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    deposits.find(Seq(depositId1, depositId6)).value should contain only deposit1
  }

  it should "return a None if the depositId is unknown" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    deposits.find(Seq(depositId6)).value shouldBe empty
  }
  
  it should "return an empty collection when the input collection is empty" in {
    val deposits = new SQLDepositDao
    
    deposits.find(Seq.empty).value shouldBe empty
  }

  "search" should "find all deposits when no filters are given" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters()

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1, deposit2, deposit3, deposit4, deposit5))
  }

  it should "find deposits from the given user" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(depositorId = Some("user001"))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1, deposit2, deposit4))
  }

  it should "find deposits with the given bag name null" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(bagName = Some(null))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit2))
  }

  it should "find deposits with the given bag name" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(bagName = Some("bag1"))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1))
  }

  it should "find deposits with the given depositorId and bag name" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(depositorId = Some("user001"), bagName = Some("bag1"))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1))
  }

  it should "find deposits with a certain 'latest state'" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1, deposit2, deposit4))
  }

  it should "find deposits that at some time had this certain state label" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.ALL)))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1, deposit2, deposit5))
  }

  it should "find deposits of a certain depositor, with a certain bagName and with a certain 'latest state'" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(depositorId = Some("user001"), bagName = Some("bag1"), stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.ALL)))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1))
  }

  it should "find deposits with a certain 'latest ingest step'" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.LATEST)))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit5))
  }

  it should "find deposits that sometime had this certain ingest step" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.VALIDATE, SeriesFilter.ALL)))

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1, deposit2, deposit5))
  }

  it should "find deposits with a certain 'latest state' and that sometime had this certain ingest step" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(
      stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
    )

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1, deposit2))
  }

  it should "find deposits of a certain depositor, with a certain bagName, with a certain 'latest state' and that sometime had this certain ingest step" in {
    val deposits = new SQLDepositDao
    val filter = DepositFilters(
      depositorId = Some("user001"),
      bagName = Some("bag1"),
      stateFilter = Some(DepositStateFilter(StateLabel.ARCHIVED, SeriesFilter.LATEST)),
      ingestStepFilter = Some(DepositIngestStepFilter(IngestStepLabel.FEDORA, SeriesFilter.ALL)),
    )

    deposits.search(Seq(filter)).value should contain only (filter -> Seq(deposit1))
  }

  it should "be able to do multiple searches in one call" in {
    val deposits = new SQLDepositDao
    val filter1 = DepositFilters(depositorId = Some("user001"))
    val filter2 = DepositFilters(depositorId = Some("user001"), bagName = Some("bag1"), stateFilter = Some(DepositStateFilter(StateLabel.SUBMITTED, SeriesFilter.ALL)))

    deposits.search(Seq(filter1, filter2)).value should contain only(
      filter1 -> Seq(deposit1, deposit2, deposit4),
      filter2 -> Seq(deposit1)
    )
  }

  "store" should "insert a deposit into the database" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val deposit6 = Deposit(depositId6, Option("bag1"), new DateTime(2019, 6, 6, 0, 0, timeZone), "user003")

    deposits.store(deposit6).value shouldBe deposit6
    deposits.find(Seq(depositId6)).value should contain only deposit6
  }

  it should "fail to insert a deposit when the depositId is already present" in {
    val deposits = new SQLDepositDao

    deposits.find(Seq(depositId1)).value should contain only deposit1
    deposits.store(deposit1).leftValue shouldBe DepositAlreadyExistsError(depositId1)
  }

  it should "correctly store and retrieve a 'creationTimestamp' from a different timezone" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val deposit6 = Deposit(depositId6, Option("bag6"), DateTime.parse("2019-06-06T00:00:00.000+12:00"), "user003")
    val expectedCreationTimestamp = new DateTime(2019, 6, 5, 12, 0, timeZone)

    deposits.store(deposit6).value shouldBe deposit6
    deposits.find(Seq(depositId6)).value should contain only deposit6.copy(creationTimestamp = expectedCreationTimestamp)
  }

  it should "correctly store and retrieve a deposit without bagName" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val deposit6 = Deposit(depositId6, None, new DateTime(2019, 6, 6, 0, 0, timeZone), "user003")

    deposits.store(deposit6).value shouldBe deposit6
    deposits.find(Seq(depositId6)).value should contain only deposit6
  }

  "storeBagName" should "set the bagName if it wasn't already set" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val deposit6 = Deposit(depositId6, None, new DateTime(2019, 6, 6, 0, 0, timeZone), "user003")

    deposits.store(deposit6).value shouldBe deposit6
    deposits.find(Seq(depositId6)).value should contain only deposit6
    deposits.storeBagName(depositId6, "bag6").value shouldBe depositId6
    deposits.find(Seq(depositId6)).value should contain only deposit6.copy(bagName = Some("bag6"))
  }

  it should "set the bagName if its value in the database currently is an empty string" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val deposit6 = Deposit(depositId6, Some(""), new DateTime(2019, 6, 6, 0, 0, timeZone), "user003")

    deposits.store(deposit6).value shouldBe deposit6
    deposits.find(Seq(depositId6)).value should contain only deposit6
    deposits.storeBagName(depositId6, "bag6").value shouldBe depositId6
    deposits.find(Seq(depositId6)).value should contain only deposit6.copy(bagName = Some("bag6"))
  }

  it should "fail to set the bagName when the deposit doesn't exist" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    deposits.storeBagName(depositId6, "bag6").leftValue shouldBe NoSuchDepositError(depositId6)
  }
  
  it should "fail to set the bagName when the deposit exists and the bagName is already set" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val deposit6 = Deposit(depositId6, Some("bag6"), new DateTime(2019, 6, 6, 0, 0, timeZone), "user003")

    deposits.store(deposit6).value shouldBe deposit6
    deposits.find(Seq(depositId6)).value should contain only deposit6
    deposits.storeBagName(depositId6, "another name").leftValue shouldBe BagNameAlreadySetError(depositId6)
    deposits.find(Seq(depositId6)).value should contain only deposit6
  }
  
  "lastModified" should "find the 'last modified' timestamp for each of the given depositIds" in {
    val deposits = new SQLDepositDao

    deposits.lastModified(Seq(depositId1, depositId5)).value should contain only(
      depositId1 -> new DateTime(2019, 1, 1, 5, 5, timeZone),
      depositId5 -> new DateTime(2019, 5, 5, 4, 5, timeZone),
    )
  }

  it should "return an empty collection when no depositIds are given" in {
    val deposits = new SQLDepositDao

    deposits.lastModified(Seq.empty).value shouldBe empty
  }

  it should "return an empty collection for an unknown depositId" in {
    val deposits = new SQLDepositDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    deposits.lastModified(Seq(depositId6)).value shouldBe empty
  }
}
