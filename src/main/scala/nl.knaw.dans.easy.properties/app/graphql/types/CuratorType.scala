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
import nl.knaw.dans.easy.properties.app.graphql.ordering.{ CuratorOrder, CuratorOrderField, OrderDirection }
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ CurationResolver, DepositResolver, executionContext }
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.curator.{ Curator, DepositCuratorFilter }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.marshalling.FromInput._
import sangria.relay._
import sangria.schema.{ Argument, Context, DeferredValue, EnumType, Field, InputObjectType, ObjectType, OptionInputType, OptionType }

trait CuratorType {
  this: DepositType
    with StateType
    with IngestStepType
    with DoiEventTypes
    with CurationEventType
    with ContentTypeGraphQLType
    with TimebasedSearch
    with NodeType
    with MetaTypes
    with Scalars =>

  implicit val DepositCuratorFilterType: InputObjectType[DepositCuratorFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The label and filter to be used in searching for deposits by curator."),
    DocumentInputField("curator", "If provided, only show deposits with this curator."),
    DocumentInputField("filter", "Determine whether to search in current curator (`LATEST`, default) only or all current and past curators (`ALL`) of this deposit."),
    RenameInputField("curator", "userId"),
  )
  implicit val DepositCuratorFilterFromInput: FromInput[DepositCuratorFilter] = fromInput(ad => DepositCuratorFilter(
    curator = ad("userId").asInstanceOf[String],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))

  private val seriesFilterArgument: Argument[SeriesFilter] = Argument(
    name = "curatorFilter",
    argumentType = SeriesFilterType,
    description = Some("Determine whether to search in current curators (`LATEST`, default) only or all current and past curators (`ALL`) of this deposit."),
    defaultValue = Some(SeriesFilter.LATEST -> SeriesFilterToInput),
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val depositCuratorFilterArgument: Argument[Option[DepositCuratorFilter]] = {
    Argument(
      name = "curator",
      argumentType = OptionInputType(DepositCuratorFilterType),
      description = Some("List only those deposits that have this specified data manager."),
      defaultValue = None,
      fromInput = optionInput(inputObjectResultInput(DepositCuratorFilterFromInput)),
      astDirectives = Vector.empty,
      astNodes = Vector.empty,
    )
  }

  private val depositField: Field[DataContext, Curator] = Field(
    name = "deposit",
    description = Some("Returns the deposit that is associated with this particular curator object."),
    fieldType = OptionType(DepositType),
    resolve = getDepositByCurator(_),
  )
  private val depositsField: Field[DataContext, Curator] = Field(
    name = "deposits",
    description = Some("List all deposits with the same data manager."),
    arguments = List(
      seriesFilterArgument,
      depositBagNameFilterArgument,
      depositOriginFilterArgument,
      depositStateFilterArgument,
      depositIngestStepFilterArgument,
      depositDoiRegisteredFilterArgument,
      depositDoiActionFilterArgument,
      depositIsNewVersionFilterArgument,
      depositCurationRequiredFilterArgument,
      depositCurationPerformedFilterArgument,
      depositContentTypeFilterArgument,
      optDepositOrderArgument,
    ) ::: timebasedSearchArguments ::: Connection.Args.All,
    fieldType = OptionType(depositConnectionType),
    resolve = getDeposits(_),
  )

  private def getDepositByCurator(implicit context: Context[DataContext, Curator]): DeferredValue[DataContext, Option[Deposit]] = {
    CurationResolver.depositByCurationId(context.value.id)
  }

  private def getDeposits(implicit context: Context[DataContext, Curator]): DeferredValue[DataContext, ExtendedConnection[Deposit]] = {
    val label = context.value.userId
    val curatorFilter = context.arg(seriesFilterArgument)

    DepositResolver.findDeposit(DepositFilters(
      bagName = context.arg(depositBagNameFilterArgument),
      originFilter = context.arg(depositOriginFilterArgument),
      stateFilter = context.arg(depositStateFilterArgument),
      ingestStepFilter = context.arg(depositIngestStepFilterArgument),
      doiRegisteredFilter = context.arg(depositDoiRegisteredFilterArgument),
      doiActionFilter = context.arg(depositDoiActionFilterArgument),
      curatorFilter = Some(DepositCuratorFilter(label, curatorFilter)),
      isNewVersionFilter = context.arg(depositIsNewVersionFilterArgument),
      curationRequiredFilter = context.arg(depositCurationRequiredFilterArgument),
      curationPerformedFilter = context.arg(depositCurationPerformedFilterArgument),
      contentTypeFilter = context.arg(depositContentTypeFilterArgument),
    )).map(timebasedFilterAndSort(optDepositOrderArgument))
      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  implicit val CuratorType: ObjectType[DataContext, Curator] = deriveObjectType(
    ObjectTypeDescription("Data manager responsible for curating this deposit."),
    Interfaces[DataContext, Curator](nodeInterface),
    DocumentField("userId", "The data manager's username in EASY."),
    DocumentField("email", "The data manager's email address."),
    DocumentField("timestamp", "The timestamp at which the data manager was assigned to this deposit."),
    AddFields(
      depositField,
      depositsField,
    ),
    ReplaceField("id", Node.globalIdField[DataContext, Curator]),
  )

  val ConnectionDefinition(_, curatorConnectionType) = ExtendedConnection.definition[DataContext, ExtendedConnection, Curator](
    name = "Curator",
    nodeType = CuratorType,
  )

  implicit val CuratorOrderFieldType: EnumType[CuratorOrderField.Value] = deriveEnumType()
  implicit val CuratorOrderInputType: InputObjectType[CuratorOrder] = deriveInputObjectType(
    InputObjectTypeDescription("Ordering options for curators"),
    DocumentInputField("field", "The field to order curators by"),
    DocumentInputField("direction", "The ordering direction"),
  )
  implicit val CuratorOrderFromInput: FromInput[CuratorOrder] = fromInput(ad => CuratorOrder(
    field = ad("field").asInstanceOf[CuratorOrderField.Value],
    direction = ad("direction").asInstanceOf[OrderDirection.Value],
  ))
  val optCuratorOrderArgument: Argument[Option[CuratorOrder]] = {
    Argument(
      name = "orderBy",
      argumentType = OptionInputType(CuratorOrderInputType),
      description = Some("Ordering options for the returned curators."),
      defaultValue = None,
      fromInput = optionInput(inputObjectResultInput(CuratorOrderFromInput)),
      astDirectives = Vector.empty,
      astNodes = Vector.empty,
    )
  }
}
