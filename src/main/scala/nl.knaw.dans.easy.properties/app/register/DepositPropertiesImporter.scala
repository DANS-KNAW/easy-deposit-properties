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

import better.files.StringOps
import cats.instances.either._
import cats.instances.list._
import cats.instances.option._
import cats.syntax.either._
import cats.syntax.traverse._
import nl.knaw.dans.easy.properties.app.repository.{ MutationErrorOr, Repository }
import org.apache.commons.configuration.{ ConfigurationException, PropertiesConfiguration }

object DepositPropertiesImporter {

  def readDepositProperties(props: String): Either[ReadImportError, PropertiesConfiguration] = {
    Either.catchOnly[ConfigurationException] {
      new PropertiesConfiguration() {
        setDelimiterParsingDisabled(true)
        load(props.inputStream)
      }
    }.leftMap(e => ReadImportError(e.getMessage, e))
  }

  def importDepositProperties(props: DepositProperties, repo: Repository): MutationErrorOr[Unit] = {
    val depositId = props.deposit.id

    for {
      _ <- repo.deposits.store(props.deposit)
      _ <- props.state.traverse(repo.states.store(depositId, _))
      _ <- props.ingestStep.traverse(repo.ingestSteps.store(depositId, _))
      _ <- props.identifiers.toList.traverse(repo.identifiers.store(depositId, _))
      _ <- props.doiAction.traverse(repo.doiAction.store(depositId, _))
      _ <- props.doiRegistered.traverse(repo.doiRegistered.store(depositId, _))
      _ <- props.curation.traverse(repo.curation.store(depositId, _))
      _ <- props.springfield.traverse(repo.springfield.store(depositId, _))
      _ <- props.contentType.traverse(repo.contentType.store(depositId, _))
    } yield ()
  }
}
