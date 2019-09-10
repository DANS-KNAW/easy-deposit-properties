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

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.model.curation.Curation
import nl.knaw.dans.easy.properties.app.model.curator.Curator
import nl.knaw.dans.easy.properties.app.model.{ CurationPerformedEvent, CurationRequiredEvent, Deposit, DepositId, IsNewVersionEvent }
import sangria.schema.DeferredValue

object CurationResolver {

  val byIdFetcher: ByIdFetcher[Curation] = fetchById(_.repo.curation.getById)
  val currentCurationsFetcher: CurrentFetcher[Curation] = fetchCurrent(_.repo.curation.getCurrent)
  val allCurationsFetcher: AllFetcher[Curation] = fetchAll(_.repo.curation.getAll)
  val depositByCurationIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.curation.getDepositsById)

  def curationById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Curation]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentCuratorsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Curator]] = {
    DeferredValue(allCurationsFetcher.defer(depositId))
      .map { case (_, curations) => curations
        .map(_.getCurator)
        .distinctUntilChanged(curator => (curator.userId, curator.email))
        .lastOption
      }
  }

  def allCuratorsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[Curator]] = {
    DeferredValue(allCurationsFetcher.defer(depositId))
      .map { case (_, curations) => curations
        .map(_.getCurator)
        .distinctUntilChanged(curator => (curator.userId, curator.email))
      }
  }

  def depositByCurationId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Deposit]] = {
    DeferredValue(depositByCurationIdFetcher.defer(id))
      .map { case (_, optDeposit) => optDeposit }
  }

  def isNewVersion(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Boolean]] = {
    DeferredValue(currentCurationsFetcher.deferOpt(depositId))
      .map(_.flatMap { case (_, curation) => curation.isNewVersion })
  }

  def allIsNewVersionEvents(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[IsNewVersionEvent]] = {
    DeferredValue(allCurationsFetcher.defer(depositId))
      .map { case (_, curations) => curations.map(_.getIsNewVersionEvent).distinctUntilChanged(_.isNewVersion) }
  }

  def isCurationRequired(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Boolean]] = {
    DeferredValue(currentCurationsFetcher.deferOpt(depositId))
      .map(_.map { case (_, curation) => curation.isRequired })
  }

  def allIsCurationRequiredEvents(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[CurationRequiredEvent]] = {
    DeferredValue(allCurationsFetcher.defer(depositId))
      .map { case (_, curations) => curations.map(_.getCurationRequiredEvent).distinctUntilChanged(_.curationRequired) }
  }

  def isCurationPerformed(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Boolean]] = {
    DeferredValue(currentCurationsFetcher.deferOpt(depositId))
      .map(_.map { case (_, curation) => curation.isPerformed })
  }

  def allIsCurationPerformedEvents(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[CurationPerformedEvent]] = {
    DeferredValue(allCurationsFetcher.defer(depositId))
      .map { case (_, curations) => curations.map(_.getCurationPerformedEvent).distinctUntilChanged(_.curationPerformed) }
  }
}
