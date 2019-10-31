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
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ DepositResolver, IdentifierResolver, executionContext }
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DepositorId }
import nl.knaw.dans.easy.properties.app.repository.{ DepositFilters, DepositorIdFilters }
import sangria.marshalling.FromInput.coercedScalaInput
import sangria.relay.{ Connection, ConnectionArgs }
import sangria.schema.{ Argument, Context, DeferredValue, Field, ObjectType, OptionInputType, OptionType, StringType, fields }

trait QueryType {
  this: DepositType
    with DepositorType
    with StateType
    with IngestStepType
    with IdentifierGraphQLType
    with DoiEventTypes
    with CuratorType
    with CurationEventType
    with ContentTypeGraphQLType
    with TimebasedSearch
    with MetaTypes
    with NodeType
    with Scalars =>

  private val depositorIdArgument: Argument[Option[DepositorId]] = Argument(
    name = "id",
    description = Some("If provided, only show deposits from this depositor."),
    defaultValue = None,
    argumentType = OptionInputType(StringType),
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val depositIdArgument: Argument[DepositId] = Argument(
    name = "id",
    description = Some("The id for which to find the deposit"),
    defaultValue = None,
    argumentType = UUIDType,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val identifierTypeArgument: Argument[IdentifierType.Value] = Argument(
    name = "type",
    description = Some("The type of identifier to be found."),
    defaultValue = None,
    argumentType = IdentifierTypeType,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val identifierValueArgument: Argument[String] = Argument(
    name = "value",
    description = Some("The value of the identifier to be found."),
    defaultValue = None,
    argumentType = StringType,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )

  private val depositField: Field[DataContext, Unit] = Field(
    name = "deposit",
    description = Some("Get the technical metadata of the deposit identified by 'id'."),
    arguments = List(depositIdArgument),
    fieldType = OptionType(DepositType),
    resolve = getDeposit(_),
  )
  private val depositsField: Field[DataContext, Unit] = Field(
    name = "deposits",
    description = Some("List all registered deposits."),
    arguments = List(
      depositBagNameFilterArgument,
      depositOriginFilterArgument,
      depositStateFilterArgument,
      depositIngestStepFilterArgument,
      depositDoiRegisteredFilterArgument,
      depositDoiActionFilterArgument,
      depositCuratorFilterArgument,
      depositIsNewVersionFilterArgument,
      depositCurationRequiredFilterArgument,
      depositCurationPerformedFilterArgument,
      depositContentTypeFilterArgument,
      optDepositOrderArgument,
    ) ::: timebasedSearchArguments ::: Connection.Args.All,
    fieldType = OptionType(depositConnectionType),
    resolve = getDeposits(_),
  )
  private val depositorField: Field[DataContext, Unit] = Field(
    name = "depositor",
    description = Some("Get the technical metadata related to this depositor."),
    arguments = List(
      depositorIdArgument,
    ),
    fieldType = OptionType(DepositorType),
    resolve = getDepositor(_),
  )
  private val depositorsField: Field[DataContext, Unit] = Field(
    name = "depositors",
    description = Some("List all depositors."),
    arguments = List(
      depositOriginFilterArgument.copy(description = Some("Find only those depositors that have deposited data by this specific origin.")),
      depositStateFilterArgument.copy(description = Some("Find only those depositors that have deposits with this state")),
      depositIngestStepFilterArgument.copy(description = Some("Find only those depositors that have deposits with this ingest step.")),
      depositDoiRegisteredFilterArgument.copy(description = Some("Find only those depositors that have deposits with this registered value.")),
      depositDoiActionFilterArgument.copy(description = Some("Find only those depositors that have deposits with this action value.")),
      depositCuratorFilterArgument.copy(description = Some("Find only those depositors that have deposits with this curator.")),
      depositIsNewVersionFilterArgument.copy(description = Some("Find only those depositors that have deposits with this 'new version' value.")),
      depositCurationRequiredFilterArgument.copy(description = Some("Find only those depositors that have deposits with this 'curation required' value.")),
      depositCurationPerformedFilterArgument.copy(description = Some("Find only those depositors that have deposits with this 'curation performed' value.")),
      depositContentTypeFilterArgument.copy(description = Some("Find only those depositors that have deposits with this content type.")),
    ) ::: Connection.Args.All,
    fieldType = OptionType(depositorConnectionType),
    resolve = getDepositors(_),
  )
  private val identifierField: Field[DataContext, Unit] = Field(
    name = "identifier",
    description = Some("Find an identifier with the given type and value."),
    arguments = List(
      identifierTypeArgument,
      identifierValueArgument,
    ),
    fieldType = OptionType(IdentifierObjectType),
    resolve = getIdentifier(_),
  )

  private def getDeposit(implicit context: Context[DataContext, Unit]): DeferredValue[DataContext, Option[Deposit]] = {
    DepositResolver.depositById(context.arg(depositIdArgument))
  }

  private def getDeposits(implicit context: Context[DataContext, Unit]): DeferredValue[DataContext, ExtendedConnection[Deposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      bagName = context.arg(depositBagNameFilterArgument),
      originFilter = context.arg(depositOriginFilterArgument),
      stateFilter = context.arg(depositStateFilterArgument),
      ingestStepFilter = context.arg(depositIngestStepFilterArgument),
      doiRegisteredFilter = context.arg(depositDoiRegisteredFilterArgument),
      doiActionFilter = context.arg(depositDoiActionFilterArgument),
      curatorFilter = context.arg(depositCuratorFilterArgument),
      isNewVersionFilter = context.arg(depositIsNewVersionFilterArgument),
      curationRequiredFilter = context.arg(depositCurationRequiredFilterArgument),
      curationPerformedFilter = context.arg(depositCurationPerformedFilterArgument),
      contentTypeFilter = context.arg(depositContentTypeFilterArgument),
    )).map(timebasedFilterAndSort(optDepositOrderArgument))
      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  private def getDepositor(implicit context: Context[DataContext, Unit]): Option[DepositorId] = {
    context.arg(depositorIdArgument)
  }

  private def getDepositors(implicit context: Context[DataContext, Unit]): DeferredValue[DataContext, ExtendedConnection[DepositorId]] = {
    DepositResolver.listDepositors(DepositorIdFilters(
      originFilter = context.arg(depositOriginFilterArgument),
      stateFilter = context.arg(depositStateFilterArgument),
      ingestStepFilter = context.arg(depositIngestStepFilterArgument),
      doiRegisteredFilter = context.arg(depositDoiRegisteredFilterArgument),
      doiActionFilter = context.arg(depositDoiActionFilterArgument),
      curatorFilter = context.arg(depositCuratorFilterArgument),
      isNewVersionFilter = context.arg(depositIsNewVersionFilterArgument),
      curationRequiredFilter = context.arg(depositCurationRequiredFilterArgument),
      curationPerformedFilter = context.arg(depositCurationPerformedFilterArgument),
      contentTypeFilter = context.arg(depositContentTypeFilterArgument),
    )).map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  private def getIdentifier(implicit context: Context[DataContext, Unit]): DeferredValue[DataContext, Option[Identifier]] = {
    IdentifierResolver.identifierByTypeAndValue(
      idType = context.arg(identifierTypeArgument),
      idValue = context.arg(identifierValueArgument),
    )
  }

  implicit val QueryType: ObjectType[DataContext, Unit] = ObjectType(
    name = "Query",
    description = "The query root of easy-deposit-properties' GraphQL interface.",
    fields = fields[DataContext, Unit](
      depositField,
      depositsField,
      depositorField,
      depositorsField,
      identifierField,
      nodeField,
      nodesField,
    ),
  )
}
