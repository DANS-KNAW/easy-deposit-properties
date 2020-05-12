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
import nl.knaw.dans.easy.properties.app.model.curator.Curator
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.schema.DeferredValue

object CuratorResolver {

  val byIdFetcher: ByIdFetcher[Curator] = fetchById(_.repo.curator.getById)
  val currentCuratorsFetcher: CurrentFetcher[Curator] = fetchCurrent(_.repo.curator.getCurrent)
  val allCuratorsFetcher: AllFetcher[Curator] = fetchAll(_.repo.curator.getAll)
  val depositByCuratorIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.curator.getDepositsById)

  def curationById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Curator]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentCuratorsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Curator]] = {
    DeferredValue(currentCuratorsFetcher.deferOpt(depositId))
      .map(_.map { case (_, curator) => curator })
  }

  def allCuratorsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[Curator]] = {
    DeferredValue(allCuratorsFetcher.defer(depositId))
      .map { case (_, curators) => curators }
  }

  def depositByCuratorId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Deposit] = {
    DeferredValue(depositByCuratorIdFetcher.defer(id))
      .map { case (_, deposit) => deposit }
  }
}
