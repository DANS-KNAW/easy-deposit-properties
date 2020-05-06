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
package nl.knaw.dans.easy.properties.app.graphql.resolvers

import nl.knaw.dans.easy.properties.app.graphql._
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.IsCurationPerformed
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.schema.DeferredValue

object IsCurationPerformedResolver {

  val byIdFetcher: ByIdFetcher[IsCurationPerformed] = fetchById(_.repo.isCurationPerformed.getById)
  val currentIsCurationPerformedsFetcher: CurrentFetcher[IsCurationPerformed] = fetchCurrent(_.repo.isCurationPerformed.getCurrent)
  val allIsCurationPerformedsFetcher: AllFetcher[IsCurationPerformed] = fetchAll(_.repo.isCurationPerformed.getAll)
  val depositByIsCurationPerformedIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.isCurationPerformed.getDepositsById)

  def isCurationPerformedById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[IsCurationPerformed]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentIsCurationPerformedById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[IsCurationPerformed]] = {
    DeferredValue(currentIsCurationPerformedsFetcher.deferOpt(depositId))
      .map(_.map { case (_, isCurationPerformed) => isCurationPerformed })
  }

  def allIsCurationPerformedsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[IsCurationPerformed]] = {
    DeferredValue(allIsCurationPerformedsFetcher.defer(depositId))
      .map { case (_, isCurationPerformeds) => isCurationPerformeds }
  }

  def depositByIsCurationPerformedId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Deposit]] = {
    DeferredValue(depositByIsCurationPerformedIdFetcher.deferOpt(id))
      .map(_.map { case (_, deposit) => deposit })
  }
}
