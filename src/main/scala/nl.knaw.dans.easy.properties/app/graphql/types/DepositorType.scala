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
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ DepositResolver, executionContext }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositorId }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import sangria.relay.{ Connection, ConnectionArgs, ConnectionDefinition }
import sangria.schema.{ Context, DeferredValue, Field, ObjectType, OptionType, StringType, fields }

trait DepositorType {
  this: DepositType
    with StateType
    with IngestStepType
    with DoiEventTypes
    with CuratorType
    with CurationEventType
    with ContentTypeGraphQLType
    with TimebasedSearch
    with MetaTypes =>

  private val depositorIdField: Field[DataContext, DepositorId] = Field(
    name = "depositorId",
    description = Some("The EASY account of the depositor."),
    fieldType = StringType,
    resolve = ctx => ctx.value,
  )
  private val depositsField: Field[DataContext, DepositorId] = Field(
    name = "deposits",
    description = Some("List all deposits originating from the same depositor."),
    arguments = List(
      depositBagNameFilterArgument,
      depositOriginFilterArgument,
      depositStateFilterArgument,
      depositIngestStepFilterArgument,
      depositDoiRegisteredFilterArgument,
      depositDoiActionFilterArgument,
      depositIsNewVersionFilterArgument,
      depositCurationRequiredFilterArgument,
      depositCurationPerformedFilterArgument,
      depositCuratorFilterArgument,
      depositContentTypeFilterArgument,
      optDepositOrderArgument,
    ) ::: timebasedSearchArguments ::: Connection.Args.All,
    fieldType = OptionType(depositConnectionType),
    resolve = getDeposits(_),
  )

  private def getDeposits(implicit context: Context[DataContext, DepositorId]): DeferredValue[DataContext, ExtendedConnection[Deposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      depositorId = Some(context.value),
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

  implicit val DepositorType: ObjectType[DataContext, DepositorId] = ObjectType(
    name = "Depositor",
    description = "Information about the depositor that submitted this deposit.",
    fields = fields[DataContext, DepositorId](
      depositorIdField,
      depositsField,
    ),
  )

  val ConnectionDefinition(_, depositorConnectionType) = ExtendedConnection.definition[DataContext, ExtendedConnection, DepositorId](
    name = "DepositorId",
    nodeType = DepositorType,
  )
}
