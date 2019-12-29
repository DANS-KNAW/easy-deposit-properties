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
package nl.knaw.dans.easy.properties.app.graphql.types.typedefs

import nl.knaw.dans.easy.properties.app.graphql.types.Scalars
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.{ DepositDoiRegisteredFilter, SeriesFilter }
import sangria.macros.derive.{ DocumentInputField, InputObjectTypeDescription, deriveInputObjectType }
import sangria.marshalling.FromInput
import sangria.schema.InputObjectType

trait GraphQLDoiRegisteredType {
  this: Scalars with GraphQLCommonTypes =>

  implicit val DepositDoiRegisteredFilterType: InputObjectType[DepositDoiRegisteredFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The label and filter to be used in searching for deposits by whether the DOI is registered."),
    DocumentInputField("value", "If provided, only show deposits with the same value for DOI registered."),
    DocumentInputField("filter", "Determine whether to search in current value for DOI registered (`LATEST`, default) or all current and past values (`ALL`)."),
  )
  implicit val DepositDoiRegisteredFilterFromInput: FromInput[DepositDoiRegisteredFilter] = fromInput(ad => DepositDoiRegisteredFilter(
    value = ad("value").asInstanceOf[Boolean],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))
}
