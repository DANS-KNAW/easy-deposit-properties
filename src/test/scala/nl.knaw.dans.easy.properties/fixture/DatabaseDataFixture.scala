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
package nl.knaw.dans.easy.properties.fixture

import java.util.UUID

import better.files.File
import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentType, ContentTypeValue }
import nl.knaw.dans.easy.properties.app.model.curation.Curation
import nl.knaw.dans.easy.properties.app.model.curator.Curator
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel }
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.IsCurationPerformed
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.IsCurationRequired
import nl.knaw.dans.easy.properties.app.model.isnewversion.IsNewVersion
import nl.knaw.dans.easy.properties.app.model.springfield.{ Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ State, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DoiAction, DoiActionEvent, DoiRegisteredEvent, Origin }
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatest.{ BeforeAndAfterEach, Suite }
import resource.managed

trait DatabaseDataFixture extends BeforeAndAfterEach {
  this: DatabaseFixture with Suite =>

  val timeZone: DateTimeZone = DateTimeZone.UTC

  val depositId1: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val depositId2: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val depositId3: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000003")
  val depositId4: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000004")
  val depositId5: DepositId = UUID.fromString("00000000-0000-0000-0000-000000000005")

  val deposit1 = Deposit(depositId1, Option("bag1"), new DateTime(2019, 1, 1, 0, 0, timeZone), "user001", Origin.API)
  val deposit2 = Deposit(depositId2, Option.empty, new DateTime(2019, 2, 2, 0, 0, timeZone), "user001", Origin.API)
  val deposit3 = Deposit(depositId3, Option("bag3"), new DateTime(2019, 3, 3, 0, 0, timeZone), "user002", Origin.SWORD2)
  val deposit4 = Deposit(depositId4, Option("bag4"), new DateTime(2019, 4, 4, 0, 0, timeZone), "user001", Origin.SMD)
  val deposit5 = Deposit(depositId5, Option("bag5"), new DateTime(2019, 5, 5, 0, 0, timeZone), "user002", Origin.SWORD2)

  val state10 = State("0", StateLabel.DRAFT, "draft with continued deposit", new DateTime(2019, 1, 1, 0, 0, timeZone))
  val state11 = State("1", StateLabel.DRAFT, "draft with continued deposit", new DateTime(2019, 1, 1, 1, 1, timeZone))
  val state12 = State("2", StateLabel.UPLOADED, "deposit upload has been completed", new DateTime(2019, 1, 1, 2, 2, timeZone))
  val state13 = State("3", StateLabel.FINALIZING, "deposit is finalizing", new DateTime(2019, 1, 1, 3, 3, timeZone))
  val state14 = State("4", StateLabel.SUBMITTED, "deposit is processing", new DateTime(2019, 1, 1, 4, 4, timeZone))
  val state15 = State("5", StateLabel.ARCHIVED, "deposit is archived", new DateTime(2019, 1, 1, 5, 5, timeZone))

  val state20 = State("6", StateLabel.UPLOADED, "deposit upload has been completed", new DateTime(2019, 2, 2, 0, 0, timeZone))
  val state21 = State("7", StateLabel.FINALIZING, "deposit is finalizing", new DateTime(2019, 2, 2, 1, 1, timeZone))
  val state22 = State("8", StateLabel.SUBMITTED, "deposit is processing", new DateTime(2019, 2, 2, 2, 2, timeZone))
  val state23 = State("9", StateLabel.ARCHIVED, "deposit is archived", new DateTime(2019, 2, 2, 3, 3, timeZone))

  val state30 = State("10", StateLabel.UPLOADED, "deposit upload has been completed", new DateTime(2019, 3, 3, 0, 0, timeZone))
  val state31 = State("11", StateLabel.FINALIZING, "deposit is finalizing", new DateTime(2019, 3, 3, 1, 1, timeZone))
  val state32 = State("12", StateLabel.INVALID, "deposit is invalid", new DateTime(2019, 3, 3, 2, 2, timeZone))

  val state40 = State("13", StateLabel.UPLOADED, "deposit upload has been completed", new DateTime(2019, 4, 4, 0, 0, timeZone))
  val state41 = State("14", StateLabel.FINALIZING, "deposit is finalizing", new DateTime(2019, 4, 4, 1, 1, timeZone))
  val state42 = State("15", StateLabel.ARCHIVED, "deposit is archived", new DateTime(2019, 4, 4, 2, 2, timeZone))

