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
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ ContentTypeResolver, CurationResolver, DepositResolver, DoiEventResolver, IdentifierResolver, IngestStepResolver, SpringfieldResolver, StateResolver, executionContext }
import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.contentType.ContentType
import nl.knaw.dans.easy.properties.app.model.curator.Curator
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType }
import nl.knaw.dans.easy.properties.app.model.ingestStep.IngestStep
import nl.knaw.dans.easy.properties.app.model.springfield.Springfield
import nl.knaw.dans.easy.properties.app.model.state.State
import nl.knaw.dans.easy.properties.app.model.{ CurationPerformedEvent, CurationRequiredEvent, Deposit, DepositorId, DoiActionEvent, DoiRegisteredEvent, IsNewVersionEvent, Origin, Timestamp, timestampOrdering }
import sangria.macros.derive._
import sangria.marshalling.FromInput.coercedScalaInput
import sangria.relay._
import sangria.schema.{ Argument, BooleanType, Context, DeferredValue, EnumType, Field, ListType, ObjectType, OptionInputType, OptionType, StringType }

trait DepositType {
  this: DepositorType
    with StateType
    with IngestStepType
    with IdentifierGraphQLType
    with DoiEventTypes
    with CuratorType
    with CurationEventType
    with CurationType
    with SpringfieldType
    with ContentTypeGraphQLType
    with TimebasedSearch
    with NodeType
    with MetaTypes
    with Scalars =>

