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
package nl.knaw.dans.easy.properties.app.graphql.model

import nl.knaw.dans.easy.properties.app.graphql._
import nl.knaw.dans.easy.properties.app.graphql.resolvers.IdentifierResolver
import nl.knaw.dans.easy.properties.app.model.Timestamp
import nl.knaw.dans.easy.properties.app.model.identifier.Identifier
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.Node
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("Identifier")
@GraphQLDescription("An identifier related to a deposit.")
class GraphQLIdentifier(identifier: Identifier) extends Node {

  @GraphQLField
  @GraphQLName("type")
  @GraphQLDescription("The type of identifier.")
  val idType: IdentifierType = identifier.idType

  @GraphQLField
  @GraphQLName("value")
  @GraphQLDescription("The value of the identifier.")
  val idValue: String = identifier.idValue

  @GraphQLField
  @GraphQLDescription("The timestamp at which the identifier got added to this deposit.")
  val timestamp: Timestamp = identifier.timestamp

  override val id: String = identifier.id

  @GraphQLField
  @GraphQLDescription("Returns the deposit that is associated with this particular identifier.")
  def deposit(implicit ctx: Context[DataContext, GraphQLIdentifier]): DeferredValue[DataContext, GraphQLDeposit] = {
    IdentifierResolver.depositByIdentifierId(ctx.value.id)
      .map(new GraphQLDeposit(_))
  }
}
