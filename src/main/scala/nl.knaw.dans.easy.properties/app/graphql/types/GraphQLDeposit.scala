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
package nl.knaw.dans.easy.properties.app.graphql.types

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.ordering._
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ ContentTypeResolver, CurationResolver, DepositResolver, DoiEventResolver, IdentifierResolver, IngestStepResolver, SpringfieldResolver, StateResolver }
import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, Timestamp, timestampOrdering }
import org.joda.time.DateTime
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.{ ConnectionArgs, Node }
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("Deposit")
@GraphQLDescription("Contains all technical metadata about this deposit.")
class GraphQLDeposit(deposit: Deposit) extends Node {

  @GraphQLField
  @GraphQLDescription("The identifier of the deposit.")
  val depositId: DepositId = deposit.id

  @GraphQLField
  @GraphQLDescription("The name of the deposited bag.")
  val bagName: Option[String] = deposit.bagName

  @GraphQLField
  @GraphQLDescription("The moment this deposit was created.")
  val creationTimestamp: Timestamp = deposit.creationTimestamp

  @GraphQLField
  @GraphQLDescription("The origin of the deposit.")
  val origin: Origin = deposit.origin

  override val id: String = depositId.toString

  @GraphQLField
  @GraphQLDescription("Get the timestamp at which this deposit was last modified. If the dataset was only created, the creation timestamp is returned.")
  def lastModified(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[Timestamp]] = {
    DepositResolver.lastModified(deposit.id)
  }

