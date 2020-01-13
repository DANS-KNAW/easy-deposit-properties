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

import nl.knaw.dans.easy.properties.app.model._

import scala.Ordering.Implicits._

object TimebasedSearch {

  def apply[T <: Timestamped](timeFilter: Option[TimeFilter],
                              orderBy: Option[Ordering[T]],
                             )(input: Seq[T]): Seq[T] = {
    val filterTimebased: Timestamped => Boolean = timeFilter map {
      case EarlierThan(earlierThan) => (t: Timestamped) => t.timestamp < earlierThan
      case LaterThan(laterThan) => (t: Timestamped) => t.timestamp > laterThan
      case AtTime(atTimestamp) => (t: Timestamped) => t.timestamp equiv atTimestamp
      case Between(earlierThan, laterThan) => (t: Timestamped) => t.timestamp < earlierThan && t.timestamp > laterThan
      case NotBetween(earlierThan, laterThan) => (t: Timestamped) => t.timestamp < earlierThan || t.timestamp > laterThan
    } getOrElse {
      _: Timestamped => true
    }
    val filtered = input.filter(filterTimebased)
    orderBy.fold(filtered)(filtered.sorted(_))
  }
}
