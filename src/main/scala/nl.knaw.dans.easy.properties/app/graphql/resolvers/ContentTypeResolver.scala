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
import nl.knaw.dans.easy.properties.app.model.contentType.ContentType
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import sangria.schema.DeferredValue

object ContentTypeResolver {

  val byIdFetcher: ByIdFetcher[ContentType] = fetchById(_.repo.contentType.getById)
  val currentContentTypesFetcher: CurrentFetcher[ContentType] = fetchCurrent(_.repo.contentType.getCurrent)
  val allContentTypesFetcher: AllFetcher[ContentType] = fetchAll(_.repo.contentType.getAll)
  val depositByContentTypeIdFetcher: DepositByIdFetcher = fetchDepositsById(_.repo.contentType.getDepositsById)

  def contentTypeById(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[ContentType]] = {
    DeferredValue(byIdFetcher.deferOpt(id))
  }

  def currentById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[ContentType]] = {
    DeferredValue(currentContentTypesFetcher.deferOpt(depositId))
      .map(_.map { case (_, contentType) => contentType })
  }

  def allById(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[ContentType]] = {
    DeferredValue(allContentTypesFetcher.defer(depositId))
      .map { case (_, contentTypes) => contentTypes }
  }

  def depositByContentTypeId(id: String)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Deposit]] = {
    DeferredValue(depositByContentTypeIdFetcher.defer(id))
      .map { case (_, optDeposit) => optDeposit }
  }
}