  @GraphQLField
  @GraphQLDescription("The current state of the deposit.")
  def state(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLState]] = {
    StateResolver.currentById(deposit.id)
      .map(_.map(new GraphQLState(_)))
  }

  @GraphQLField
  @GraphQLDescription("List all states of the deposit.")
  def states(@GraphQLDescription("Ordering options for the returned states.") orderBy: Option[StateOrder],
             @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
             @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
             @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
             before: Option[String] = None,
             after: Option[String] = None,
             first: Option[Int] = None,
             last: Option[Int] = None,
            )(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, ExtendedConnection[GraphQLState]] = {
    StateResolver.allById(deposit.id)
      .map(TimebasedSearch(earlierThan, laterThan, atTimestamp, orderBy))
      .map(states => ExtendedConnection.connectionFromSeq(
        states.map(new GraphQLState(_)),
        ConnectionArgs(before, after, first, last),
      ))
  }

  @GraphQLField
  @GraphQLDescription("The current ingest step of the deposit.")
  def ingestStep(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLIngestStep]] = {
    IngestStepResolver.currentById(deposit.id)
      .map(_.map(new GraphQLIngestStep(_)))
  }

  @GraphQLField
  @GraphQLDescription("List all ingest steps of the deposit.")
  def ingestSteps(@GraphQLDescription("Ordering options for the returned ingest steps.") orderBy: Option[IngestStepOrder],
                  @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
                  @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
                  @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
                  before: Option[String] = None,
                  after: Option[String] = None,
                  first: Option[Int] = None,
                  last: Option[Int] = None,
                 )(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, ExtendedConnection[GraphQLIngestStep]] = {
    IngestStepResolver.allById(deposit.id)
      .map(TimebasedSearch(earlierThan, laterThan, atTimestamp, orderBy))
      .map(ingestSteps => ExtendedConnection.connectionFromSeq(
        ingestSteps.map(new GraphQLIngestStep(_)),
        ConnectionArgs(before, after, first, last),
      ))
  }

  @GraphQLField
  @GraphQLDescription("Information about the depositor that submitted this deposit.")
  def depositor(implicit ctx: Context[DataContext, GraphQLDeposit]): GraphQLDepositor = {
    new GraphQLDepositor(deposit.depositorId)
  }

  @GraphQLField
  @GraphQLDescription("Return the identifier of the given type related to this deposit.")
  def identifier(@GraphQLName("type") @GraphQLDescription("Find the identifier with this specific type.") idType: IdentifierType.Value)
                (implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLIdentifier]] = {
    IdentifierResolver.identifierByType(deposit.id, idType)
      .map(_.map(new GraphQLIdentifier(_)))
  }

  @GraphQLField
  @GraphQLDescription("List the identifiers related to this deposit.")
  def identifiers(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLIdentifier]] = {
    IdentifierResolver.allById(deposit.id)
      .map(_.map(new GraphQLIdentifier(_)))
  }

  @GraphQLField
  @GraphQLDescription("Returns whether the DOI is registered in DataCite.")
  def doiRegistered(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[Boolean]] = {
    DoiEventResolver.isDoiRegistered(deposit.id)
  }

  @GraphQLField
  @GraphQLDescription("Lists all state changes related to the registration of the DOI in DataCite.")
  def doiRegisteredEvents(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLDoiRegistered]] = {
    DoiEventResolver.allDoiRegisteredById(deposit.id)
      .map(_.sortBy(_.timestamp).map(new GraphQLDoiRegistered(_)))
  }

  @GraphQLField
  @GraphQLDescription("Returns whether the DOI should be 'created' or 'updated' on registration in DataCite.")
  def doiAction(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[DoiAction]] = {
    DoiEventResolver.currentDoiActionById(deposit.id)
  }

  @GraphQLField
  @GraphQLDescription("Lists all state changes related to whether the DOI should be 'created' or 'updated' on registration in DataCite.")
  def doiActionEvents(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLDoiAction]] = {
    DoiEventResolver.allDoiActionsById(deposit.id)
      .map(_.sortBy(_.timestamp).map(new GraphQLDoiAction(_)))
  }

  @GraphQLField
  @GraphQLDescription("The data manager currently assigned to this deposit.")
  def curator(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLCurator]] = {
    CurationResolver.currentCuratorsById(deposit.id)
      .map(_.map(new GraphQLCurator(_)))
  }

  @GraphQLField
  @GraphQLDescription("List all data manager that were ever assigned to this deposit.")
  def curators(@GraphQLDescription("Ordering options for the returned curators.") orderBy: Option[CuratorOrder],
               @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
               @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
               @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
               before: Option[String] = None,
               after: Option[String] = None,
               first: Option[Int] = None,
               last: Option[Int] = None,
              )(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, ExtendedConnection[GraphQLCurator]] = {
    CurationResolver.allCuratorsById(deposit.id)
      .map(TimebasedSearch(earlierThan, laterThan, atTimestamp, orderBy))
      .map(curators => ExtendedConnection.connectionFromSeq(
        curators.map(new GraphQLCurator(_)),
        ConnectionArgs(before, after, first, last),
      ))
  }

  @GraphQLField
  @GraphQLDescription("Whether this deposit is a new version.")
  def isNewVersion(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[Boolean]] = {
    CurationResolver.isNewVersion(deposit.id)
  }

  @GraphQLField
  @GraphQLDescription("List the present and past values for 'is-new-version'.")
  def isNewVersionEvents(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLIsNewVersion]] = {
    CurationResolver.allIsNewVersionEvents(deposit.id)
      .map(_.sortBy(_.timestamp).map(new GraphQLIsNewVersion(_)))
  }

  @GraphQLField
  @GraphQLDescription("Whether this deposit requires curation.")
  def curationRequired(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[Boolean]] = {
    CurationResolver.isCurationRequired(deposit.id)
  }

  @GraphQLField
  @GraphQLDescription("List the present and past values for 'curation-required'.")
  def curationRequiredEvents(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLCurationRequired]] = {
    CurationResolver.allIsCurationRequiredEvents(deposit.id)
      .map(_.sortBy(_.timestamp).map(new GraphQLCurationRequired(_)))
  }

  @GraphQLField
  @GraphQLDescription("Whether curation on this deposit has been performed.")
  def curationPerformed(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[Boolean]] = {
    CurationResolver.isCurationPerformed(deposit.id)
  }

  @GraphQLField
  @GraphQLDescription("List the present and past values for 'curation-performed'.")
  def curationPerformedEvents(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLCurationPerformed]] = {
    CurationResolver.allIsCurationPerformedEvents(deposit.id)
      .map(_.sortBy(_.timestamp).map(new GraphQLCurationPerformed(_)))
  }

  @GraphQLField
  @GraphQLDescription("The springfield configuration currently associated with this deposit.")
  def springfield(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLSpringfield]] = {
    SpringfieldResolver.currentById(deposit.id)
      .map(_.map(new GraphQLSpringfield(_)))
  }

  @GraphQLField
  @GraphQLDescription("List the present and past values for springfield configuration.")
  def springfields(@GraphQLDescription("Ordering options for the returned springfields.") orderBy: Option[SpringfieldOrder],
                   @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
                   @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
                   @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
                  )(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLSpringfield]] = {
    SpringfieldResolver.allById(deposit.id)
      .map(TimebasedSearch(earlierThan, laterThan, atTimestamp, orderBy))
      .map(_.map(new GraphQLSpringfield(_)))
  }

  @GraphQLField
  @GraphQLDescription("The content type currently associated with this deposit.")
  def contentType(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLContentType]] = {
    ContentTypeResolver.currentById(deposit.id)
      .map(_.map(new GraphQLContentType(_)))
  }

  @GraphQLField
  @GraphQLDescription("List the present and past values of content types.")
  def contentTypes(@GraphQLDescription("Ordering options for the returned content types.") orderBy: Option[ContentTypeOrder],
                   @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
                   @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
                   @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
                  )(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLContentType]] = {
    ContentTypeResolver.allById(deposit.id)
      .map(TimebasedSearch(earlierThan, laterThan, atTimestamp, orderBy))
      .map(_.map(new GraphQLContentType(_)))
  }
}
