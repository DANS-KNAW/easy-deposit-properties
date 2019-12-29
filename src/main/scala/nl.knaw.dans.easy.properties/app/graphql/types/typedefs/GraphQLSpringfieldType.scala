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

import nl.knaw.dans.easy.properties.app.graphql.ordering.{ OrderDirection, SpringfieldOrder, SpringfieldOrderField }
import nl.knaw.dans.easy.properties.app.model.springfield.SpringfieldPlayMode
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.schema.{ EnumType, InputObjectType }

trait GraphQLSpringfieldType {
  this: GraphQLCommonTypes =>

  implicit val SpringfieldPlayModeType: EnumType[SpringfieldPlayMode.Value] = deriveEnumType(
    EnumTypeDescription("The playmode in Springfield for this deposit."),
    DocumentValue("CONTINUOUS", "Play audio/video continuously in Springfield."),
    DocumentValue("MENU", "Play audio/video in Springfield as selected in a menu."),
  )

  implicit val SpringfieldOrderFieldType: EnumType[SpringfieldOrderField.Value] = deriveEnumType()
  implicit val SpringfieldOrderInputType: InputObjectType[SpringfieldOrder] = deriveInputObjectType(
    InputObjectTypeDescription("Ordering options for springfields"),
    DocumentInputField("field", "The field to order springfields by"),
    DocumentInputField("direction", "The ordering direction"),
  )
  implicit val SpringfieldOrderFromInput: FromInput[SpringfieldOrder] = fromInput(ad => SpringfieldOrder(
    field = ad("field").asInstanceOf[SpringfieldOrderField.Value],
    direction = ad("direction").asInstanceOf[OrderDirection.Value],
  ))
}
