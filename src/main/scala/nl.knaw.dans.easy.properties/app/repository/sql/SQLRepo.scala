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

import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, CurationDao, DepositDao, DoiActionDao, DoiRegisteredDao, IdentifierDao, IngestStepDao, Repository, SpringfieldDao, StateDao }

class SQLRepo(implicit connection: Connection, errorHandler: SQLErrorHandler) {

  private val depositDao: DepositDao = new SQLDepositDao
  private val stateDao: StateDao = new SQLStateDao
  private val ingestStepDao: IngestStepDao = new SQLIngestStepDao
  private val identifierDao: IdentifierDao = new SQLIdentifierDao
  private val doiRegisteredDao: DoiRegisteredDao = new SQLDoiRegisteredDao
  private val doiActionDao: DoiActionDao = new SQLDoiActionDao
  private val curationDao: CurationDao = new SQLCurationDao
  private val springfieldDao: SpringfieldDao = new SQLSpringfieldDao
  private val contentTypeDao: ContentTypeDao = new SQLContentTypeDao

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
}
