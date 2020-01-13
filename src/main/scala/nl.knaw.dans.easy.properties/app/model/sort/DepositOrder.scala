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

object DepositOrderField extends Enumeration {
  type DepositOrderField = Value

  // @formatter:off
  val DEPOSIT_ID         : DepositOrderField = Value("depositId")
  val BAG_NAME           : DepositOrderField = Value("bagName")
  val CREATION_TIMESTAMP : DepositOrderField = Value("creationTimestamp")
  val ORIGIN             : DepositOrderField = Value("origin")
  // @formatter:on
}

case class DepositOrder(field: DepositOrderField.DepositOrderField,
                        direction: OrderDirection.OrderDirection)
