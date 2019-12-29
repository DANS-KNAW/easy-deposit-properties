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

import nl.knaw.dans.easy.properties.app.model.{ CurationRequiredEvent, Timestamp }
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }

@GraphQLName("CurationRequiredEvent")
@GraphQLDescription("Whether curation by data manager is required.")
class GraphQLCurationRequired(curationRequired: CurationRequiredEvent) {

  @GraphQLField
  @GraphQLDescription("True if curation by a data manager is required.")
  val value: Boolean = curationRequired.curationRequired

  @GraphQLField
  @GraphQLDescription("The timestamp at which was decided that this deposit requires curation.")
  val timestamp: Timestamp = curationRequired.timestamp
}
