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
package nl.knaw.dans.easy.properties.app.graphql.model

import nl.knaw.dans.easy.properties.app.graphql._
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ DepositResolver, IngestStepResolver }
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.ingestStep.IngestStepLabel.IngestStepLabel
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ DepositIngestStepFilter, IngestStep }
import nl.knaw.dans.easy.properties.app.model.sort.DepositOrder
import nl.knaw.dans.easy.properties.app.model.{ SeriesFilter, TimeFilter, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import org.joda.time.DateTime
import sangria.macros.derive.{ GraphQLDefault, GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.{ ConnectionArgs, Node }
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("IngestStep")
@GraphQLDescription("The ingest step of the deposit.")
class GraphQLIngestStep(ingestStep: IngestStep) extends Node {

  @GraphQLField
  @GraphQLDescription("The label of the ingest step.")
  val step: IngestStepLabel = ingestStep.step

  @GraphQLField
  @GraphQLDescription("The timestamp at which the deposit got into this ingest step.")
  val timestamp: Timestamp = ingestStep.timestamp

  override val id: String = ingestStep.id

  @GraphQLField
  @GraphQLDescription("Returns the deposit that is associated with this particular ingest step.")
  def deposit(implicit ctx: Context[DataContext, GraphQLIngestStep]): DeferredValue[DataContext, GraphQLDeposit] = {
    IngestStepResolver.depositByIngestStepId(id)
      .map(new GraphQLDeposit(_))
  }

  @GraphQLField
  @GraphQLDescription("List all deposits with the same current ingest step.")
  def deposits(@GraphQLDescription("Determine whether to search in current ingest steps (`LATEST`, default) or all current and past ingest steps (`ALL`).") @GraphQLDefault(SeriesFilter.LATEST) ingestStepFilter: SeriesFilter,
               @GraphQLDescription("Ordering options for the returned deposits.") orderBy: Option[DepositOrder] = None,
               @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
               @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
               @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
               before: Option[String] = None,
               after: Option[String] = None,
               first: Option[Int] = None,
               last: Option[Int] = None,
              )(implicit ctx: Context[DataContext, GraphQLIngestStep]): DeferredValue[DataContext, ExtendedConnection[GraphQLDeposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      ingestStepFilter = Some(DepositIngestStepFilter(step, ingestStepFilter)),
      timeFilter = TimeFilter(earlierThan, laterThan, atTimestamp),
      sort = orderBy,
    ))
      .map(deposits => ExtendedConnection.connectionFromSeq(
        deposits.map(new GraphQLDeposit(_)),
        ConnectionArgs(before, after, first, last),
      ))
  }
}
