package nl.knaw.dans.easy.properties.app.repository.sql

import java.sql.Connection

import cats.data.NonEmptyList
import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.MutationError
import resource.managed

trait SQLDeletable {
  implicit val connection: Connection
  private[sql] val tableName: String

  /** @return rowCount */
  def deleteBy(ids: Seq[DepositId]): Either[MutationError, Int] = {
    NonEmptyList.fromList(ids.toList)
      .map(delete)
      .getOrElse(0.asRight)
  }

  /** @return rowCount */
  private[sql] def delete(ids: NonEmptyList[DepositId]): Either[MutationError, Int] = {
    val query = QueryGenerator.deleteByDepositId(tableName)(ids)
    managed(connection.prepareStatement(query))
      .map(_.executeUpdateWith(ids.map(_.toString).toList))
      .either
      .either
      .leftMap(throwables => {
        assert(throwables.nonEmpty)
        MutationError(throwables.map(_.getMessage).mkString("; "))
      })
  }
}
