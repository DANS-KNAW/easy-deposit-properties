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
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.repository.{ InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLIdentifierDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find identifiers identified by their identifierId" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getById(Seq("7", "9", "13")).value should contain inOrderOnly(identifier7, identifier9, identifier13)
  }

  it should "return an empty collection if the identifierId is unknown" in {
    val identifiers = new SQLIdentifierDao
    val unknownIdentifierId = "102"

    identifiers.getById(Seq(unknownIdentifierId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid identifierId is given" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getByType" should "find the identifiers with the given depositIds and types" in {
    val identifiers = new SQLIdentifierDao
    val input = Seq(depositId2 -> IdentifierType.FEDORA, depositId5 -> IdentifierType.BAG_STORE)

    identifiers.getByType(input).value should contain only(
      (depositId2 -> IdentifierType.FEDORA) -> identifier7,
      (depositId5 -> IdentifierType.BAG_STORE) -> identifier13,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val identifiers = new SQLIdentifierDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    identifiers.getByType(Seq(depositId6 -> IdentifierType.BAG_STORE)).value shouldBe empty
  }

  it should "return an empty collection if there is no identifier with the given type for this deposit" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getByType(Seq(depositId5 -> IdentifierType.FEDORA)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getByType(Seq.empty).value shouldBe empty
  }

  "getByTypesAndValues" should "find the identifiers with the given types and values" in {
    val identifiers = new SQLIdentifierDao
    val input = Seq(IdentifierType.FEDORA -> "easy-dataset:1", IdentifierType.DOI -> "10.5072/dans-p7q-rst8")

    identifiers.getByTypesAndValues(input).value should contain only(
      (IdentifierType.FEDORA -> "easy-dataset:1") -> identifier3,
      (IdentifierType.DOI -> "10.5072/dans-p7q-rst8") -> identifier10,
    )
  }

  it should "return an empty collection if there is no identifier with the given value for this type" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getByTypesAndValues(Seq(IdentifierType.URN -> "easy-dataset:1")).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getByTypesAndValues(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all identifiers associated with the given deposits" in {
    val identifiers = new SQLIdentifierDao

    val results = identifiers.getAll(Seq(depositId2, depositId5)).value.toMap
    results.keySet should contain only(depositId2, depositId5)
    results(depositId2) should contain only(identifier4, identifier5, identifier6, identifier7)
    results(depositId5) should contain only identifier13
  }

  it should "return an empty collection if the depositId is unknown" in {
    val identifiers = new SQLIdentifierDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    identifiers.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new state into the database" in {
    val identifiers = new SQLIdentifierDao
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputIdentifier = InputIdentifier(IdentifierType.FEDORA, "easy-dataset:12345", timestamp)
    val expectedIdentifier = Identifier("14", IdentifierType.FEDORA, "easy-dataset:12345", timestamp)

    identifiers.store(depositId5, inputIdentifier).value shouldBe expectedIdentifier
    identifiers.getById(Seq("14")).value should contain only expectedIdentifier
    identifiers.getByTypesAndValues(Seq(IdentifierType.FEDORA -> "easy-dataset:12345")).value should contain only ((IdentifierType.FEDORA -> "easy-dataset:12345") -> expectedIdentifier)
    identifiers.getAll(Seq(depositId5)).value.toMap.apply(depositId5) should contain(expectedIdentifier)
  }

  it should "fail when the given depositId does not exist" in {
    val identifiers = new SQLIdentifierDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputIdentifier = InputIdentifier(IdentifierType.FEDORA, "easy-dataset:12345", timestamp)

    identifiers.store(depositId6, inputIdentifier).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId, schema and timestamp combination is already present, even though the value is different" in {
    val identifiers = new SQLIdentifierDao
    val depositId = depositId3
    val timestamp = new DateTime(2019, 3, 3, 1, 1, timeZone)
    val inputIdentifier1 = InputIdentifier(IdentifierType.FEDORA, "easy-dataset:12345", timestamp)
    val inputIdentifier2 = InputIdentifier(IdentifierType.FEDORA, "easy-dataset:56789", timestamp)

    identifiers.store(depositId, inputIdentifier1) shouldBe right
    identifiers.store(depositId, inputIdentifier2).leftValue.msg should include(s"identifier fedora already exists for depositId $depositId")
  }

  it should "fail when the identifier type and value combination is already present (for another deposit), even though the timestamp and depositId are different" in {
    val identifiers = new SQLIdentifierDao
    val depositId = depositId3
    val timestamp = new DateTime(2019, 3, 3, 1, 1, timeZone)
    val idType = identifier5.idType
    val idValue = identifier5.idValue
    // NOTE: identifier5 was already associated with depositId2
    val inputIdentifier = InputIdentifier(idType, idValue, timestamp)

    identifiers.store(depositId, inputIdentifier).leftValue.msg should include(s"$idType '$idValue' is already associated with another deposit")
  }

  "getDepositsById" should "find deposits identified by these identifierIds" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getDepositsById(Seq("6", "8", "13")).value should contain only(
      "6" -> deposit2,
      "8" -> deposit3,
      "13" -> deposit5,
    )
  }

  it should "return an empty collection if the identifierId is unknown" in {
    val identifiers = new SQLIdentifierDao
    val unknownIdentifierId = "102"

    identifiers.getDepositsById(Seq(unknownIdentifierId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid identifierId is given" in {
    val identifiers = new SQLIdentifierDao

    identifiers.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