  val state50 = State("16", StateLabel.UPLOADED, "deposit upload has been completed", new DateTime(2019, 5, 5, 0, 0, timeZone))
  val state51 = State("17", StateLabel.FINALIZING, "deposit is finalizing", new DateTime(2019, 5, 5, 1, 1, timeZone))
  val state52 = State("18", StateLabel.SUBMITTED, "deposit is processing", new DateTime(2019, 5, 5, 2, 2, timeZone))
  val state53 = State("19", StateLabel.REJECTED, "deposit is rejected", new DateTime(2019, 5, 5, 3, 3, timeZone))

  val identifier0 = Identifier("0", IdentifierType.BAG_STORE, depositId1.toString, new DateTime(2019, 1, 1, 0, 0, timeZone))
  val identifier1 = Identifier("1", IdentifierType.DOI, "10.5072/dans-a1b-cde2", new DateTime(2019, 1, 1, 0, 1, timeZone))
  val identifier2 = Identifier("2", IdentifierType.URN, "urn:nbn:123456", new DateTime(2019, 1, 1, 0, 2, timeZone))
  val identifier3 = Identifier("3", IdentifierType.FEDORA, "easy-dataset:1", new DateTime(2019, 1, 1, 0, 3, timeZone))

  val identifier4 = Identifier("4", IdentifierType.BAG_STORE, depositId2.toString, new DateTime(2019, 2, 2, 0, 0, timeZone))
  val identifier5 = Identifier("5", IdentifierType.DOI, "10.5072/dans-f3g-hij4", new DateTime(2019, 2, 2, 0, 1, timeZone))
  val identifier6 = Identifier("6", IdentifierType.URN, "urn:nbn:789012", new DateTime(2019, 2, 2, 0, 2, timeZone))
  val identifier7 = Identifier("7", IdentifierType.FEDORA, "easy-dataset:2", new DateTime(2019, 2, 2, 0, 3, timeZone))

  val identifier8 = Identifier("8", IdentifierType.BAG_STORE, depositId3.toString, new DateTime(2019, 3, 3, 0, 0, timeZone))

  val identifier9 = Identifier("9", IdentifierType.BAG_STORE, depositId4.toString, new DateTime(2019, 4, 4, 0, 0, timeZone))
  val identifier10 = Identifier("10", IdentifierType.DOI, "10.5072/dans-p7q-rst8", new DateTime(2019, 4, 4, 0, 1, timeZone))
  val identifier11 = Identifier("11", IdentifierType.URN, "urn:nbn:901234", new DateTime(2019, 4, 4, 0, 2, timeZone))
  val identifier12 = Identifier("12", IdentifierType.FEDORA, "easy-dataset:4", new DateTime(2019, 4, 4, 0, 3, timeZone))

  val identifier13 = Identifier("13", IdentifierType.BAG_STORE, depositId5.toString, new DateTime(2019, 5, 5, 0, 0, timeZone))

