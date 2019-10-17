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
package nl.knaw.dans.easy.properties.app.repository.sql

import java.sql.Connection

import cats.data.NonEmptyList
import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.{ MutationError, Repository }

class SQLRepo(implicit connection: Connection, errorHandler: SQLErrorHandler) {

  private val depositDao = new SQLDepositDao
  private val stateDao = new SQLStateDao
  private val ingestStepDao = new SQLIngestStepDao
  private val identifierDao = new SQLIdentifierDao
  private val doiRegisteredDao = new SQLDoiRegisteredDao
  private val doiActionDao = new SQLDoiActionDao
  private val curationDao = new SQLCurationDao
  private val springfieldDao = new SQLSpringfieldDao
  private val contentTypeDao = new SQLContentTypeDao

  def repository: Repository = Repository(
    depositDao,
    stateDao,
    ingestStepDao,
    identifierDao,
    doiRegisteredDao,
    doiActionDao,
    curationDao,
    springfieldDao,
    contentTypeDao,
  )

  def deleteDepositsBy(ids: Seq[DepositId]): Either[MutationError, Seq[DepositId]] = {
    for {
      deposits <- depositDao.find(ids).leftMap(error => MutationError(error.msg))
      actualIds = deposits.map(_.id).toList
      _ = NonEmptyList.fromList(actualIds).map(deleteDeposits)
    } yield actualIds
  }

  private def deleteDeposits(ids: NonEmptyList[DepositId]): Either[MutationError, Int] = {
    Stream[SQLDeletable](
      stateDao,
     // ingestStepDao, TODO need to filter ids with find on every DAO
      identifierDao,
      doiRegisteredDao,
     // doiActionDao,
      curationDao,
      springfieldDao,
     // contentTypeDao,
      // TODO Dao for simpleProperties
      depositDao, // last because of foreign keys by the others
    ).map(_.delete(ids))
      .find(_.isLeft) // the stream makes it a fail fast
      .getOrElse(0.asRight)
  }
}
