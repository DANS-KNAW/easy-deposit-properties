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

import nl.knaw.dans.easy.properties.app.graphql.ordering.OrderDirection
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.model.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import sangria.macros.derive.{ DocumentValue, EnumTypeDescription, deriveEnumType }
import sangria.marshalling.ToInput
import sangria.marshalling.ToInput.ScalarToInput
import sangria.schema.{ EnumType, ObjectType }

trait GraphQLCommonTypes {

  implicit val SeriesFilterType: EnumType[SeriesFilter.Value] = deriveEnumType(
    EnumTypeDescription("Mark a query to only search through current states, or also to include past states."),
    DocumentValue("LATEST", "Only search through current states."),
    DocumentValue("ALL", "Search through both current and past states."),
  )
  implicit val SeriesFilterToInput: ToInput[SeriesFilter, _] = new ScalarToInput

  implicit val OrderDirectionType: EnumType[OrderDirection.Value] = deriveEnumType()

  implicit def GeneralConnectionType[Ctx, T](implicit objType: ObjectType[Ctx, T]): ObjectType[Ctx, ExtendedConnection[T]] = {
    ExtendedConnection.definition[Ctx, ExtendedConnection, T](objType.name, objType).connectionType
  }
}
