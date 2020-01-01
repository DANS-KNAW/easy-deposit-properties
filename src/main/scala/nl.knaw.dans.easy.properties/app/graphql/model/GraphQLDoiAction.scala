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

import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.{ DoiActionEvent, Timestamp }
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }

@GraphQLName("DoiActionEvent")
@GraphQLDescription("A DOI action event related to a deposit.")
class GraphQLDoiAction(doiAction: DoiActionEvent) {

  @GraphQLField
  @GraphQLDescription("Whether the DOI must be 'created' or 'updated' when registering in DataCite.")
  val value: DoiAction = doiAction.value

  @GraphQLField
  @GraphQLDescription("The timestamp at which this value was added.")
  val timestamp: Timestamp = doiAction.timestamp
}
