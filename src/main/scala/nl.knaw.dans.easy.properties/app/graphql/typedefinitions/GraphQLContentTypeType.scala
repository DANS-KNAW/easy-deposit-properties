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

import nl.knaw.dans.easy.properties.app.model.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.contentType.DepositContentTypeFilter
import nl.knaw.dans.easy.properties.app.model.sort.{ ContentTypeOrder, ContentTypeOrderField, OrderDirection }
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.schema.{ EnumType, InputObjectType }

trait GraphQLContentTypeType {
  this: GraphQLCommonTypes =>

  implicit val DepositContentTypeFilterType: InputObjectType[DepositContentTypeFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The label and filter to be used in searching for deposits by content type."),
    DocumentInputField("value", "If provided, only show deposits with this content type."),
    DocumentInputField("filter", "Determine whether to search in current content types (`LATEST`, default) or all current and past content types (`ALL`)."),
  )
  implicit val DepositContentTypeFilterFromInput: FromInput[DepositContentTypeFilter] = fromInput(ad => DepositContentTypeFilter(
    value = ad("value").asInstanceOf[String],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))

  implicit val ContentTypeOrderFieldType: EnumType[ContentTypeOrderField.Value] = deriveEnumType(
    EnumTypeDescription("Properties by which content types can be ordered"),
    DocumentValue("VALUE", "Order content types by value"),
    DocumentValue("TIMESTAMP", "Order content types by timestamp"),
  )
  implicit val ContentTypeOrderInputType: InputObjectType[ContentTypeOrder] = deriveInputObjectType(
    InputObjectTypeDescription("Ordering options for content types"),
    DocumentInputField("field", "The field to order content types by"),
    DocumentInputField("direction", "The ordering direction"),
  )
  implicit val ContentTypeOrderFromInput: FromInput[ContentTypeOrder] = fromInput(ad => ContentTypeOrder(
    field = ad("field").asInstanceOf[ContentTypeOrderField.Value],
    direction = ad("direction").asInstanceOf[OrderDirection.Value],
  ))
}
