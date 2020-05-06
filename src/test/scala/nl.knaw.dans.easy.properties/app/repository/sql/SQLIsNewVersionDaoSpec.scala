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
import nl.knaw.dans.easy.properties.app.model.isnewversion.{ InputIsNewVersion, IsNewVersion }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLIsNewVersionDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find IsNewVersion identified by their id" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getById(Seq("32", "33", "34")).value should contain inOrderOnly(isNewVersion0, isNewVersion1, isNewVersion2)
  }

  it should "return an empty collection if the id is unknown" in {
    val isNewVersions = new SQLIsNewVersionDao
    val unknownId = "102"

    isNewVersions.getById(Seq(unknownId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid id is given" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current IsNewVersion of the given deposits" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getCurrent(Seq(depositId1, depositId3)).value should contain only (
      depositId3 -> isNewVersion0,
      )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val isNewVersions = new SQLIsNewVersionDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    isNewVersions.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all IsNewVersion associated with the given deposits" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getAll(Seq(depositId2, depositId4)).value should contain only(
      depositId2 -> Seq.empty,
      depositId4 -> Seq(isNewVersion1),
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val isNewVersions = new SQLIsNewVersionDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    isNewVersions.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new IsNewVersion into the database" in {
    val isNewVersions = new SQLIsNewVersionDao
    val timestamp = new DateTime(2019, 7, 19, 22, 45, timeZone)
    val inputIsNewVersion = InputIsNewVersion(value = true, timestamp)
    val expectedIsNewVersion = IsNewVersion("47", value = true, timestamp)

    isNewVersions.store(depositId4, inputIsNewVersion).value shouldBe expectedIsNewVersion
    isNewVersions.getById(Seq("47")).value should contain only expectedIsNewVersion
    isNewVersions.getCurrent(Seq(depositId4)).value should contain only (depositId4 -> expectedIsNewVersion)
    isNewVersions.getAll(Seq(depositId4)).value.toMap.apply(depositId4) should contain(expectedIsNewVersion)
  }

  it should "fail when the given depositId does not exist" in {
    val isNewVersions = new SQLIsNewVersionDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputIsNewVersion = InputIsNewVersion(value = true, timestamp)

    isNewVersions.store(depositId6, inputIsNewVersion).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val isNewVersions = new SQLIsNewVersionDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val inputIsNewVersion1 = InputIsNewVersion(value = true, timestamp)
    val inputIsNewVersion2 = InputIsNewVersion(value = false, timestamp)

    isNewVersions.store(depositId, inputIsNewVersion1) shouldBe right
    isNewVersions.store(depositId, inputIsNewVersion2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, objName = "is new version")
  }

  "getDepositsById" should "find deposits identified by these IsNewVersionIds" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getDepositsById(Seq("32", "33", "34")).value should contain only(
      "32" -> deposit3,
      "33" -> deposit4,
      "34" -> deposit5,
    )
  }

  it should "return an empty collection if the IsNewVersionIds is unknown" in {
    val isNewVersions = new SQLIsNewVersionDao
    val unknownIsNewVersionId = "102"

    isNewVersions.getDepositsById(Seq(unknownIsNewVersionId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid isNewVersionId is given" in {
    val isNewVersions = new SQLIsNewVersionDao

    isNewVersions.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
