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
package nl.knaw.dans.easy.properties.app.graphql.model

import nl.knaw.dans.easy.properties.app.model.{ Timestamp, Timestamped, timestampOrdering }
import scala.Ordering.Implicits._

object TimebasedSearch {

  def apply[T <: Timestamped](earlierThan: Option[Timestamp],
                              laterThan: Option[Timestamp],
                              atTimestamp: Option[Timestamp],
                              orderBy: Option[Ordering[T]],
                             )(input: Seq[T]): Seq[T] = {
    val filterTimebased: Timestamped => Boolean = (earlierThan, laterThan, atTimestamp) match {
      case (None, None, None) => _ => true // returns a predicate that always returns true
      case (Some(earlierThan), None, None) => _.timestamp < earlierThan
      case (None, Some(laterThan), None) => _.timestamp > laterThan
      case (Some(earlierThan), Some(laterThan), None) => between(earlierThan, laterThan)
      case (None, None, Some(atTimestamp)) => _.timestamp equiv atTimestamp
      case (None, Some(_), Some(_)) |
           (Some(_), None, Some(_)) |
           (Some(_), Some(_), Some(_)) => throw new IllegalArgumentException("argument 'atTimestamp' cannot be used in conjunction with arguments 'earlierThan' or 'laterThan'")
    }
    val filtered = input.filter(filterTimebased)
    orderBy.fold(filtered)(filtered.sorted(_))
  }

  private def between(earlierThan: Timestamp, laterThan: Timestamp)(timestamped: Timestamped): Boolean = {
    if (earlierThan equiv laterThan)
      throw new IllegalArgumentException("arguments 'earlierThan' and 'laterThan' cannot have the same value; use 'atTimestamp' instead")
    else if (earlierThan > laterThan) {
      timestamped.timestamp < earlierThan && timestamped.timestamp > laterThan
    }
    else
      timestamped.timestamp < earlierThan || timestamped.timestamp > laterThan
  }
}
