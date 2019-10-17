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

import cats.syntax.either._
import cats.syntax.foldable._
import nl.knaw.dans.easy.properties.ApplicationErrorOr
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.Repository

class DepositPropertiesRegistration(repository: => Repository) {

  def register(depositId: DepositId, props: String): ImportErrorOr[DepositId] = {
    for {
      props <- DepositPropertiesImporter.readDepositProperties(props)
      depositProperties <- DepositPropertiesValidator.validateDepositProperties(depositId)(props)
        .leftMap(errors => ValidationImportErrors(errors.toList))
        .toEither
      _ <- importDeposit(depositProperties)
        .leftMap {
          case e: ImportError => e
          case e => DBImportError(e.getMessage, e)
        }
    } yield depositProperties.deposit.id
  }

  private def importDeposit(depositProperties: DepositProperties): ApplicationErrorOr[Unit] = {
    val depositId = depositProperties.deposit.id
    for {
      exists <- DepositPropertiesValidator.depositExists(depositId)(repository)
      _ <- if (exists) DepositAlreadyExistsError(depositId).asLeft
           else ().asRight
      _ <- DepositPropertiesImporter.importDepositProperties(depositProperties, repository)
    } yield ()
  }
}
