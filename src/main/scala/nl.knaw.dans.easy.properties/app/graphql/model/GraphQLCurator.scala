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
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ CuratorResolver, DepositResolver }
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model._
import nl.knaw.dans.easy.properties.app.model.isnewversion.DepositIsNewVersionFilter
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.DepositIsCurationPerformedFilter
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.DepositIsCurationRequiredFilter
import nl.knaw.dans.easy.properties.app.model.contentType.DepositContentTypeFilter
import nl.knaw.dans.easy.properties.app.model.curator.{ Curator, DepositCuratorFilter }
import nl.knaw.dans.easy.properties.app.model.ingestStep.DepositIngestStepFilter
import nl.knaw.dans.easy.properties.app.model.sort.DepositOrder
import nl.knaw.dans.easy.properties.app.model.state.DepositStateFilter
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import org.joda.time.DateTime
import sangria.macros.derive.{ GraphQLDefault, GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.{ ConnectionArgs, Node }
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("Curator")
@GraphQLDescription("Data manager responsible for curating this deposit.")
class GraphQLCurator(curator: Curator) extends Node {

  @GraphQLField
  @GraphQLDescription("The data manager's username in EASY.")
  val userId: String = curator.userId

  @GraphQLField
  @GraphQLDescription("The data manager's email address.")
  val email: String = curator.email

  @GraphQLField
  @GraphQLDescription("The timestamp at which the data manager was assigned to this deposit.")
  val timestamp: Timestamp = curator.timestamp

  override val id: String = curator.id

  @GraphQLField
  @GraphQLDescription("Returns the deposit that is associated with this particular curator object.")
  def deposit(implicit ctx: Context[DataContext, GraphQLCurator]): DeferredValue[DataContext, GraphQLDeposit] = {
    CuratorResolver.depositByCuratorId(id)
      .map(new GraphQLDeposit(_))
  }

  @GraphQLField
  @GraphQLDescription("List all deposits with the same data manager.")
  def deposits(@GraphQLDescription("Determine whether to search in current curators (`LATEST`, default) only or all current and past curators (`ALL`) of this deposit.") @GraphQLDefault(SeriesFilter.LATEST) curatorFilter: SeriesFilter,
               @GraphQLDescription("Find only those deposits that have this specified bag name.") bagName: Option[String] = None,
               @GraphQLDescription("Find only those deposits that have this specified origin.") origin: Option[Origin] = None,
               @GraphQLDescription("List only those deposits that have this specified state label.") state: Option[DepositStateFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified ingest step label.") ingestStep: Option[DepositIngestStepFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for DOI registered.") doiRegistered: Option[DepositDoiRegisteredFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for DOI action.") doiAction: Option[DepositDoiActionFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for 'is-new-version'.") isNewVersion: Option[DepositIsNewVersionFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for 'curation required'.") curationRequired: Option[DepositIsCurationRequiredFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for 'curation performed'.") curationPerformed: Option[DepositIsCurationPerformedFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified content type.") contentType: Option[DepositContentTypeFilter] = None,
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
              )(implicit ctx: Context[DataContext, GraphQLCurator]): DeferredValue[DataContext, ExtendedConnection[GraphQLDeposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      bagName = bagName,
      originFilter = origin,
      stateFilter = state,
      ingestStepFilter = ingestStep,
      doiRegisteredFilter = doiRegistered,
      doiActionFilter = doiAction,
      curatorFilter = Some(DepositCuratorFilter(userId, curatorFilter)),
      isNewVersionFilter = isNewVersion,
      curationRequiredFilter = curationRequired,
      curationPerformedFilter = curationPerformed,
      contentTypeFilter = contentType,
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
