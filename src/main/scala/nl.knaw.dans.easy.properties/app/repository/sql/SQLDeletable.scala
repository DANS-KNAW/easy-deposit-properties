package nl.knaw.dans.easy.properties.app.repository.sql

import java.sql.Connection

import cats.data.NonEmptyList
import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.{ Deletable, MutationError }
import resource.managed

trait SQLDeletable extends Deletable {
  implicit val connection: Connection
  private[sql] val daoName: String

  /** @return rowCount */
  def deleteBy(ids: Seq[DepositId]): Either[MutationError, Int] = {
    NonEmptyList.fromList(ids.toList)
      .map(delete)
      .getOrElse(0.asRight)
  }

  /** @return rowCount */
  private def delete(ids: NonEmptyList[DepositId]): Either[MutationError, Int] = {
    managed(connection.prepareStatement(getQuery(ids)))
      .map(statement =>
        statement.executeUpdateWith(ids.map(_.toString).toList)
      )
      .either
      .either
      .leftMap(throwables => {
        assert(throwables.nonEmpty)
        MutationError(throwables.map(_.getMessage).mkString("; "))
      })
  }

  private[sql] def getQuery(ids: NonEmptyList[DepositId]): String = {
    QueryGenerator.deleteByDepositId(daoName)(ids)
  }
}
