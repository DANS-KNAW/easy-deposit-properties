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
package nl.knaw.dans.easy.properties.app.repository

import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.contentType.DepositContentTypeFilter
import nl.knaw.dans.easy.properties.app.model.curator.DepositCuratorFilter
import nl.knaw.dans.easy.properties.app.model.ingestStep.DepositIngestStepFilter
import nl.knaw.dans.easy.properties.app.model.sort.DepositOrder
import nl.knaw.dans.easy.properties.app.model.state.DepositStateFilter
import nl.knaw.dans.easy.properties.app.model.{ DepositCurationPerformedFilter, DepositCurationRequiredFilter, DepositDoiActionFilter, DepositDoiRegisteredFilter, DepositIsNewVersionFilter, DepositorId, TimeFilter }

case class DepositFilters(depositorId: Option[DepositorId] = Option.empty,
                          bagName: Option[String] = Option.empty,
                          stateFilter: Option[DepositStateFilter] = Option.empty,
                          ingestStepFilter: Option[DepositIngestStepFilter] = Option.empty,
                          doiRegisteredFilter: Option[DepositDoiRegisteredFilter] = Option.empty,
                          doiActionFilter: Option[DepositDoiActionFilter] = Option.empty,
                          curatorFilter: Option[DepositCuratorFilter] = Option.empty,
                          isNewVersionFilter: Option[DepositIsNewVersionFilter] = Option.empty,
                          curationRequiredFilter: Option[DepositCurationRequiredFilter] = Option.empty,
                          curationPerformedFilter: Option[DepositCurationPerformedFilter] = Option.empty,
                          contentTypeFilter: Option[DepositContentTypeFilter] = Option.empty,
                          originFilter: Option[Origin] = Option.empty,
                          timeFilter: Option[TimeFilter] = Option.empty,
                          sort: Option[DepositOrder] = Option.empty,
                         ) {
  override def toString: String = {
    Seq(
      depositorId,
      bagName,
      stateFilter,
      ingestStepFilter,
      doiRegisteredFilter,
      doiActionFilter,
      curatorFilter,
      isNewVersionFilter,
      curationRequiredFilter,
      curationPerformedFilter,
      contentTypeFilter,
      originFilter,
      timeFilter,
      sort,
    )
      .collect { case Some(x) => x }
      .mkString("DepositFilter(", ", ", ")")
  }
}
