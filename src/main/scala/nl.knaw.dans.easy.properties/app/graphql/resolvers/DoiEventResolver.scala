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
import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.{ DepositId, DoiActionEvent, DoiRegisteredEvent }
import sangria.schema.DeferredValue

object DoiEventResolver {

  val currentDoisRegisteredFetcher: CurrentFetcher[DoiRegisteredEvent] = fetchCurrent(_.repo.doiRegistered.getCurrent)
  val allDoisRegisteredFetcher: AllFetcher[DoiRegisteredEvent] = fetchAll(_.repo.doiRegistered.getAll)
  val currentDoisActionFetcher: CurrentFetcher[DoiActionEvent] = fetchCurrent(_.repo.doiAction.getCurrent)
  val allDoisActionFetcher: AllFetcher[DoiActionEvent] = fetchAll(_.repo.doiAction.getAll)

  def isDoiRegistered(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Boolean]] = {
    DeferredValue(currentDoisRegisteredFetcher.deferOpt(depositId))
      .map(_.map { case (_, doiRegisteredEvent) => doiRegisteredEvent.value })
  }

  def allDoiRegisteredById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[DoiRegisteredEvent]] = {
    DeferredValue(allDoisRegisteredFetcher.defer(depositId))
      .map { case (_, doiRegisteredEvents) => doiRegisteredEvents }
  }

  def currentDoiActionById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[DoiAction]] = {
    DeferredValue(currentDoisActionFetcher.deferOpt(depositId))
      .map(_.map { case (_, doiActionEvent) => doiActionEvent.value })
  }

  def allDoiActionsById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[DoiActionEvent]] = {
    DeferredValue(allDoisActionFetcher.defer(depositId))
      .map { case (_, doiActionEvents) => doiActionEvents }
  }
}
