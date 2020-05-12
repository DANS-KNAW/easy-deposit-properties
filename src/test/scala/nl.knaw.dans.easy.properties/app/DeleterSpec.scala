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
package nl.knaw.dans.easy.properties.app

import java.util.UUID

import cats.scalatest.EitherValues
import cats.syntax.either._
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }

class DeleterSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues {

  private val uuid5: UUID = depositId5 // exists
  private val uuid6: UUID = UUID.fromString("00000000-0000-0000-0000-000000000006") // does not exist

  "delete" should "fail with a foreign key violation" in {
    repository.deposits.deleteBy(Seq(uuid5)).leftValue.msg should {
      include("integrity constraint violation: foreign key no action") and include("table: STATE")
    }
  }

  it should "succeed with a mix of existing and non existing IDs" in {
    val uuids = Seq(uuid5, uuid6)
    val deleter = new Deleter(repository)

    // just sampling preconditions of one other table
    repository.states.getAll(uuids).getOrElse(fail) should matchPattern {
      case List((`uuid5`, List(_, _, _, _)), (`uuid6`, List())) => // 4 states were found for uuid5, none for uuid6
    }

    deleter.deleteDepositsBy(uuids) should matchPattern {
      case Right(List(_)) => // just one of the IDs is found in the deposits table
    }
    repository.deposits.find(uuids).getOrElse(fail) shouldBe empty

    // sampling post conditions of the same other table
    repository.states.getAll(uuids).getOrElse(fail) should matchPattern {
      case List((`uuid5`, List()), (`uuid6`, List())) => // no states at all any more
    }
  }

  it should "succeed with a non existing ID" in {
    new Deleter(repository).deleteDepositsBy(Seq(uuid6)) should matchPattern {
      case Right(List()) =>
    }
  }

  it should "succeed when one of the tables was empty" in {
    val uuids = Seq(uuid5)
    val deleter = new Deleter(repository)
    repository.identifiers.deleteBy(uuids) // clear one of the DAOs

    deleter.deleteDepositsBy(uuids) shouldBe uuids.toList.asRight
  }
}
