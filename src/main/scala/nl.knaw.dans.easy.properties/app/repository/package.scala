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

import nl.knaw.dans.easy.properties.ApplicationError
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.{ DepositId, Timestamp }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import sangria.execution.UserFacingError

import scala.concurrent.Future

package object repository extends DebugEnhancedLogging {

  type QueryErrorOr[T] = Either[QueryError, T]
  type MutationErrorOr[T] = Either[MutationError, T]

  sealed abstract class QueryError(val msg: String) extends Exception(msg) with ApplicationError with UserFacingError
  case class DepositDoesNotExistError(depositId: DepositId) extends QueryError(s"Deposit $depositId does not exist.")
  case class InvalidValueError(override val msg: String) extends QueryError(msg)
  object InvalidValueError {
    def apply(ts: Seq[Throwable], query: String =""): InvalidValueError = {
      val msg = ts.map(_.getMessage).mkString("\n")
      logger.debug(s"$msg [$query]")
      InvalidValueError(msg)
    }
  }

  sealed abstract class MutationError(val msg: String) extends Exception(msg) with ApplicationError with UserFacingError
  object MutationError {
    def apply(msg: String): MutationError = new MutationError(msg) {}
  }
  case class NoSuchDepositError(depositId: DepositId) extends MutationError(s"Deposit $depositId does not exist.")
  case class BagNameAlreadySetError(depositId: DepositId) extends MutationError(s"Deposit $depositId already has a bagName set. This cannot be done twice.")
  case class DepositAlreadyExistsError(depositId: DepositId) extends MutationError(s"Deposit $depositId already exist.")
  case class DepositIdAndTimestampAlreadyExistError(depositId: DepositId, timestamp: Timestamp, objName: String) extends MutationError(s"Cannot insert this $objName: timestamp '$timestamp' is already used for another $objName associated to depositId $depositId.")
  case class IdentifierAlreadyExistsError(depositId: DepositId, identifierType: IdentifierType) extends MutationError(s"Identifier $identifierType already exists for depositId $depositId.")

  implicit class MaxByOption[A](val t: TraversableOnce[A]) extends AnyVal {
    def maxByOption[B](f: A => B)(implicit cmp: Ordering[B]): Option[A] = {
      if (t.isEmpty) Option.empty
      else Option(t.maxBy(f))
    }
  }

  implicit class EitherToFuture[A <: Throwable, B](val either: Either[A, B]) extends AnyVal {
    def toFuture: Future[B] = {
      either match {
        case Left(error) => Future.failed(error)
        case Right(value) => Future.successful(value)
      }
    }
  }
}
