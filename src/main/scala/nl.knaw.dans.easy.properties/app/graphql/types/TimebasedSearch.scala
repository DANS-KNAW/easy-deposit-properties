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
package nl.knaw.dans.easy.properties.app.graphql.types

import nl.knaw.dans.easy.properties.app.model.{ Timestamp, Timestamped, timestampOrdering }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import sangria.marshalling.FromInput.coercedScalaInput
import sangria.schema.{ Argument, OptionInputType, WithArguments }

import scala.Ordering.Implicits._
import scala.language.postfixOps

trait TimebasedSearch extends DebugEnhancedLogging {
  this: Scalars =>

  case class TimebasedSearchArguments(earlierThan: Option[Timestamp],
                                      laterThan: Option[Timestamp],
                                      atTimestamp: Option[Timestamp],
                                     ) {

    private def between(earlierThan: Timestamp, laterThan: Timestamp)(timestamped: Timestamped): Boolean = {
      if (earlierThan equiv laterThan)
        throw new IllegalArgumentException("arguments 'earlierThan' and 'laterThan' cannot have the same value; use 'atTimestamp' instead")
      else if (earlierThan > laterThan)
        timestamped.timestamp < earlierThan && timestamped.timestamp > laterThan
      else
        timestamped.timestamp < earlierThan || timestamped.timestamp > laterThan
    }

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
  }
  object TimebasedSearchArguments {
    def apply(args: WithArguments): TimebasedSearchArguments = {
      TimebasedSearchArguments(
        args arg earlierThanArgument,
        args arg laterThanArgument,
        args arg atTimestampArgument,
      )
    }
  }

  val earlierThanArgument: Argument[Option[Timestamp]] = Argument(
    name = "earlierThan",
    argumentType = OptionInputType(DateTimeType),
    description = Some("List only those elements that have a timestamp earlier than this given timestamp."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val laterThanArgument: Argument[Option[Timestamp]] = Argument(
    name = "laterThan",
    argumentType = OptionInputType(DateTimeType),
    description = Some("List only those elements that have a timestamp later than this given timestamp."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val atTimestampArgument: Argument[Option[Timestamp]] = Argument(
    name = "atTimestamp",
    argumentType = OptionInputType(DateTimeType),
    description = Some("List only those elements that have a timestamp equal to the given timestamp."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val timebasedSearchArguments = List(
    earlierThanArgument,
    laterThanArgument,
    atTimestampArgument,
  )

  def timebasedFilterAndSort[T <: Timestamped, Ord <: Ordering[T]](orderArgument: Argument[Option[Ord]])
                                                                  (input: Seq[T])
                                                                  (implicit context: WithArguments): Seq[T] = {
    val timebasedSearchArguments = TimebasedSearchArguments(context)
    val filtered = input.filter(timebasedSearchArguments.filterTimebased)

    context.arg(orderArgument)
      .fold(filtered)(filtered.sorted(_))
  }
}
