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

import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ ContentTypeResolver, CurationResolver, DepositResolver, DoiEventResolver, IdentifierResolver, IngestStepResolver, SpringfieldResolver, StateResolver }
import nl.knaw.dans.easy.properties.app.graphql.types._
import sangria.execution.deferred.DeferredResolver
import sangria.schema._

object GraphQLSchema extends Scalars
  with NodeType
  with MetaTypes
  with TimebasedSearch
  with ContentTypeGraphQLType
  with SpringfieldType
  with CurationEventType
  with DoiEventTypes
  with IdentifierGraphQLType
  with IngestStepType
  with StateType
  with CurationType
  with CuratorType
  with DepositorType
  with DepositType
  with QueryType
  with MutationType {

  val schema: Schema[DataContext, Unit] = Schema[DataContext, Unit](QueryType, mutation = Option(MutationType))
  val deferredResolver: DeferredResolver[DataContext] = DeferredResolver.fetchers(
    DepositResolver.byIdFetcher, DepositResolver.depositsFetcher, DepositResolver.lastModifiedFetcher, DepositResolver.depositorFetcher,
    StateResolver.byIdFetcher, StateResolver.currentStatesFetcher, StateResolver.allStatesFetcher, StateResolver.depositByStateIdFetcher,
    IngestStepResolver.byIdFetcher, IngestStepResolver.currentIngestStepsFetcher, IngestStepResolver.allIngestStepsFetcher, IngestStepResolver.depositByIngestStepIdFetcher,
    IdentifierResolver.byIdFetcher, IdentifierResolver.identifiersByTypeFetcher, IdentifierResolver.identifierTypesAndValuesFetcher, IdentifierResolver.identifiersByDepositIdFetcher, IdentifierResolver.depositByIdentifierIdFetcher,
    DoiEventResolver.currentDoisRegisteredFetcher, DoiEventResolver.allDoisRegisteredFetcher,
    DoiEventResolver.currentDoisActionFetcher, DoiEventResolver.allDoisActionFetcher,
    CurationResolver.byIdFetcher, CurationResolver.currentCurationsFetcher, CurationResolver.allCurationsFetcher, CurationResolver.depositByCurationIdFetcher,
    SpringfieldResolver.byIdFetcher, SpringfieldResolver.currentSpringfieldsFetcher, SpringfieldResolver.allSpringfieldsFetcher, SpringfieldResolver.depositBySpringfieldIdFetcher,
    ContentTypeResolver.byIdFetcher, ContentTypeResolver.currentContentTypesFetcher, ContentTypeResolver.allContentTypesFetcher, ContentTypeResolver.depositByContentTypeIdFetcher,
  )
}
