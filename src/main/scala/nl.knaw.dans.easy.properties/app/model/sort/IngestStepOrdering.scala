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

import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel }
import nl.knaw.dans.easy.properties.app.model.{ Timestamp, timestampOrdering }

object IngestStepOrderField extends Enumeration {
  type IngestStepOrderField = Value

  // @formatter:off
  val STEP     : IngestStepOrderField = Value("STEP")
  val TIMESTAMP: IngestStepOrderField = Value("TIMESTAMP")
  // @formatter:on
}

case class IngestStepOrder(field: IngestStepOrderField.IngestStepOrderField,
                           direction: OrderDirection.OrderDirection) extends Ordering[IngestStep] {
  def compare(x: IngestStep, y: IngestStep): Int = {
    val orderByField: Ordering[IngestStep] = field match {
      case IngestStepOrderField.STEP =>
        Ordering[IngestStepLabel.IngestStepLabel].on(_.step)
      case IngestStepOrderField.TIMESTAMP =>
        Ordering[Timestamp].on(_.timestamp)
    }

    direction.withOrder(orderByField).compare(x, y)
  }
}
