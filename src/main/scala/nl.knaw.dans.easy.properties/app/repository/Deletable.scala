package nl.knaw.dans.easy.properties.app.repository

import nl.knaw.dans.easy.properties.app.model.DepositId

trait Deletable {

  /** @return rowCount */
  def deleteBy(ids: Seq[DepositId]): Either[MutationError, Int]
}
