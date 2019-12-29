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
import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.{ DepositDoiActionFilter, DoiAction, SeriesFilter }
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.schema.{ EnumType, InputObjectType }

trait GraphQLDoiActionType {
  this: Scalars with GraphQLCommonTypes =>

  implicit val DoiActionType: EnumType[DoiAction.Value] = deriveEnumType(
    EnumTypeDescription("Whether the DANS-DOI must be created or updated in the DataCite resolver."),
    DocumentValue("CREATE", "The DANS-DOI must be created in the DataCite resolver."),
    DocumentValue("UPDATE", "The DANS-DOI must be updated in the DataCite resolver."),
    DocumentValue("NONE", "None action must be taken for this DANS-DOI in the DataCite resolver."),
  )

  implicit val DepositDoiActionFilterType: InputObjectType[DepositDoiActionFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The label and filter to be used in searching for deposits by DOI registration action."),
    DocumentInputField("value", "If provided, only show deposits with the same value for DOI action."),
    DocumentInputField("filter", "Determine whether to search in current value for DOI action (`LATEST`, default) or all current and past values (`ALL`)."),
  )
  implicit val DepositDoiActionFilterFromInput: FromInput[DepositDoiActionFilter] = fromInput(ad => DepositDoiActionFilter(
    value = ad("value").asInstanceOf[DoiAction],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))
}