  private val identifierTypeArgument: Argument[IdentifierType.Value] = Argument(
    name = "type",
    argumentType = IdentifierTypeType,
    description = Some("Find the identifier with this specific type."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  // lazy because we need it before being declared
  lazy val depositBagNameFilterArgument: Argument[Option[String]] = Argument(
    name = "bagName",
    argumentType = OptionInputType(StringType),
    description = Some("Find only those deposits that have this specified bag name."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val bagNameField: Field[DataContext, Deposit] = Field(
    name = "bagName",
    fieldType = OptionType(StringType),
    description = Option("The name of the deposited bag."),
    resolve = _.value.bagName,
  )

  implicit lazy val OriginType: EnumType[Origin.Value] = deriveEnumType(
    EnumTypeDescription("The origin of the deposit."),
    DocumentValue("SWORD2", "The DANS-DOI must be created in the DataCite resolver."),
    DocumentValue("API", "The DANS-DOI must be updated in the DataCite resolver."),
    DocumentValue("SMD", "None action must be taken for this DANS-DOI in the DataCite resolver."),
  )
  lazy val depositOriginFilterArgument: Argument[Option[Origin]] = Argument(
    name = "origin",
    argumentType = OptionInputType(OriginType),
    description = Some("Find only those deposits that have this specified origin."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val originField: Field[DataContext, Deposit] = Field(
    name = "origin",
    fieldType = OptionType(OriginType),
    description = Option("The origin of the deposit."),
    resolve = _.value.origin,
  )

  private val lastModifiedField: Field[DataContext, Deposit] = Field(
    name = "lastModified",
    fieldType = OptionType(DateTimeType),
    description = Option("Get the timestamp at which this deposit was last modified. If the dataset was only created, the creation timestamp is returned."),
    resolve = getLastModified(_),
  )
  private val stateField: Field[DataContext, Deposit] = Field(
    name = "state",
    fieldType = OptionType(StateType),
    description = Option("The current state of the deposit."),
    resolve = getCurrentState(_),
  )
  private val statesField: Field[DataContext, Deposit] = Field(
    name = "states",
    description = Option("List all states of the deposit."),
    arguments = optStateOrderArgument :: timebasedSearchArguments ::: Connection.Args.All,
    fieldType = OptionType(stateConnectionType),
    resolve = getAllStates(_),
  )
  private val ingestStepField: Field[DataContext, Deposit] = Field(
    name = "ingestStep",
    fieldType = OptionType(IngestStepType),
    description = Option("The current ingest step of the deposit."),
    resolve = getCurrentIngestStep(_),
  )
  private val ingestStepsField: Field[DataContext, Deposit] = Field(
    name = "ingestSteps",
    description = Option("List all ingest steps of the deposit."),
    arguments = optIngestStepOrderArgument :: timebasedSearchArguments ::: Connection.Args.All,
    fieldType = OptionType(ingestStepConnectionType),
    resolve = getAllIngestSteps(_),
  )
  private val depositorField: Field[DataContext, Deposit] = Field(
    name = "depositor",
    fieldType = DepositorType,
    description = Option("Information about the depositor that submitted this deposit."),
    resolve = getDepositor,
  )
  private val identifierField: Field[DataContext, Deposit] = Field(
    name = "identifier",
    description = Some("Return the identifier of the given type related to this deposit"),
    arguments = List(identifierTypeArgument),
    fieldType = OptionType(IdentifierObjectType),
    resolve = getIdentifier(_),
  )
  private val identifiersField: Field[DataContext, Deposit] = Field(
    name = "identifiers",
    description = Some("List the identifiers related to this deposit"),
    fieldType = ListType(IdentifierObjectType),
    resolve = getIdentifiers(_),
  )
  private val doiRegisteredField: Field[DataContext, Deposit] = Field(
    name = "doiRegistered",
    description = Some("Returns whether the DOI is registered in DataCite."),
    fieldType = OptionType(BooleanType),
    resolve = getDoiRegistered(_),
  )
  private val doiRegisteredEventsField: Field[DataContext, Deposit] = Field(
    name = "doiRegisteredEvents",
    description = Some("Lists all state changes related to the registration of the DOI in DataCite"),
    fieldType = ListType(DoiRegisteredEventType),
    resolve = getDoiRegisteredEvents(_),
  )
  private val doiActionField: Field[DataContext, Deposit] = Field(
    name = "doiAction",
    description = Some("Returns whether the DOI should be 'created' or 'updated' on registration in DataCite"),
    fieldType = OptionType(DoiActionType),
    resolve = getDoiAction(_),
  )
  private val doiActionEventsField: Field[DataContext, Deposit] = Field(
    name = "doiActionEvents",
    description = Some("Lists all state changes related to whether the DOI should be 'created' or 'updated' on registration in DataCite"),
    fieldType = ListType(DoiActionEventType),
    resolve = getDoiActionEvents(_),
  )
  private val curatorField: Field[DataContext, Deposit] = Field(
    name = "curator",
    description = Some("The data manager currently assigned to this deposit"),
    fieldType = OptionType(CuratorType),
    resolve = getCurrentCurator(_),
  )
  private val curatorsField: Field[DataContext, Deposit] = Field(
    name = "curators",
    description = Some("List all data manager that were ever assigned to this deposit."),
    arguments = optCuratorOrderArgument :: timebasedSearchArguments ::: Connection.Args.All,
    fieldType = OptionType(curatorConnectionType),
    resolve = getAllCurators(_),
  )
  private val isNewVersionField: Field[DataContext, Deposit] = Field(
    name = "isNewVersion",
    description = Some("Whether this deposit is a new version."),
    fieldType = OptionType(BooleanType),
    resolve = getIsNewVersion(_),
  )
  private val isNewVersionEventsField: Field[DataContext, Deposit] = Field(
    name = "isNewVersionEvents",
    description = Some("List the present and past values for 'is-new-version'."),
    fieldType = ListType(IsNewVersionEventType),
    resolve = getIsNewVersionEvents(_),
  )
  private val curationRequiredField: Field[DataContext, Deposit] = Field(
    name = "curationRequired",
    description = Some("Whether this deposit requires curation."),
    fieldType = OptionType(BooleanType),
    resolve = getCurationRequired(_),
  )
  private val curationRequiredEventsField: Field[DataContext, Deposit] = Field(
    name = "curationRequiredEvents",
    description = Some("List the present and past values for 'curation-required'."),
    fieldType = ListType(CurationRequiredEventType),
    resolve = getCurationRequiredEvents(_),
  )
  private val curationPerformedField: Field[DataContext, Deposit] = Field(
    name = "curationPerformed",
    description = Some("Whether curation on this deposit has been performed."),
    fieldType = OptionType(BooleanType),
    resolve = getCurationPerformed(_),
  )
  private val curationPerformedEventsField: Field[DataContext, Deposit] = Field(
    name = "curationPerformedEvents",
    description = Some("List the present and past values for 'curation-performed'."),
    fieldType = ListType(CurationPerformedEventType),
    resolve = getCurationPerformedEvents(_),
  )
  private val springfieldField: Field[DataContext, Deposit] = Field(
    name = "springfield",
    description = Some("The springfield configuration currently associated with this deposit."),
    fieldType = OptionType(SpringfieldType),
    resolve = getSpringfield(_),
  )
  private val springfieldsField: Field[DataContext, Deposit] = Field(
    name = "springfields",
    description = Some("List the present and past values for springfield configuration."),
    arguments = optSpringfieldOrderArgument :: timebasedSearchArguments,
    fieldType = ListType(SpringfieldType),
    resolve = getSpringfields(_),
  )
  private val contentTypeField: Field[DataContext, Deposit] = Field(
    name = "contentType",
    description = Some("The content type currently associated with this deposit."),
    fieldType = OptionType(ContentTypeType),
    resolve = getContentType(_),
  )
  private val contentTypesField: Field[DataContext, Deposit] = Field(
    name = "contentTypes",
    description = Some("List the present and past values of content types."),
    arguments = optContentTypeOrderArgument :: timebasedSearchArguments,
    fieldType = ListType(ContentTypeType),
    resolve = getContentTypes(_),
  )

  private def getLastModified(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Timestamp]] = {
    DepositResolver.lastModified(context.value.id)
  }

  private def getCurrentState(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[State]] = {
    StateResolver.currentById(context.value.id)
  }

  private def getAllStates(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, ExtendedConnection[State]] = {
    StateResolver.allById(context.value.id)(context.ctx)
      .map(timebasedFilterAndSort(optStateOrderArgument))
      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  private def getCurrentIngestStep(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[IngestStep]] = {
    IngestStepResolver.currentById(context.value.id)
  }

  private def getAllIngestSteps(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, ExtendedConnection[IngestStep]] = {
    IngestStepResolver.allById(context.value.id)
      .map(timebasedFilterAndSort(optIngestStepOrderArgument))
      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  private def getDepositor(context: Context[DataContext, Deposit]): DepositorId = {
    context.value.depositorId
  }

  private def getIdentifier(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Identifier]] = {
    IdentifierResolver.identifierByType(context.value.id, context.arg(identifierTypeArgument))
  }

  private def getIdentifiers(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[Identifier]] = {
    IdentifierResolver.allById(context.value.id)
  }

  private def getDoiRegistered(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Boolean]] = {
    DoiEventResolver.isDoiRegistered(context.value.id)
  }

  private def getDoiRegisteredEvents(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[DoiRegisteredEvent]] = {
    DoiEventResolver.allDoiRegisteredById(context.value.id)
      .map(_.sortBy(_.timestamp))
  }

  private def getDoiAction(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[DoiAction]] = {
    DoiEventResolver.currentDoiActionById(context.value.id)
  }

  private def getDoiActionEvents(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[DoiActionEvent]] = {
    DoiEventResolver.allDoiActionsById(context.value.id)
      .map(_.sortBy(_.timestamp))
  }

  private def getCurrentCurator(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Curator]] = {
    CurationResolver.currentCuratorsById(context.value.id)
  }

  private def getAllCurators(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, ExtendedConnection[Curator]] = {
    CurationResolver.allCuratorsById(context.value.id)
      .map(timebasedFilterAndSort(optCuratorOrderArgument))
      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  private def getIsNewVersion(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Boolean]] = {
    CurationResolver.isNewVersion(context.value.id)
  }

  private def getIsNewVersionEvents(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[IsNewVersionEvent]] = {
    CurationResolver.allIsNewVersionEvents(context.value.id).map(_.sortBy(_.timestamp))
  }

  private def getCurationRequired(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Boolean]] = {
    CurationResolver.isCurationRequired(context.value.id)
  }

  private def getCurationRequiredEvents(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[CurationRequiredEvent]] = {
    CurationResolver.allIsCurationRequiredEvents(context.value.id).map(_.sortBy(_.timestamp))
  }

  private def getCurationPerformed(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Boolean]] = {
    CurationResolver.isCurationPerformed(context.value.id)
  }

  private def getCurationPerformedEvents(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[CurationPerformedEvent]] = {
    CurationResolver.allIsCurationPerformedEvents(context.value.id).map(_.sortBy(_.timestamp))
  }

  private def getSpringfield(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[Springfield]] = {
    SpringfieldResolver.currentById(context.value.id)
  }

  private def getSpringfields(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[Springfield]] = {
    SpringfieldResolver.allById(context.value.id)
      .map(timebasedFilterAndSort(optSpringfieldOrderArgument))
  }

  private def getContentType(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Option[ContentType]] = {
    ContentTypeResolver.currentById(context.value.id)
  }

  private def getContentTypes(implicit context: Context[DataContext, Deposit]): DeferredValue[DataContext, Seq[ContentType]] = {
    ContentTypeResolver.allById(context.value.id).map(timebasedFilterAndSort(optContentTypeOrderArgument))
  }

  implicit val depositIdentifiable: Identifiable[Deposit] = _.id.toString

  // lazy because we need it before being declared
  implicit lazy val DepositType: ObjectType[DataContext, Deposit] = deriveObjectType(
    ObjectTypeDescription("Contains all technical metadata about this deposit."),
    Interfaces[DataContext, Deposit](nodeInterface),
    RenameField("id", "depositId"),
    DocumentField("id", "The identifier of the deposit."),
    DocumentField("bagName", "The name of the deposited bag."),
    DocumentField("creationTimestamp", "The moment this deposit was created."),
    AddFields(
      Node.globalIdField[DataContext, Deposit],
      stateField,
      statesField,
      lastModifiedField,
      ingestStepField,
      ingestStepsField,
      identifierField,
      identifiersField,
      doiRegisteredField,
      doiRegisteredEventsField,
      doiActionField,
      doiActionEventsField,
      curatorField,
      curatorsField,
      isNewVersionField,
      isNewVersionEventsField,
      curationRequiredField,
      curationRequiredEventsField,
      curationPerformedField,
      curationPerformedEventsField,
      springfieldField,
      springfieldsField,
      contentTypeField,
      contentTypesField,
    ),
    ReplaceField("bagName", bagNameField),
    ReplaceField("origin", originField),
    ReplaceField("depositorId", depositorField),
  )

  // lazy because we need it before being declared
  lazy val ConnectionDefinition(_, depositConnectionType) = ExtendedConnection.definition[DataContext, ExtendedConnection, Deposit](
    name = "Deposit",
    nodeType = DepositType,
  )
}
