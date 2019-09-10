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
import nl.knaw.dans.easy.properties.app.model.DoiRegisteredEvent
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLDoiRegisteredDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getCurrent" should "return the current doi registered event of the given deposits" in {
    val doiRegistereds = new SQLDoiRegisteredDao

    doiRegistereds.getCurrent(Seq(depositId4, depositId5)).value should contain only(depositId5 -> doiRegistered4)
  }

  it should "return an empty collection if the depositId is unknown" in {
    val doiRegistereds = new SQLDoiRegisteredDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    doiRegistereds.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val doiRegistereds = new SQLDoiRegisteredDao

    doiRegistereds.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all doi registered events associated with the given deposits" in {
    val doiRegistereds = new SQLDoiRegisteredDao

    doiRegistereds.getAll(Seq(depositId2, depositId4)).value should contain only(
      depositId2 -> Seq(doiRegistered2, doiRegistered3),
      depositId4 -> Seq.empty,
    )
  }

  it should "return a None if the depositId is unknown" in {
    val doiRegistereds = new SQLDoiRegisteredDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    doiRegistereds.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val doiRegistereds = new SQLDoiRegisteredDao

    doiRegistereds.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new doi registered event into the database" in {
    val doiRegistereds = new SQLDoiRegisteredDao
    val timestamp = new DateTime(2019, 7, 19, 22, 45, timeZone)
    val doiRegisteredEvent = DoiRegisteredEvent(value = true, timestamp)

    doiRegistereds.store(depositId5, doiRegisteredEvent).value shouldBe doiRegisteredEvent
    doiRegistereds.getCurrent(Seq(depositId5)).value should contain only (depositId5 -> doiRegisteredEvent)
    doiRegistereds.getAll(Seq(depositId5)).value.toMap.apply(depositId5) should contain only(doiRegistered4, doiRegisteredEvent)
  }

  it should "fail when the given depositId does not exist" in {
    val doiRegistereds = new SQLDoiRegisteredDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val doiRegisteredEvent = DoiRegisteredEvent(value = true, timestamp)

    doiRegistereds.store(depositId6, doiRegisteredEvent).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val doiRegistereds = new SQLDoiRegisteredDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val doiRegisteredEvent1 = DoiRegisteredEvent(value = true, timestamp)
    val doiRegisteredEvent2 = DoiRegisteredEvent(value = false, timestamp)

    doiRegistereds.store(depositId, doiRegisteredEvent1) shouldBe right
    doiRegistereds.store(depositId, doiRegisteredEvent2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, "doi registered event")
  }
}
