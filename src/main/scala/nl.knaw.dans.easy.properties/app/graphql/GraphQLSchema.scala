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
package nl.knaw.dans.easy.properties.app.graphql

import nl.knaw.dans.easy.properties.app.graphql.resolvers._
import nl.knaw.dans.easy.properties.app.graphql.model.Scalars
import nl.knaw.dans.easy.properties.app.graphql.typedefinitions._
import sangria.execution.deferred.DeferredResolver
import sangria.macros.derive._
import sangria.schema._

object GraphQLSchema extends Scalars
  with GraphQLCommonTypes
  with GraphQLContentTypeType
  with GraphQLCurationPerformedType
  with GraphQLCurationRequiredType
  with GraphQLCuratorType
  with GraphQLDepositType
  with GraphQLDoiActionType
  with GraphQLDoiRegisteredType
  with GraphQLIdentifierType
  with GraphQLIngestStepType
  with GraphQLIsNewVersionType
  with GraphQLMutationType
  with GraphQLQueryType
  with GraphQLSpringfieldType
  with GraphQLStateType {

  implicit val QueryType: ObjectType[DataContext, Unit] = deriveContextObjectType(
    _.query,
    AddFields(nodeField, nodesField),
  )
  implicit val MutationType: ObjectType[DataContext, Unit] = deriveContextObjectType(_.mutation)

  val schema: Schema[DataContext, Unit] = Schema[DataContext, Unit](QueryType, mutation = Option(MutationType))
  val deferredResolver: DeferredResolver[DataContext] = DeferredResolver.fetchers(
    DepositResolver.byIdFetcher, DepositResolver.depositsFetcher, DepositResolver.lastModifiedFetcher, DepositResolver.depositorFetcher,
    StateResolver.byIdFetcher, StateResolver.currentStatesFetcher, StateResolver.allStatesFetcher, StateResolver.depositByStateIdFetcher,
    IngestStepResolver.byIdFetcher, IngestStepResolver.currentIngestStepsFetcher, IngestStepResolver.allIngestStepsFetcher, IngestStepResolver.depositByIngestStepIdFetcher,
    IdentifierResolver.byIdFetcher, IdentifierResolver.identifiersByTypeFetcher, IdentifierResolver.identifierTypesAndValuesFetcher, IdentifierResolver.identifiersByDepositIdFetcher, IdentifierResolver.depositByIdentifierIdFetcher,
    DoiEventResolver.currentDoisRegisteredFetcher, DoiEventResolver.allDoisRegisteredFetcher,
    DoiEventResolver.currentDoisActionFetcher, DoiEventResolver.allDoisActionFetcher,
    CuratorResolver.byIdFetcher, CuratorResolver.currentCuratorsFetcher, CuratorResolver.allCuratorsFetcher, CuratorResolver.depositByCuratorIdFetcher,
    IsNewVersionResolver.byIdFetcher, IsNewVersionResolver.currentIsNewVersionsFetcher, IsNewVersionResolver.allIsNewVersionsFetcher, IsNewVersionResolver.depositByIsNewVersionIdFetcher,
    IsCurationRequiredResolver.byIdFetcher, IsCurationRequiredResolver.currentIsCurationRequiredsFetcher, IsCurationRequiredResolver.allIsCurationRequiredsFetcher, IsCurationRequiredResolver.depositByIsCurationRequiredIdFetcher,
    IsCurationPerformedResolver.byIdFetcher, IsCurationPerformedResolver.currentIsCurationPerformedsFetcher, IsCurationPerformedResolver.allIsCurationPerformedsFetcher, IsCurationPerformedResolver.depositByIsCurationPerformedIdFetcher,
    SpringfieldResolver.byIdFetcher, SpringfieldResolver.currentSpringfieldsFetcher, SpringfieldResolver.allSpringfieldsFetcher, SpringfieldResolver.depositBySpringfieldIdFetcher,
    ContentTypeResolver.byIdFetcher, ContentTypeResolver.currentContentTypesFetcher, ContentTypeResolver.allContentTypesFetcher, ContentTypeResolver.depositByContentTypeIdFetcher,
  )
}
