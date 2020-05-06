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
package nl.knaw.dans.easy.properties.app.graphql.typedefinitions

import nl.knaw.dans.easy.properties.app.graphql._
import nl.knaw.dans.easy.properties.app.graphql.model._
import nl.knaw.dans.easy.properties.app.graphql.resolvers._
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.string._
import sangria.macros.derive._
import sangria.relay.{ GlobalId, Node, NodeDefinition }
import sangria.schema.{ Context, ObjectType }

trait GraphQLQueryType {
  this: Scalars
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
    with GraphQLSpringfieldType
    with GraphQLStateType =>

  val NodeDefinition(nodeInterface, nodeField, nodesField) = {
    Node.definition(
      resolve = (id: GlobalId, ctx: Context[DataContext, Unit]) => {
        implicit val context: Context[DataContext, Unit] = ctx
        id.typeName match {
          case GraphQLContentTypeType.name =>
            ContentTypeResolver.contentTypeById(id.id)
              .map(_.map(new GraphQLContentType(_)))
          case GraphQLCuratorType.name =>
            CuratorResolver.curationById(id.id)
              .map(_.map(new GraphQLCurator(_)))
          case GraphQLDepositType.name =>
            DepositResolver.depositById(id.id.toUUID.toTry.unsafeGetOrThrow)
              .map(_.map(new GraphQLDeposit(_)))
          case GraphQLIdentifierType.name =>
            IdentifierResolver.identifierById(id.id)
              .map(_.map(new GraphQLIdentifier(_)))
          case GraphQLIngestStepType.name =>
            IngestStepResolver.ingestStepById(id.id)
              .map(_.map(new GraphQLIngestStep(_)))
          case GraphQLIsNewVersionType.name =>
            IsNewVersionResolver.isNewVersionById(id.id)
              .map(_.map(new GraphQLIsNewVersion(_)))
          case GraphQLCurationRequiredType.name =>
            IsCurationRequiredResolver.isCurationRequiredById(id.id)
              .map(_.map(new GraphQLCurationRequired(_)))
          case GraphQLCurationPerformedType.name =>
            IsCurationPerformedResolver.isCurationPerformedById(id.id)
              .map(_.map(new GraphQLCurationPerformed(_)))
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
        GraphQLIsNewVersionType,
        GraphQLCurationRequiredType,
        GraphQLCurationPerformedType,
        GraphQLSpringfieldType,
        GraphQLStateType,
      ),
    )
  }

  implicit lazy val GraphQLContentTypeType: ObjectType[DataContext, GraphQLContentType] = deriveObjectType[DataContext, GraphQLContentType](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLCurationPerformedType: ObjectType[DataContext, GraphQLCurationPerformed] = deriveObjectType[DataContext, GraphQLCurationPerformed](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLCurationRequiredType: ObjectType[DataContext, GraphQLCurationRequired] = deriveObjectType[DataContext, GraphQLCurationRequired](
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

  implicit val GraphQLDepositorType: ObjectType[DataContext, GraphQLDepositor] = deriveObjectType[DataContext, GraphQLDepositor]()

  implicit val GraphQLDoiActionType: ObjectType[DataContext, GraphQLDoiAction] = deriveObjectType[DataContext, GraphQLDoiAction]()

  implicit val GraphQLDoiRegisteredType: ObjectType[DataContext, GraphQLDoiRegistered] = deriveObjectType[DataContext, GraphQLDoiRegistered]()

  implicit lazy val GraphQLIdentifierType: ObjectType[DataContext, GraphQLIdentifier] = deriveObjectType[DataContext, GraphQLIdentifier](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLIngestStepType: ObjectType[DataContext, GraphQLIngestStep] = deriveObjectType[DataContext, GraphQLIngestStep](
    Interfaces(nodeInterface),
    AddFields(Node.globalIdField),
  )

  implicit lazy val GraphQLIsNewVersionType: ObjectType[DataContext, GraphQLIsNewVersion] = deriveObjectType[DataContext, GraphQLIsNewVersion](
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
}
