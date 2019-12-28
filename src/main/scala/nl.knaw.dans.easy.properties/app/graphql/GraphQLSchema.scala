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

import java.util.UUID

import nl.knaw.dans.easy.properties.app.graphql.annotated.{ GraphQLIdentifier, dataContextFromContext, _ }
import nl.knaw.dans.easy.properties.app.graphql.annotated.types.{ GraphQLDepositType, GraphQLIdentifierType }
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ ContentTypeResolver, CurationResolver, DepositResolver, DoiEventResolver, IdentifierResolver, IngestStepResolver, SpringfieldResolver, StateResolver }
import nl.knaw.dans.easy.properties.app.graphql.types.Scalars
import sangria.execution.deferred.DeferredResolver
import sangria.macros.derive._
import sangria.relay.{ GlobalId, Node, NodeDefinition }
import sangria.schema._

object GraphQLSchema extends
  Scalars
  with GraphQLDepositType
  with GraphQLIdentifierType
  //  with NodeType
  //  with MetaTypes
  //  with TimebasedSearch
  //  with ContentTypeGraphQLType
  //  with SpringfieldType
  //  with CurationEventType
  //  with DoiEventTypes
  //  with IdentifierGraphQLType
  //  with IngestStepType
  //  with StateType
  //  with CurationType
  //  with CuratorType
  //  with DepositorType
  //  with DepositType
  //  with QueryType
  //  with MutationType 
{

  val NodeDefinition(nodeInterface, nodeField, nodesField) = {
    Node.definition(
      resolve = (id: GlobalId, ctx: Context[DataContext, Unit]) => {
        implicit val context: Context[DataContext, Unit] = ctx
        id.typeName match {
          case GraphQLContentTypeType.name =>
            ContentTypeResolver.contentTypeById(id.id)
              .map(_.map(new GraphQLContentType(_)))
          case GraphQLCuratorType.name =>
            CurationResolver.curationById(id.id)
              .map(_.map(curation => new GraphQLCurator(curation.getCurator)))
          case GraphQLDepositType.name =>
            DepositResolver.depositById(UUID.fromString(id.id))
              .map(_.map(new GraphQLDeposit(_)))
          case GraphQLIdentifierType.name =>
            IdentifierResolver.identifierById(id.id)
              .map(_.map(new GraphQLIdentifier(_)))
          case GraphQLIngestStepType.name =>
            IngestStepResolver.ingestStepById(id.id)
              .map(_.map(new GraphQLIngestStep(_)))
          case GraphQLSpringfieldType.name =>
            SpringfieldResolver.springfieldById(id.id)
              .map(_.map(new GraphQLSpringfield(_)))
          case GraphQLStateType.name =>
            StateResolver.stateById(id.id)
              .map(_.map(new GraphQLState(_)))
          case _ => None
        }
      },
      possibleTypes = Node.possibleNodeTypes[DataContext, Node](
        GraphQLContentTypeType,
        GraphQLCuratorType,
        GraphQLDepositType,
        GraphQLIdentifierType,
        GraphQLIngestStepType,
        GraphQLSpringfieldType,
        GraphQLStateType,
      ),
    )
  }

  implicit lazy val GraphQLContentTypeType: ObjectType[DataContext, GraphQLContentType] = deriveObjectType[DataContext, GraphQLContentType](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLCuratorType: ObjectType[DataContext, GraphQLCurator] = deriveObjectType[DataContext, GraphQLCurator](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLDepositType: ObjectType[DataContext, GraphQLDeposit] = deriveObjectType[DataContext, GraphQLDeposit](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLIdentifierType: ObjectType[DataContext, GraphQLIdentifier] = deriveObjectType[DataContext, GraphQLIdentifier](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLIngestStepType: ObjectType[DataContext, GraphQLIngestStep] = deriveObjectType[DataContext, GraphQLIngestStep](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLSpringfieldType: ObjectType[DataContext, GraphQLSpringfield] = deriveObjectType[DataContext, GraphQLSpringfield](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLStateType: ObjectType[DataContext, GraphQLState] = deriveObjectType[DataContext, GraphQLState](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit def GeneralConnectionType[Ctx, T](implicit objType: ObjectType[Ctx, T]): ObjectType[Ctx, ExtendedConnection[T]] = {
    ExtendedConnection.definition[Ctx, ExtendedConnection, T](objType.name, objType).connectionType
  }

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
    CurationResolver.byIdFetcher, CurationResolver.currentCurationsFetcher, CurationResolver.allCurationsFetcher, CurationResolver.depositByCurationIdFetcher,
    SpringfieldResolver.byIdFetcher, SpringfieldResolver.currentSpringfieldsFetcher, SpringfieldResolver.allSpringfieldsFetcher, SpringfieldResolver.depositBySpringfieldIdFetcher,
    ContentTypeResolver.byIdFetcher, ContentTypeResolver.currentContentTypesFetcher, ContentTypeResolver.allContentTypesFetcher, ContentTypeResolver.depositByContentTypeIdFetcher,
  )
}
