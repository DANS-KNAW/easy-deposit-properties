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
package nl.knaw.dans.easy.properties.app.graphql.ordering

import nl.knaw.dans.easy.properties.app.model.Origin
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import sangria.macros.derive.GraphQLDescription

@GraphQLDescription("Properties by which Origins can be ordered")
object OriginOrderField extends Enumeration {
  type OriginOrderField = Value

  // @formatter:off
  @GraphQLDescription("Order Origins by timestamp")
  val ORIGIN: OriginOrderField = Value("ORIGIN")
  // @formatter:on
}

case class OriginOrder(field: OriginOrderField.OriginOrderField,
                       direction: OrderDirection.OrderDirection) extends Ordering[Origin] {
  def compare(x: Origin, y: Origin): Int = {
    val orderByField: Ordering[Origin] = field match {
      case OriginOrderField.ORIGIN =>
        Ordering[Origin.type].on(_)
    }

    direction.withOrder(orderByField).compare(x, y)
  }
}
