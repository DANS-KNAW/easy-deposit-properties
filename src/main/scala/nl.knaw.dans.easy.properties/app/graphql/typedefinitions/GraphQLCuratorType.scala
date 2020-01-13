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
import nl.knaw.dans.easy.properties.app.model.curator.DepositCuratorFilter
import nl.knaw.dans.easy.properties.app.model.sort.{ CuratorOrder, CuratorOrderField, OrderDirection }
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.schema.{ EnumType, InputObjectType }

trait GraphQLCuratorType {
  this: GraphQLCommonTypes =>

  implicit val DepositCuratorFilterType: InputObjectType[DepositCuratorFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The label and filter to be used in searching for deposits by curator."),
    DocumentInputField("curator", "If provided, only show deposits with this curator."),
    DocumentInputField("filter", "Determine whether to search in current curator (`LATEST`, default) only or all current and past curators (`ALL`) of this deposit."),
    RenameInputField("curator", "userId"),
  )
  implicit val DepositCuratorFilterFromInput: FromInput[DepositCuratorFilter] = fromInput(ad => DepositCuratorFilter(
    curator = ad("userId").asInstanceOf[String],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))

  implicit val CuratorOrderFieldType: EnumType[CuratorOrderField.Value] = deriveEnumType(
    EnumTypeDescription("Properties by which curators can be ordered"),
    DocumentValue("USERID", "Order curators by step"),
    DocumentValue("TIMESTAMP", "Order curators by timestamp"),
  )
  implicit val CuratorOrderInputType: InputObjectType[CuratorOrder] = deriveInputObjectType(
    InputObjectTypeDescription("Ordering options for curators"),
    DocumentInputField("field", "The field to order curators by"),
    DocumentInputField("direction", "The ordering direction"),
  )
  implicit val CuratorOrderFromInput: FromInput[CuratorOrder] = fromInput(ad => CuratorOrder(
    field = ad("field").asInstanceOf[CuratorOrderField.Value],
    direction = ad("direction").asInstanceOf[OrderDirection.Value],
  ))
}
