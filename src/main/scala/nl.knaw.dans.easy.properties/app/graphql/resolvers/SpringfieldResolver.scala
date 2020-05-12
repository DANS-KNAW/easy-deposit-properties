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
import nl.knaw.dans.easy.properties.app.model.springfield.Springfield
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.schema.DeferredValue

object SpringfieldResolver {

  val byIdFetcher: ByIdFetcher[Springfield] = fetchById(_.repo.springfield.getById)
  val currentSpringfieldsFetcher: CurrentFetcher[Springfield] = fetchCurrent(_.repo.springfield.getCurrent)
  val allSpringfieldsFetcher: AllFetcher[Springfield] = fetchAll(_.repo.springfield.getAll)
  val depositBySpringfieldIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.springfield.getDepositsById)

  def springfieldById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Springfield]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Springfield]] = {
    DeferredValue(currentSpringfieldsFetcher.deferOpt(depositId))
      .map(_.map { case (_, springfield) => springfield })
  }

  def allById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[Springfield]] = {
    DeferredValue(allSpringfieldsFetcher.defer(depositId))
      .map { case (_, springfields) => springfields }
  }

  def depositBySpringfieldId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Deposit] = {
    DeferredValue(depositBySpringfieldIdFetcher.defer(id))
      .map { case (_, deposit) => deposit }
  }
}
