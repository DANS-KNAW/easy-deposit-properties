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
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositorId }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import sangria.relay.{ Connection, ConnectionArgs }
import sangria.schema.{ Context, DeferredValue, Field, ObjectType, OptionType, StringType, fields }

import scala.concurrent.ExecutionContext.Implicits.global

trait DepositorType {
  this: DepositType
    with StateType
    with IngestStepType
    with DoiEventTypes
    with CuratorType
    with CurationEventType
    with ContentTypeGraphQLType
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
    ) ++ Connection.Args.All,
    fieldType = OptionType(depositConnectionType),
    resolve = ctx => getDeposits(ctx).map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(ctx))),
  )

  private def getDeposits(context: Context[DataContext, DepositorId]): DeferredValue[DataContext, Seq[Deposit]] = {
    val depositorId = context.value
    val stateInput = context.arg(depositStateFilterArgument)
    val ingestStepInput = context.arg(depositIngestStepFilterArgument)
    val doiRegistered = context.arg(depositDoiRegisteredFilterArgument)
    val doiAction = context.arg(depositDoiActionFilterArgument)
    val curator = context.arg(depositCuratorFilterArgument)
    val isNewVersion = context.arg(depositIsNewVersionFilterArgument)
    val curationRequired = context.arg(depositCurationRequiredFilterArgument)
    val curationPerformed = context.arg(depositCurationPerformedFilterArgument)
    val contentType = context.arg(depositContentTypeFilterArgument)
    val orderBy = context.arg(optDepositOrderArgument)

    DeferredValue(depositsFetcher.defer(DepositFilters(
      depositorId = Some(depositorId),
      stateFilter = stateInput,
      ingestStepFilter = ingestStepInput,
      doiRegisteredFilter = doiRegistered,
      doiActionFilter = doiAction,
      curatorFilter = curator,
      isNewVersionFilter = isNewVersion,
      curationRequiredFilter = curationRequired,
      curationPerformedFilter = curationPerformed,
      contentTypeFilter = contentType,
    ))).map { case (_, deposits) => orderBy.fold(deposits)(order => deposits.sorted(order.ordering)) }
  }

  implicit val DepositorType: ObjectType[DataContext, DepositorId] = ObjectType(
    name = "Depositor",
    description = "Information about the depositor that submitted this deposit.",
    fields = fields[DataContext, DepositorId](
      depositorIdField,
      depositsField,
    ),
  )
}
