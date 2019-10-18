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

import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.{ Deletable, MutationError, Repository }

class Deleter(repository: => Repository) {

  def deleteDepositsBy(ids: Seq[DepositId]): Either[MutationError, Seq[DepositId]] = {
    for {
      deposits <- repository.deposits.find(ids).leftMap(error => MutationError(error.msg))
      actualIds = deposits.map(_.id).toList
      _ = deleteDeposits(actualIds)
    } yield actualIds
  }

  private def deleteDeposits(ids: Seq[DepositId]): Either[MutationError, Int] = {
    Stream[Deletable](
      repository.states,
      repository.identifiers,
      repository.curation,
      repository.springfield,
      repository.ingestSteps,
      repository.doiRegistered,
      repository.doiAction,
      repository.contentType,
      repository.deposits, // last because of foreign keys by the others
    ).map(_.deleteBy(ids))
      .find(_.isLeft) // the stream makes it a fail fast
      .getOrElse(0.asRight)
  }
}
