package nl.knaw.dans.easy.properties.app.repository.sql

import cats.data.NonEmptyList
import nl.knaw.dans.easy.properties.app.model.DepositId

trait SQLDeletableProperty extends SQLDeletable {
  private[sql] val key: String

  override private[sql] def getQuery(ids: NonEmptyList[DepositId]) = {
    QueryGenerator.deleteByDepositId("SimpleProperties", key)(ids)
  }
}