  @deprecated val curation0 = Curation("0", isNewVersion = none, isRequired = true, isPerformed = false, "archie002", "does.not.exists2@dans.knaw.nl", new DateTime(2019, 1, 1, 0, 0, timeZone))
  @deprecated val curation1 = Curation("1", isNewVersion = none, isRequired = true, isPerformed = false, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 1, 1, 3, 3, timeZone))
  @deprecated val curation2 = Curation("2", isNewVersion = none, isRequired = true, isPerformed = true, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 1, 1, 4, 4, timeZone))

  @deprecated val curation3 = Curation("3", isNewVersion = none, isRequired = true, isPerformed = false, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 3, 3, 0, 0, timeZone))
  @deprecated val curation4 = Curation("4", isNewVersion = none, isRequired = true, isPerformed = false, "archie002", "does.not.exists2@dans.knaw.nl", new DateTime(2019, 3, 3, 4, 4, timeZone))
  @deprecated val curation5 = Curation("5", isNewVersion = true.some, isRequired = true, isPerformed = true, "archie002", "does.not.exists2@dans.knaw.nl", new DateTime(2019, 3, 3, 6, 6, timeZone))

  @deprecated val curation6 = Curation("6", isNewVersion = false.some, isRequired = true, isPerformed = false, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 4, 4, 0, 0, timeZone))
  @deprecated val curation7 = Curation("7", isNewVersion = false.some, isRequired = true, isPerformed = true, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 4, 4, 4, 4, timeZone))

  @deprecated val curation8 = Curation("8", isNewVersion = false.some, isRequired = true, isPerformed = false, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 5, 5, 0, 0, timeZone))
  @deprecated val curation9 = Curation("9", isNewVersion = false.some, isRequired = true, isPerformed = true, "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 5, 5, 4, 4, timeZone))

  val curator0 = Curator("0", "archie002", "does.not.exists2@dans.knaw.nl", new DateTime(2019, 1, 1, 0, 0, timeZone))
  val curator1 = Curator("1", "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 1, 1, 3, 3, timeZone))

  val curator2 = Curator("2", "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 3, 3, 0, 0, timeZone))
  val curator3 = Curator("3", "archie002", "does.not.exists2@dans.knaw.nl", new DateTime(2019, 3, 3, 4, 4, timeZone))

  val curator4 = Curator("4", "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 4, 4, 0, 0, timeZone))

  val curator5 = Curator("5", "archie001", "does.not.exists1@dans.knaw.nl", new DateTime(2019, 5, 5, 0, 0, timeZone))

  val springfield0 = Springfield("0", "domain1", "user1", "collection1", SpringfieldPlayMode.CONTINUOUS, new DateTime(2019, 1, 1, 0, 0, timeZone))
  val springfield1 = Springfield("1", "domain1", "user1", "collection1", SpringfieldPlayMode.CONTINUOUS, new DateTime(2019, 2, 2, 0, 0, timeZone))
  val springfield2 = Springfield("2", "domain2", "user2", "collection2", SpringfieldPlayMode.MENU, new DateTime(2019, 2, 2, 2, 2, timeZone))

  val step0 = IngestStep("0", IngestStepLabel.VALIDATE, new DateTime(2019, 1, 1, 4, 5, timeZone))
  val step1 = IngestStep("1", IngestStepLabel.PID_GENERATOR, new DateTime(2019, 1, 1, 4, 6, timeZone))
  val step2 = IngestStep("2", IngestStepLabel.FEDORA, new DateTime(2019, 1, 1, 4, 7, timeZone))
  val step3 = IngestStep("3", IngestStepLabel.SPRINGFIELD, new DateTime(2019, 1, 1, 4, 8, timeZone))
  val step4 = IngestStep("4", IngestStepLabel.BAGSTORE, new DateTime(2019, 1, 1, 4, 9, timeZone))
  val step5 = IngestStep("5", IngestStepLabel.SOLR4FILES, new DateTime(2019, 1, 1, 4, 10, timeZone))
  val step6 = IngestStep("6", IngestStepLabel.COMPLETED, new DateTime(2019, 1, 1, 4, 11, timeZone))

  val step7 = IngestStep("7", IngestStepLabel.VALIDATE, new DateTime(2019, 2, 2, 2, 5, timeZone))
  val step8 = IngestStep("8", IngestStepLabel.PID_GENERATOR, new DateTime(2019, 2, 2, 2, 6, timeZone))
  val step9 = IngestStep("9", IngestStepLabel.FEDORA, new DateTime(2019, 2, 2, 2, 7, timeZone))
  val step10 = IngestStep("10", IngestStepLabel.SPRINGFIELD, new DateTime(2019, 2, 2, 2, 8, timeZone))
  val step11 = IngestStep("11", IngestStepLabel.BAGSTORE, new DateTime(2019, 2, 2, 2, 9, timeZone))
  val step12 = IngestStep("12", IngestStepLabel.SOLR4FILES, new DateTime(2019, 2, 2, 2, 10, timeZone))
  val step13 = IngestStep("13", IngestStepLabel.COMPLETED, new DateTime(2019, 2, 2, 2, 11, timeZone))

  val step14 = IngestStep("14", IngestStepLabel.VALIDATE, new DateTime(2019, 5, 5, 4, 5, timeZone))

  val doiRegistered0 = DoiRegisteredEvent(value = false, new DateTime(2019, 1, 1, 0, 0, timeZone))
  val doiRegistered1 = DoiRegisteredEvent(value = true, new DateTime(2019, 1, 1, 4, 7, timeZone))
  val doiRegistered2 = DoiRegisteredEvent(value = false, new DateTime(2019, 2, 2, 0, 0, timeZone))
  val doiRegistered3 = DoiRegisteredEvent(value = true, new DateTime(2019, 2, 2, 2, 7, timeZone))
  val doiRegistered4 = DoiRegisteredEvent(value = false, new DateTime(2019, 5, 5, 0, 0, timeZone))

  val doiAction0 = DoiActionEvent(DoiAction.UPDATE, new DateTime(2019, 1, 1, 1, 1, timeZone))
  val doiAction1 = DoiActionEvent(DoiAction.NONE, new DateTime(2019, 1, 1, 4, 5, timeZone))
  val doiAction2 = DoiActionEvent(DoiAction.CREATE, new DateTime(2019, 2, 2, 0, 0, timeZone))
  val doiAction3 = DoiActionEvent(DoiAction.CREATE, new DateTime(2019, 3, 3, 0, 0, timeZone))
  val doiAction4 = DoiActionEvent(DoiAction.CREATE, new DateTime(2019, 4, 4, 0, 0, timeZone))
  val doiAction5 = DoiActionEvent(DoiAction.UPDATE, new DateTime(2019, 5, 5, 0, 0, timeZone))

  val contentType0 = ContentType("26", ContentTypeValue.ZIP, new DateTime(2019, 1, 1, 0, 5, timeZone))
  val contentType1 = ContentType("27", ContentTypeValue.OCTET, new DateTime(2019, 1, 1, 0, 10, timeZone))
  val contentType2 = ContentType("28", ContentTypeValue.ZIP, new DateTime(2019, 2, 2, 0, 5, timeZone))
  val contentType3 = ContentType("29", ContentTypeValue.ZIP, new DateTime(2019, 3, 3, 0, 5, timeZone))
  val contentType4 = ContentType("30", ContentTypeValue.ZIP, new DateTime(2019, 4, 4, 0, 5, timeZone))
  val contentType5 = ContentType("31", ContentTypeValue.ZIP, new DateTime(2019, 5, 5, 0, 5, timeZone))

  val isNewVersion0 = IsNewVersion("32", value = true, new DateTime(2019, 3, 3, 6, 6, timeZone))
  val isNewVersion1 = IsNewVersion("33", value = false, new DateTime(2019, 4, 4, 0, 0, timeZone))
  val isNewVersion2 = IsNewVersion("34", value = false, new DateTime(2019, 5, 5, 0, 0, timeZone))

  val isCurationRequired0 = IsCurationRequired("35", value = true, new DateTime(2019, 1, 1, 0, 0, timeZone))
  val isCurationRequired1 = IsCurationRequired("36", value = true, new DateTime(2019, 3, 3, 0, 0, timeZone))
  val isCurationRequired2 = IsCurationRequired("37", value = true, new DateTime(2019, 4, 4, 0, 0, timeZone))
  val isCurationRequired3 = IsCurationRequired("38", value = true, new DateTime(2019, 5, 5, 0, 0, timeZone))

  val isCurationPerformed0 = IsCurationPerformed("39", value = false, new DateTime(2019, 1, 1, 0, 0, timeZone))
  val isCurationPerformed1 = IsCurationPerformed("40", value = true, new DateTime(2019, 1, 1, 4, 4, timeZone))
  val isCurationPerformed2 = IsCurationPerformed("41", value = false, new DateTime(2019, 3, 3, 0, 0, timeZone))
  val isCurationPerformed3 = IsCurationPerformed("42", value = true, new DateTime(2019, 3, 3, 6, 6, timeZone))
  val isCurationPerformed4 = IsCurationPerformed("43", value = false, new DateTime(2019, 4, 4, 0, 0, timeZone))
  val isCurationPerformed5 = IsCurationPerformed("44", value = true, new DateTime(2019, 4, 4, 4, 4, timeZone))
  val isCurationPerformed6 = IsCurationPerformed("45", value = false, new DateTime(2019, 5, 5, 0, 0, timeZone))
  val isCurationPerformed7 = IsCurationPerformed("46", value = true, new DateTime(2019, 5, 5, 4, 4, timeZone))

  def prepareTest(query: String): Unit = {
    managed(connection.createStatement())
      .acquireAndGet(_.executeUpdate(query))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    prepareTest {
      File(getClass.getClassLoader.getResource("database/TestData.sql").toURI).contentAsString
    }
  }
}
