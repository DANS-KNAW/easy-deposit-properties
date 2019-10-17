package nl.knaw.dans.easy.properties.app.repository.sql

import java.util.UUID

import cats.scalatest.EitherValues
import cats.syntax.either._
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }

import scala.util.{ Failure, Success, Try }

class SQLRepoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues {

  "delete" should "succeed with a mix of existing and non existing IDs" in {
    val uuids = Seq(
      "00000000-0000-0000-0000-000000000005",
      "00000000-0000-0000-0000-000000000006",
    ).map(UUID.fromString)

    // just sampling preconditions of one other table
    val stateDao = new SQLStateDao()
    stateDao.getAll(uuids).getOrElse(fail) should matchPattern {
      case List((_, List(_, _, _, _)), (_, List())) =>
    }

    new SQLRepo().deleteDepositsBy(uuids) should matchPattern {
      case Right(List(_)) => // just one of the IDs is found
    }
    new SQLDepositDao().find(uuids).getOrElse(fail) shouldBe empty

    // sampling post conditions of the same other table
    stateDao.getAll(uuids).getOrElse(fail) should matchPattern {
      // deletion succeeds when dropping this tableName in deleteAllFromTables
      // TODO apparently the test database does not throw a ForeignKeyError on deletion
      case List((_, List()), (_, List())) =>
    }
  }

  it should "fail on a null as id" in {
    // TODO MutationError?
    val uuids = Seq(
      UUID.fromString("00000000-0000-0000-0000-000000000005"),
      null,
    )
    val dao = new SQLRepo()
    Try(dao.deleteDepositsBy(uuids)) should matchPattern {
      case Failure(e) if e.isInstanceOf[NullPointerException] =>
    }
  }

  it should "succeed when one of the tables was empty" in {
    val uuids = Seq(
      UUID.fromString("00000000-0000-0000-0000-000000000005"),
    )
    val repo = new SQLRepo()
    repo.repository.identifiers.deleteBy(uuids)

    Try(repo.deleteDepositsBy(uuids)) shouldBe Success(uuids.toList.asRight)
  }
}
