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

import cats.scalatest.{ EitherMatchers, EitherValues }
import nl.knaw.dans.easy.properties.app.model.curator.{ Curator, InputCurator }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLCuratorDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find curator configurations identified by their curatorEntryId" in {
    val curators = new SQLCuratorDao

    curators.getById(Seq("0", "2", "5")).value should contain inOrderOnly(curator0, curator2, curator5)
  }

  it should "return an empty collection if the curatorEntryId is unknown" in {
    val curators = new SQLCuratorDao
    val unknowncuratorEntryId = "102"

    curators.getById(Seq(unknowncuratorEntryId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val curators = new SQLCuratorDao

    curators.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid curatorEntryId is given" in {
    val curators = new SQLCuratorDao

    curators.getById(Seq("2", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current curator configurations of the given deposits" in {
    val curators = new SQLCuratorDao

    curators.getCurrent(Seq(depositId1, depositId2)).value should contain only (depositId1 -> curator1)
  }

  it should "return an empty collection if the depositId is unknown" in {
    val curators = new SQLCuratorDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    curators.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val curators = new SQLCuratorDao

    curators.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all curator configurations associated with the given deposits" in {
    val curators = new SQLCuratorDao

    curators.getAll(Seq(depositId1, depositId2)).value should contain only(
      depositId1 -> Seq(curator0, curator1),
      depositId2 -> Seq.empty,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val curators = new SQLCuratorDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    curators.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val curators = new SQLCuratorDao

    curators.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new curator into the database" in {
    val curators = new SQLCuratorDao
    val timestamp = new DateTime(2019, 7, 20, 21, 12, timeZone)
    val inputCurator = InputCurator("my-username", "foo@bar.com", timestamp)
    val expectedCurator = Curator("6", "my-username", "foo@bar.com", timestamp)

    curators.store(depositId1, inputCurator).value shouldBe expectedCurator
    curators.getById(Seq("6")).value should contain only expectedCurator
    curators.getCurrent(Seq(depositId1)).value should contain only (depositId1 -> expectedCurator)
    curators.getAll(Seq(depositId1)).value.toMap.apply(depositId1) should contain(expectedCurator)
  }

  it should "fail when the given depositId does not exist" in {
    val curators = new SQLCuratorDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputCurator = InputCurator("my-username", "foo@bar.com", timestamp)

    curators.store(depositId6, inputCurator).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val curators = new SQLCuratorDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 5, 5, timeZone)
    val inputCurator1 = InputCurator("my-username", "foo@bar.com", timestamp)
    val inputCurator2 = InputCurator("foo", "foo@bar.com", timestamp)

    curators.store(depositId, inputCurator1) shouldBe right
    curators.store(depositId, inputCurator2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, objName = "curator")
  }

  "getDepositsById" should "find deposits identified by these curatorEntryIds" in {
    val curators = new SQLCuratorDao

    val result = curators.getDepositsById(Seq("0", "1", "2", "3", "4", "5", "6")).value
    result should contain only(
      "0" -> deposit1,
      "1" -> deposit1,
      "2" -> deposit3,
      "3" -> deposit3,
      "4" -> deposit4,
      "5" -> deposit5,
    )
    result.map(_._1) should not contain "6"
  }

  it should "return an empty collection if the curatorEntryId is unknown" in {
    val curators = new SQLCuratorDao
    val unknowncuratorEntryId = "102"

    curators.getDepositsById(Seq(unknowncuratorEntryId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val curators = new SQLCuratorDao

    curators.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid curatorEntryId is given" in {
    val curators = new SQLCuratorDao

    curators.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
