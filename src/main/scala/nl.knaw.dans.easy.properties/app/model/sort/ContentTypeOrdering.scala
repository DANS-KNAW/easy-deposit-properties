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

import nl.knaw.dans.easy.properties.app.model.contentType.ContentType
import nl.knaw.dans.easy.properties.app.model.{ Timestamp, timestampOrdering }

object ContentTypeOrderField extends Enumeration {
  type ContentTypeOrderField = Value

  // @formatter:off
  val VALUE:     ContentTypeOrderField = Value("VALUE")
  val TIMESTAMP: ContentTypeOrderField = Value("TIMESTAMP")
  // @formatter:on
}

case class ContentTypeOrder(field: ContentTypeOrderField.ContentTypeOrderField,
                            direction: OrderDirection.OrderDirection) extends Ordering[ContentType] {
  def compare(x: ContentType, y: ContentType): Int = {
    val orderByField: Ordering[ContentType] = field match {
      case ContentTypeOrderField.VALUE =>
        Ordering[String].on(_.value)
      case ContentTypeOrderField.TIMESTAMP =>
        Ordering[Timestamp].on(_.timestamp)
    }

    direction.withOrder(orderByField).compare(x, y)
  }
}
