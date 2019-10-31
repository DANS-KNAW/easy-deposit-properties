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
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLIngestStepDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find ingest steps identified by their id" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getById(Seq("4", "10", "14")).value should contain inOrderOnly(step4, step10, step14)
  }

  it should "return an empty collection if the id is unknown" in {
    val ingestSteps = new SQLIngestStepDao
    val unknownId = "102"

    ingestSteps.getById(Seq(unknownId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid id is given" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current ingest step of the given deposits" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getCurrent(Seq(depositId4, depositId5)).value should contain only(
      depositId5 -> step14,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val ingestSteps = new SQLIngestStepDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    ingestSteps.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all ingest steps associated with the given deposits" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getAll(Seq(depositId2, depositId4)).value should contain only(
      depositId2 -> Seq(step7, step8, step9, step10, step11, step12, step13),
      depositId4 -> Seq.empty,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val ingestSteps = new SQLIngestStepDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    ingestSteps.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new ingest step into the database" in {
    val ingestSteps = new SQLIngestStepDao
    val timestamp = new DateTime(2019, 7, 19, 22, 45, timeZone)
    val inputIngestStep = InputIngestStep(IngestStepLabel.BAGSTORE, timestamp)
    val expectedIngestStep = IngestStep("32", IngestStepLabel.BAGSTORE, timestamp)

    ingestSteps.store(depositId4, inputIngestStep).value shouldBe expectedIngestStep
    ingestSteps.getById(Seq("32")).value should contain only expectedIngestStep
    ingestSteps.getCurrent(Seq(depositId4)).value should contain only (depositId4 -> expectedIngestStep)
    ingestSteps.getAll(Seq(depositId4)).value.toMap.apply(depositId4) should contain(expectedIngestStep)
  }

  it should "fail when the given depositId does not exist" in {
    val ingestSteps = new SQLIngestStepDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputIngestStep = InputIngestStep(IngestStepLabel.BAGSTORE, timestamp)

    ingestSteps.store(depositId6, inputIngestStep).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val ingestSteps = new SQLIngestStepDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val inputIngestStep1 = InputIngestStep(IngestStepLabel.BAGSTORE, timestamp)
    val inputIngestStep2 = InputIngestStep(IngestStepLabel.SOLR4FILES, timestamp)

    ingestSteps.store(depositId, inputIngestStep1) shouldBe right
    ingestSteps.store(depositId, inputIngestStep2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, objName = "ingest step")
  }

  "getDepositsById" should "find deposits identified by these ingestStepIds" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getDepositsById(Seq("5", "11", "13")).value should contain only(
      "5" -> deposit1,
      "11" -> deposit2,
      "13" -> deposit2,
    )
  }

  it should "return an empty collection if the ingestStepId is unknown" in {
    val ingestSteps = new SQLIngestStepDao
    val unknownIngestStepId = "102"

    ingestSteps.getDepositsById(Seq(unknownIngestStepId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid ingestStepId is given" in {
    val ingestSteps = new SQLIngestStepDao

    ingestSteps.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
