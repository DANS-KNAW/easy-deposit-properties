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

import nl.knaw.dans.easy.properties.app.model.sort.OrderDirection
import nl.knaw.dans.easy.properties.app.model.springfield.Springfield
import nl.knaw.dans.easy.properties.app.model.{ Timestamp, timestampOrdering }
import sangria.macros.derive.GraphQLDescription

@GraphQLDescription("Properties by which springfields can be ordered")
object SpringfieldOrderField extends Enumeration {
  type SpringfieldOrderField = Value

  // @formatter:off
  @GraphQLDescription("Order springfields by timestamp")
  val TIMESTAMP: SpringfieldOrderField = Value("TIMESTAMP")
  // @formatter:on
}

case class SpringfieldOrder(field: SpringfieldOrderField.SpringfieldOrderField,
                            direction: OrderDirection.OrderDirection) extends Ordering[Springfield] {
  def compare(x: Springfield, y: Springfield): Int = {
    val orderByField: Ordering[Springfield] = field match {
      case SpringfieldOrderField.TIMESTAMP =>
        Ordering[Timestamp].on(_.timestamp)
    }

    direction.withOrder(orderByField).compare(x, y)
  }
}
