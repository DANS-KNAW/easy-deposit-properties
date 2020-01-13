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
package nl.knaw.dans.easy.properties.app.model.sort

import sangria.macros.derive.GraphQLDescription

import scala.language.implicitConversions

object OrderDirection extends Enumeration {
  type OrderDirection = Value

  // @formatter:off
  @GraphQLDescription("Specifies an ascending order for a given orderBy argumen.")
  val ASC : OrderDirection = Value("ASC")
  @GraphQLDescription("Specifies a descending order for a given orderBy argument")
  val DESC: OrderDirection = Value("DESC")
  // @formatter:on

  case class OrderDirectionValue(value: OrderDirection) {
    def withOrder[T](ordering: Ordering[T]): Ordering[T] = {
      value match {
        case ASC => ordering
        case DESC => ordering.reverse
      }
    }
  }
  implicit def value2OrderDirectionValue(value: OrderDirection): OrderDirectionValue = OrderDirectionValue(value)
}
