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
import nl.knaw.dans.easy.properties.app.model.isnewversion.IsNewVersion
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.schema.DeferredValue

object IsNewVersionResolver {

  val byIdFetcher: ByIdFetcher[IsNewVersion] = fetchById(_.repo.isNewVersion.getById)
  val currentIsNewVersionsFetcher: CurrentFetcher[IsNewVersion] = fetchCurrent(_.repo.isNewVersion.getCurrent)
  val allIsNewVersionsFetcher: AllFetcher[IsNewVersion] = fetchAll(_.repo.isNewVersion.getAll)
  val depositByIsNewVersionIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.isNewVersion.getDepositsById)

  def isNewVersionById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[IsNewVersion]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentIsNewVersionById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[IsNewVersion]] = {
    DeferredValue(currentIsNewVersionsFetcher.deferOpt(depositId))
      .map(_.map { case (_, isNewVersion) => isNewVersion })
  }

  def allIsNewVersionsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[IsNewVersion]] = {
    DeferredValue(allIsNewVersionsFetcher.defer(depositId))
      .map { case (_, isNewVersions) => isNewVersions }
  }

  def depositByIsNewVersionId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Deposit] = {
    DeferredValue(depositByIsNewVersionIdFetcher.defer(id))
      .map { case (_, deposit) => deposit }
  }
}
