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
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.{ InputIsCurationRequired, IsCurationRequired }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLIsCurationRequiredDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find IsCurationRequired identified by their id" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getById(Seq("35", "37", "38")).value should contain inOrderOnly(isCurationRequired0, isCurationRequired2, isCurationRequired3)
  }

  it should "return an empty collection if the id is unknown" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val unknownId = "102"

    isCurationRequireds.getById(Seq(unknownId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid id is given" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current IsCurationRequired of the given deposits" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getCurrent(Seq(depositId2, depositId3)).value should contain only (
      depositId3 -> isCurationRequired1,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    isCurationRequireds.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all IsCurationRequired associated with the given deposits" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getAll(Seq(depositId2, depositId4)).value should contain only(
      depositId2 -> Seq.empty,
      depositId4 -> Seq(isCurationRequired2),
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    isCurationRequireds.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new IsCurationRequired into the database" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val timestamp = new DateTime(2019, 7, 19, 22, 45, timeZone)
    val inputIsCurationRequired = InputIsCurationRequired(value = true, timestamp)
    val expectedIsCurationRequired = IsCurationRequired("47", value = true, timestamp)

    isCurationRequireds.store(depositId4, inputIsCurationRequired).value shouldBe expectedIsCurationRequired
    isCurationRequireds.getById(Seq("47")).value should contain only expectedIsCurationRequired
    isCurationRequireds.getCurrent(Seq(depositId4)).value should contain only (depositId4 -> expectedIsCurationRequired)
    isCurationRequireds.getAll(Seq(depositId4)).value.toMap.apply(depositId4) should contain(expectedIsCurationRequired)
  }

  it should "fail when the given depositId does not exist" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputIsCurationRequired = InputIsCurationRequired(value = true, timestamp)

    isCurationRequireds.store(depositId6, inputIsCurationRequired).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val inputIsCurationRequired1 = InputIsCurationRequired(value = true, timestamp)
    val inputIsCurationRequired2 = InputIsCurationRequired(value = false, timestamp)

    isCurationRequireds.store(depositId, inputIsCurationRequired1) shouldBe right
    isCurationRequireds.store(depositId, inputIsCurationRequired2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, objName = "is curation required")
  }

  "getDepositsById" should "find deposits identified by these IsCurationRequiredIds" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getDepositsById(Seq("35", "36", "37")).value should contain only(
      "35" -> deposit1,
      "36" -> deposit3,
      "37" -> deposit4,
    )
  }

  it should "return an empty collection if the IsCurationRequiredIds is unknown" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao
    val unknownIsCurationRequiredId = "102"

    isCurationRequireds.getDepositsById(Seq(unknownIsCurationRequiredId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid isCurationRequiredId is given" in {
    val isCurationRequireds = new SQLIsCurationRequiredDao

    isCurationRequireds.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
