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
import nl.knaw.dans.easy.properties.app.graphql.resolvers.SpringfieldResolver
import nl.knaw.dans.easy.properties.app.model.Timestamp
import nl.knaw.dans.easy.properties.app.model.springfield.Springfield
import nl.knaw.dans.easy.properties.app.model.springfield.SpringfieldPlayMode.SpringfieldPlayMode
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.Node
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("Springfield")
@GraphQLDescription("Springfield configuration associated with this deposit.")
class GraphQLSpringfield(springfield: Springfield) extends Node {

  @GraphQLField
  @GraphQLDescription("The domain of Springfield.")
  val domain: String = springfield.domain
  
  @GraphQLField
  @GraphQLDescription("The user of Springfield.")
  val user: String = springfield.user
  
  @GraphQLField
  @GraphQLDescription("The collection of Springfield.")
  val collection: String = springfield.collection
  
  @GraphQLField
  @GraphQLDescription("The playmode used in Springfield.")
  val playmode: SpringfieldPlayMode = springfield.playmode
  
  @GraphQLField
  @GraphQLDescription("The timestamp at which this springfield configuration was associated with the deposit.")
  val timestamp: Timestamp = springfield.timestamp

  override val id: String = springfield.id

  @GraphQLField
  @GraphQLDescription("Returns the deposit that is associated with this particular springfield configuration.")
  def deposit(implicit ctx: Context[DataContext, GraphQLSpringfield]): DeferredValue[DataContext, Option[GraphQLDeposit]] = {
    SpringfieldResolver.depositBySpringfieldId(id)
      .map(_.map(new GraphQLDeposit(_)))
  }
}
