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

import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.{ DepositCurationPerformedFilter, SeriesFilter }
import sangria.macros.derive.{ DocumentInputField, InputObjectTypeDescription, RenameInputField, deriveInputObjectType }
import sangria.marshalling.FromInput
import sangria.schema.InputObjectType

trait GraphQLCurationPerformedType {
  this: GraphQLCommonTypes =>

  implicit val DepositCurationPerformedFilterType: InputObjectType[DepositCurationPerformedFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The value and filter to be used in searching for deposits by 'curation-performed'."),
    DocumentInputField("curationPerformed", "If provided, only show deposits with this value for 'curation-performed'."),
    DocumentInputField("filter", "Determine whether to search in current values (`LATEST`, default) or all current and past values (`ALL`)."),
    RenameInputField("curationPerformed", "value"),
  )
  implicit val DepositCurationPerformedFilterFromInput: FromInput[DepositCurationPerformedFilter] = fromInput(ad => DepositCurationPerformedFilter(
    curationPerformed = ad("value").asInstanceOf[Boolean],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))
}
