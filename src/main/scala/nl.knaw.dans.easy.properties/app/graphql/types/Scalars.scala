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

import java.util.UUID

import cats.syntax.either._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import sangria.ast.StringValue
import sangria.schema.ScalarType
import sangria.validation.{ StringCoercionViolation, ValueCoercionViolation, Violation }

trait Scalars {

  case object UUIDCoercionViolation extends ValueCoercionViolation("UUID value expected")

  private def parseUUID(s: String): Either[Violation, UUID] = {
    Either.catchNonFatal { UUID.fromString(s) }
      .fold(_ => Left(UUIDCoercionViolation), _.asRight)
  }

  implicit val UUIDType: ScalarType[UUID] = ScalarType("UUID",
    description = Some("The UUID scalar type represents textual data, " +
      "formatted as a universally unique identifier."),
    coerceOutput = (value, _) => value.toString,
    coerceUserInput = {
      case s: String => parseUUID(s)
      case _ => Left(StringCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseUUID(s)
      case _ => Left(StringCoercionViolation)
    }
  )

  case object DateCoercionViolation extends ValueCoercionViolation("Date value expected")

  private def parseDate(s: String): Either[Violation, DateTime] = {
    Either.catchNonFatal { DateTime.parse(s) }
      .fold(_ => Left(DateCoercionViolation), _.asRight)
  }

  implicit val DateTimeType: ScalarType[DateTime] = ScalarType("DateTime",
    description = Some("A DateTime scalar type represents textual data, " +
      "formatted as an ISO8601 date-time."),
    coerceOutput = (value, _) => ISODateTimeFormat.dateTime() print value,
    coerceUserInput = {
      case s: String => parseDate(s)
      case _ => Left(DateCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => parseDate(s)
      case _ => Left(DateCoercionViolation)
    }
  )
}
