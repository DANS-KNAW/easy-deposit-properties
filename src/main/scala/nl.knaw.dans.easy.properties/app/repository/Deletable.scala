package nl.knaw.dans.easy.properties.app.repository

import nl.knaw.dans.easy.properties.app.model.DepositId

trait Deletable {
  def delete(ids: Seq[DepositId]): Either[MutationError, Int]
}
