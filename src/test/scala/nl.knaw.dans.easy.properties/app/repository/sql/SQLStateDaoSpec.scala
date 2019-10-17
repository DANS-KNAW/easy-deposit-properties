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
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, State, StateLabel }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLStateDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find states identified by their stateId" in {
    val states = new SQLStateDao

    states.getById(Seq("7", "8", "15")).value should contain inOrderOnly(state21, state22, state42)
  }

  it should "return an empty collection if the stateId is unknown" in {
    val states = new SQLStateDao
    val unknownStateId = "102"

    states.getById(Seq(unknownStateId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val states = new SQLStateDao

    states.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid stateId is given" in {
    val states = new SQLStateDao

    states.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current states of the given deposits" in {
    val states = new SQLStateDao

    states.getCurrent(Seq(depositId2, depositId5)).value should contain inOrderOnly(
      depositId2 -> state23,
      depositId5 -> state53,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val states = new SQLStateDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    states.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val states = new SQLStateDao

    states.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all states associated with the given deposits" in {
    val states = new SQLStateDao

    states.getAll(Seq(depositId2, depositId5)).value should contain only(
      depositId2 -> Seq(state20, state21, state22, state23),
      depositId5 -> Seq(state50, state51, state52, state53),
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val states = new SQLStateDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    states.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val states = new SQLStateDao

    states.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new state into the database" in {
    val states = new SQLStateDao
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputState = InputState(StateLabel.FEDORA_ARCHIVED, "blablabla", timestamp)
    val expectedState = State("20", StateLabel.FEDORA_ARCHIVED, "blablabla", timestamp)

    states.store(depositId1, inputState).value shouldBe expectedState
    states.getById(Seq("20")).value should contain only expectedState
    states.getCurrent(Seq(depositId1)).value should contain only (depositId1 -> expectedState)
    states.getAll(Seq(depositId1)).value.toMap.apply(depositId1) should contain(expectedState)
  }

  it should "fail when the given depositId does not exist" in {
    val states = new SQLStateDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputState = InputState(StateLabel.FEDORA_ARCHIVED, "blablabla", timestamp)

    states.store(depositId6, inputState).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the label and description are different" in {
    val states = new SQLStateDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val inputState1 = InputState(StateLabel.DRAFT, "deposit is in draft", timestamp)
    val inputState2 = InputState(StateLabel.FEDORA_ARCHIVED, "deposit is archived in Fedora", timestamp)

    states.store(depositId, inputState1) shouldBe right
    states.store(depositId, inputState2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, "State")
  }

  "getDepositsById" should "find deposits identified by these stateIds" in {
    val states = new SQLStateDao

    states.getDepositsById(Seq("7", "8", "15")).value should contain only(
      "7" -> deposit2,
      "8" -> deposit2,
      "15" -> deposit4,
    )
  }

  it should "return an empty collection if the stateId is unknown" in {
    val states = new SQLStateDao
    val unknownStateId = "102"

    states.getDepositsById(Seq(unknownStateId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val states = new SQLStateDao

    states.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid stateId is given" in {
    val states = new SQLStateDao

    states.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
