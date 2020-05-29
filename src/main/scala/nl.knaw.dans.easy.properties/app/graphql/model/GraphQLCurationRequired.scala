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
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ DepositResolver, IsCurationRequiredResolver }
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.{ DepositIsCurationRequiredFilter, IsCurationRequired }
import nl.knaw.dans.easy.properties.app.model.sort.DepositOrder
import nl.knaw.dans.easy.properties.app.model.{ SeriesFilter, TimeFilter, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import org.joda.time.DateTime
import sangria.macros.derive.{ GraphQLDefault, GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.{ ConnectionArgs, Node }
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("IsCurationRequired")
@GraphQLDescription("Whether curation by data manager is required.")
class GraphQLCurationRequired(curationRequired: IsCurationRequired) extends Node {

  @GraphQLField
  @GraphQLDescription("True if curation by a data manager is required.")
  val value: Boolean = curationRequired.value

  @GraphQLField
  @GraphQLDescription("The timestamp at which was decided that this deposit requires curation.")
  val timestamp: Timestamp = curationRequired.timestamp

  override val id: String = curationRequired.id

  @GraphQLField
  @GraphQLDescription("Returns the deposit that is associated with this particular IsCurationRequiredEvent")
  def deposit(implicit ctx: Context[DataContext, GraphQLCurationRequired]): DeferredValue[DataContext, GraphQLDeposit] = {
    IsCurationRequiredResolver.depositByIsCurationRequiredId(id)
      .map(new GraphQLDeposit(_))
  }

  @GraphQLField
  @GraphQLDescription("List all deposits with the same current IsCurationRequiredEvent value.")
  def deposits(@GraphQLDescription("Determine whether to search in current IsCurationRequiredEvents (`LATEST`, default) or all current and past IsCurationRequiredEvents (`ALL`).") @GraphQLDefault(SeriesFilter.LATEST) isCurationRequiredFilter: SeriesFilter,
               @GraphQLDescription("Ordering options for the returned deposits.") orderBy: Option[DepositOrder] = None,
               @GraphQLDescription("List only those deposits that have a creation timestamp earlier than this given timestamp.") createdEarlierThan: Option[DateTime] = None,
               @GraphQLDescription("List only those deposits that have a creation timestamp later than this given timestamp.") createdLaterThan: Option[DateTime] = None,
               @GraphQLDescription("List only those deposits that have a creation timestamp equal to the given timestamp.") createdAtTimestamp: Option[DateTime] = None,
               @GraphQLDescription("List only those deposits that have a last modified timestamp earlier than this given timestamp.") lastModifiedEarlierThan: Option[DateTime] = None,
               @GraphQLDescription("List only those deposits that have a last modified timestamp later than this given timestamp.") lastModifiedLaterThan: Option[DateTime] = None,
               @GraphQLDescription("List only those deposits that have a last modified timestamp equal to the given timestamp.") lastModifiedAtTimestamp: Option[DateTime] = None,
               before: Option[String] = None,
               after: Option[String] = None,
               first: Option[Int] = None,
               last: Option[Int] = None,
              )(implicit ctx: Context[DataContext, GraphQLCurationRequired]): DeferredValue[DataContext, ExtendedConnection[GraphQLDeposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      curationRequiredFilter = Some(DepositIsCurationRequiredFilter(value, isCurationRequiredFilter)),
      creationTimeFilter = TimeFilter(createdEarlierThan, createdLaterThan, createdAtTimestamp),
      lastModifiedTimeFilter = TimeFilter(lastModifiedEarlierThan, lastModifiedLaterThan, lastModifiedAtTimestamp),
      sort = orderBy,
    ))
      .map(deposits => ExtendedConnection.connectionFromSeq(
        deposits.map(new GraphQLDeposit(_)),
        ConnectionArgs(before, after, first, last),
      ))
  }
}
