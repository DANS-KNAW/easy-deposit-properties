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
package nl.knaw.dans.easy.properties.app.model

import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import sangria.macros.derive.{ DocumentValue, EnumTypeDescription, deriveEnumType }
import sangria.schema.EnumType

case class Deposit(id: DepositId,
                   bagName: Option[String],
                   creationTimestamp: Timestamp,
                   depositorId: DepositorId,
                   origin: Origin,
                  ) extends Timestamped {
  def timestamp: Timestamp = creationTimestamp
}

object Origin extends Enumeration {
  type Origin = Value

  // @formatter:off
  val SWORD2: Origin = Value("SWORD2")
  val API   : Origin = Value("API")
  val SMD   : Origin = Value("SMD")
  // @formatter:on

  def deriveType: EnumType[Origin.Value] = deriveEnumType(
    EnumTypeDescription("The origin of the deposit."),
    DocumentValue("SWORD2", "The DANS-DOI must be created in the DataCite resolver."),
    DocumentValue("API", "The DANS-DOI must be updated in the DataCite resolver."),
    DocumentValue("SMD", "None action must be taken for this DANS-DOI in the DataCite resolver."),
  )
}

