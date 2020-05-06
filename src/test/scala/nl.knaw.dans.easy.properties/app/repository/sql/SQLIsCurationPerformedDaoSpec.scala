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
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.{ InputIsCurationPerformed, IsCurationPerformed }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLIsCurationPerformedDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find IsCurationPerformed identified by their id" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getById(Seq("39", "41", "45")).value should contain inOrderOnly(isCurationPerformed0, isCurationPerformed2, isCurationPerformed6)
  }

  it should "return an empty collection if the id is unknown" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val unknownId = "102"

    isCurationPerformeds.getById(Seq(unknownId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid id is given" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current IsCurationPerformed of the given deposits" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getCurrent(Seq(depositId2, depositId3)).value should contain only (
      depositId3 -> isCurationPerformed3,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    isCurationPerformeds.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all IsCurationPerformed associated with the given deposits" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getAll(Seq(depositId2, depositId4)).value should contain only(
      depositId2 -> Seq.empty,
      depositId4 -> Seq(isCurationPerformed4, isCurationPerformed5),
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    isCurationPerformeds.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new IsCurationPerformed into the database" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val timestamp = new DateTime(2019, 7, 19, 22, 45, timeZone)
    val inputIsCurationPerformed = InputIsCurationPerformed(value = true, timestamp)
    val expectedIsCurationPerformed = IsCurationPerformed("47", value = true, timestamp)

    isCurationPerformeds.store(depositId4, inputIsCurationPerformed).value shouldBe expectedIsCurationPerformed
    isCurationPerformeds.getById(Seq("47")).value should contain only expectedIsCurationPerformed
    isCurationPerformeds.getCurrent(Seq(depositId4)).value should contain only (depositId4 -> expectedIsCurationPerformed)
    isCurationPerformeds.getAll(Seq(depositId4)).value.toMap.apply(depositId4) should contain(expectedIsCurationPerformed)
  }

  it should "fail when the given depositId does not exist" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputIsCurationPerformed = InputIsCurationPerformed(value = true, timestamp)

    isCurationPerformeds.store(depositId6, inputIsCurationPerformed).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val inputIsCurationPerformed1 = InputIsCurationPerformed(value = true, timestamp)
    val inputIsCurationPerformed2 = InputIsCurationPerformed(value = false, timestamp)

    isCurationPerformeds.store(depositId, inputIsCurationPerformed1) shouldBe right
    isCurationPerformeds.store(depositId, inputIsCurationPerformed2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, objName = "is curation performed")
  }

  "getDepositsById" should "find deposits identified by these IsCurationPerformedIds" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getDepositsById(Seq("39", "41", "43")).value should contain only(
      "39" -> deposit1,
      "41" -> deposit3,
      "43" -> deposit4,
    )
  }

  it should "return an empty collection if the IsCurationPerformedIds is unknown" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao
    val unknownIsCurationPerformedId = "102"

    isCurationPerformeds.getDepositsById(Seq(unknownIsCurationPerformedId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid isCurationPerformedId is given" in {
    val isCurationPerformeds = new SQLIsCurationPerformedDao

    isCurationPerformeds.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
