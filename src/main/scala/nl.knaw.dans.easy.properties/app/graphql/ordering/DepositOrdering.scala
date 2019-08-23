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

import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, Timestamp, timestampOrdering }
import sangria.macros.derive.GraphQLDescription

@GraphQLDescription("Properties by which deposits can be ordered")
object DepositOrderField extends Enumeration {
  type DepositOrderField = Value

  // @formatter:off
  @GraphQLDescription("Order deposits by depositId")
  val DEPOSIT_ID        : DepositOrderField = Value("DEPOSIT_ID")
  @GraphQLDescription("Order deposits by bag name")
  val BAG_NAME          : DepositOrderField = Value("BAG_NAME")
  @GraphQLDescription("Order deposits by creation timestamp")
  val CREATION_TIMESTAMP: DepositOrderField = Value("CREATION_TIMESTAMP")
  @GraphQLDescription("Order deposits by origin")
  val ORIGIN: DepositOrderField = Value("ORIGIN")
  // @formatter:on
}

case class DepositOrder(field: DepositOrderField.DepositOrderField,
                        direction: OrderDirection.OrderDirection) extends Ordering[Deposit] {
  def compare(x: Deposit, y: Deposit): Int = {
    val orderByField: Ordering[Deposit] = field match {
      case DepositOrderField.DEPOSIT_ID =>
        Ordering[DepositId].on(_.id)
      case DepositOrderField.BAG_NAME =>
        Ordering[Option[String]].on(_.bagName)
      case DepositOrderField.CREATION_TIMESTAMP =>
        Ordering[Timestamp].on(_.creationTimestamp)
      case DepositOrderField.ORIGIN =>
        Ordering[Origin].on(_.origin)
    }

    direction.withOrder(orderByField).compare(x, y)
  }
}
