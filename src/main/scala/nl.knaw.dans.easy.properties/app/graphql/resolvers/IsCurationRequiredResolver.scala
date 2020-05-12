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
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.IsCurationRequired
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.schema.DeferredValue

object IsCurationRequiredResolver {

  val byIdFetcher: ByIdFetcher[IsCurationRequired] = fetchById(_.repo.isCurationRequired.getById)
  val currentIsCurationRequiredsFetcher: CurrentFetcher[IsCurationRequired] = fetchCurrent(_.repo.isCurationRequired.getCurrent)
  val allIsCurationRequiredsFetcher: AllFetcher[IsCurationRequired] = fetchAll(_.repo.isCurationRequired.getAll)
  val depositByIsCurationRequiredIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.isCurationRequired.getDepositsById)

  def isCurationRequiredById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[IsCurationRequired]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentIsCurationRequiredById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[IsCurationRequired]] = {
    DeferredValue(currentIsCurationRequiredsFetcher.deferOpt(depositId))
      .map(_.map { case (_, isCurationRequired) => isCurationRequired })
  }

  def allIsCurationRequiredsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[IsCurationRequired]] = {
    DeferredValue(allIsCurationRequiredsFetcher.defer(depositId))
      .map { case (_, isCurationRequireds) => isCurationRequireds }
  }

  def depositByIsCurationRequiredId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Deposit] = {
    DeferredValue(depositByIsCurationRequiredIdFetcher.defer(id))
      .map { case (_, deposit) => deposit }
  }
}
