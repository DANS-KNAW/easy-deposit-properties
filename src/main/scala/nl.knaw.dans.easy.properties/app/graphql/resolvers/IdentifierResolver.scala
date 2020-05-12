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
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.execution.deferred.Fetcher
import sangria.schema.DeferredValue

object IdentifierResolver {

  type IdentifiersByTypeFetcher = Fetcher[DataContext, ((DepositId, IdentifierType), Identifier), ((DepositId, IdentifierType), Identifier), (DepositId, IdentifierType.Value)]
  type IdentifiersByTypeAndValueFetcher = Fetcher[DataContext, ((IdentifierType, String), Identifier), ((IdentifierType, String), Identifier), (IdentifierType, String)]

  val byIdFetcher: ByIdFetcher[Identifier] = fetchById(_.repo.identifiers.getById)
  val identifiersByTypeFetcher: IdentifiersByTypeFetcher = Fetcher.caching(_.repo.identifiers.getByType(_).toFuture)
  val identifierTypesAndValuesFetcher: IdentifiersByTypeAndValueFetcher = Fetcher.caching(_.repo.identifiers.getByTypesAndValues(_).toFuture)
  val identifiersByDepositIdFetcher: AllFetcher[Identifier] = fetchAll(_.repo.identifiers.getAll)
  val depositByIdentifierIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.identifiers.getDepositsById)

  def identifierById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Identifier]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def identifierByType(depositId: DepositId, idType: IdentifierType.Value)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Identifier]] = {
    DeferredValue(identifiersByTypeFetcher.deferOpt(depositId -> idType))
      .map(_.map { case (_, identifier) => identifier })
  }
  
  def identifierByTypeAndValue(idType: IdentifierType, idValue: String)(implicit ctx: DataContext): DeferredValue[DataContext, Identifier] = {
    DeferredValue(identifierTypesAndValuesFetcher.defer(idType -> idValue))
      .map { case (_, identifier) => identifier }
  }

  def allById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[Identifier]] = {
    DeferredValue(identifiersByDepositIdFetcher.defer(depositId))
      .map { case (_, identifiers) => identifiers }
  }

  def depositByIdentifierId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Deposit] = {
    DeferredValue(depositByIdentifierIdFetcher.defer(id))
      .map { case (_, deposit) => deposit }
  }
}
