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
package nl.knaw.dans.easy.properties.app

import cats.data.ValidatedNec
import cats.syntax.option._
import nl.knaw.dans.easy.properties.ApplicationError
import nl.knaw.dans.easy.properties.app.model.DepositId
import sangria.execution.UserFacingError

package object register {

  type ValidationImportErrorsOr[T] = ValidatedNec[ValidationImportError, T]
  type ImportErrorOr[T] = Either[ImportError, T]

  abstract class ValidationImportError(val msg: String, cause: Option[Throwable] = none)
  case class PropertyNotFoundError(key: String) extends ValidationImportError(s"Mandatory property '$key' was not found.")
  case class MissingPropertiesError(missing: Seq[String], present: Seq[String]) extends ValidationImportError(s"Properties '${missing.mkString("{", ", ", "}")}' were not found, while ${present.mkString("{", ", ", "}")} were found.")
  case class PropertyParseError(key: String, cause: Throwable) extends ValidationImportError(s"Property '$key' could not be parsed: ${ cause.getMessage }", cause.some)

  abstract class ImportError(val msg: String, cause: Option[Throwable] = none) extends Exception(msg, cause.orNull) with ApplicationError with UserFacingError
  case class ReadImportError(override val msg: String, cause: Throwable) extends ImportError(msg, cause.some)
  case class ValidationImportErrors(es: Seq[ValidationImportError]) extends ImportError(s"Invalid input:\n${ es.map(e => s" - ${ e.msg }").mkString("\n") }")
  case class DepositAlreadyExistsError(depositId: DepositId) extends ImportError(s"Deposit $depositId already exists")
  case class DBImportError(override val msg: String, cause: Throwable) extends ImportError(msg, cause.some)
}
