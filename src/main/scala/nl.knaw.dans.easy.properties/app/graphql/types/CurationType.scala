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
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ CurationResolver, DepositResolver, executionContext }
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.curation.Curation
import nl.knaw.dans.easy.properties.app.model.curator.DepositCuratorFilter
import nl.knaw.dans.easy.properties.app.model.{ Deposit, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import sangria.macros.derive._
import sangria.marshalling.FromInput._
import sangria.relay.{ Connection, ConnectionArgs, Node }
import sangria.schema.{ Argument, Context, DeferredValue, Field, ObjectType, OptionType }

trait CurationType {
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

  private val seriesFilterArgument: Argument[SeriesFilter] = Argument(
    name = "curatorFilter",
    argumentType = SeriesFilterType,
    description = Some("Determine whether to search in current curators (`LATEST`, default) only or all current and past curators (`ALL`) of this deposit."),
    defaultValue = Some(SeriesFilter.LATEST -> SeriesFilterToInput),
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )

  private val depositField: Field[DataContext, Curation] = Field(
    name = "deposit",
    description = Some("Returns the deposit that is associated with this particular curation object."),
    fieldType = OptionType(DepositType),
    resolve = getDepositByCuration(_),
  )
  private val depositsField: Field[DataContext, Curation] = Field(
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

  private def getDepositByCuration(implicit context: Context[DataContext, Curation]): DeferredValue[DataContext, Option[Deposit]] = {
    CurationResolver.depositByCurationId(context.value.id)
  }

  private def getDeposits(implicit context: Context[DataContext, Curation]): DeferredValue[DataContext, ExtendedConnection[Deposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      bagName = context.arg(depositBagNameFilterArgument),
      originFilter = context.arg(depositOriginFilterArgument),
      stateFilter = context.arg(depositStateFilterArgument),
      ingestStepFilter = context.arg(depositIngestStepFilterArgument),
      doiRegisteredFilter = context.arg(depositDoiRegisteredFilterArgument),
      doiActionFilter = context.arg(depositDoiActionFilterArgument),
      curatorFilter = Some(DepositCuratorFilter(context.value.datamanagerUserId, context.arg(seriesFilterArgument))),
      isNewVersionFilter = context.arg(depositIsNewVersionFilterArgument),
      curationRequiredFilter = context.arg(depositCurationRequiredFilterArgument),
      curationPerformedFilter = context.arg(depositCurationPerformedFilterArgument),
      contentTypeFilter = context.arg(depositContentTypeFilterArgument),
    )).map(timebasedFilterAndSort(optDepositOrderArgument))
      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(context)))
  }

  implicit val CurationType: ObjectType[DataContext, Curation] = deriveObjectType(
    ObjectTypeDescription("Curation event containing the data manager and status of curation for this deposit."),
    Interfaces[DataContext, Curation](nodeInterface),
    DocumentField("datamanagerUserId", "The data manager's username in EASY."),
    DocumentField("datamanagerEmail", "The data manager's email address."),
    DocumentField("isNewVersion", "True if the deposit is a new version."),
    DocumentField("isRequired", "True if curation by a data manager is required."),
    DocumentField("isPerformed", "True if curation by the data manager has been performed."),
    DocumentField("timestamp", "The timestamp at which this curation event was assigned to this deposit."),
    AddFields(
      depositField,
      depositsField,
    ),
    ReplaceField("id", Node.globalIdField[DataContext, Curation]),
    RenameField("isRequired", "curationRequired"),
    RenameField("isPerformed", "curationPerformed"),
  )
}
