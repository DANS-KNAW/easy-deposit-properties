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

import nl.knaw.dans.easy.properties.app.model.Origin
import nl.knaw.dans.easy.properties.app.model.sort.{ DepositOrder, DepositOrderField, OrderDirection }
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.schema.{ EnumType, InputObjectType }

trait GraphQLDepositType {
  this: GraphQLCommonTypes =>

  implicit lazy val OriginType: EnumType[Origin.Value] = deriveEnumType(
    EnumTypeDescription("The origin of the deposit."),
    DocumentValue("SWORD2", "easy-sword2"),
    DocumentValue("API", "easy-deposit-api"),
    DocumentValue("SMD", "easy-split-multi-deposit"),
  )

  implicit val DepositOrderFieldType: EnumType[DepositOrderField.Value] = deriveEnumType(
    EnumTypeDescription("Properties by which deposits can be ordered."),
    DocumentValue("DEPOSIT_ID", "Order deposits by depositId."),
    DocumentValue("BAG_NAME", "Order deposits by bag name."),
    DocumentValue("CREATION_TIMESTAMP", "Order deposits by creation timestamp."),
    DocumentValue("ORIGIN", "Order deposits by origin."),
  )

  implicit val DepositOrderInputType: InputObjectType[DepositOrder] = deriveInputObjectType(
    InputObjectTypeDescription("Ordering options for deposits"),
    DocumentInputField("field", "The field to order deposit by"),
    DocumentInputField("direction", "The ordering direction"),
  )
  implicit val DepositOrderFromInput: FromInput[DepositOrder] = fromInput(ad => DepositOrder(
    field = ad("field").asInstanceOf[DepositOrderField.Value],
    direction = ad("direction").asInstanceOf[OrderDirection.Value],
  ))
}
