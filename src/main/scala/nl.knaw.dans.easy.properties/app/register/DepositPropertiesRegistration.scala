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
package nl.knaw.dans.easy.properties.app.register

import java.io.InputStream
import java.sql.Connection

import cats.syntax.either._
import cats.syntax.foldable._
import nl.knaw.dans.easy.properties.ApplicationErrorOr
import nl.knaw.dans.easy.properties.app.database.DatabaseAccess
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.Repository

class DepositPropertiesRegistration(database: DatabaseAccess,
                                    repository: Connection => Repository) {

  def register(is: InputStream): ImportErrorOr[DepositId] = {
    for {
      props <- DepositPropertiesImporter.readDepositProperties(is)
      depositProperties <- DepositPropertiesValidator.validateDepositProperties(props)
        .leftMap(es => ValidationImportErrors(es.toList))
        .toEither
      _ <- databaseInteract { implicit repo => importDeposit(depositProperties) }
    } yield depositProperties.deposit.id
  }

  private def databaseInteract[T](f: Repository => ApplicationErrorOr[T]): ImportErrorOr[T] = {
    database.doTransaction(conn => f(repository(conn)).toTry)
      .toEither
      .leftMap {
        case e: ImportError => e
        case e => DBImportError(e.getMessage, e)
      }
  }

  private def importDeposit(depositProperties: DepositProperties)(implicit repo: Repository): ApplicationErrorOr[Unit] = {
    val depositId = depositProperties.deposit.id
    for {
      exists <- DepositPropertiesValidator.depositExists(depositId)
      _ <- if (exists) DepositAlreadyExistsError(depositId).asLeft
           else ().asRight
      _ <- DepositPropertiesImporter.importDepositProperties(depositProperties)
    } yield ()
  }
}
