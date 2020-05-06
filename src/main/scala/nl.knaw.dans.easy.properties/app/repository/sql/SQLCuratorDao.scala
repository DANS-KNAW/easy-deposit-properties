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

import java.sql.{ Connection, ResultSet, Statement }

import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.curator.{ Curator, InputCurator }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ CuratorDao, DepositIdAndTimestampAlreadyExistError, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLCuratorDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends CuratorDao with SQLDeletable with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val tableName = "Curator"

  private def parseCurator(resultSet: ResultSet): Either[InvalidValueError, Curator] = {
    for {
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
      curatorEntryId = resultSet.getString("curatorEntryId")
      userId = resultSet.getString("datamanagerUserId")
      email = resultSet.getString("datamanagerEmail")
    } yield Curator(curatorEntryId, userId, email, timestamp)
  }

  private def parseDepositIdAndCurator(resultSet: ResultSet): Either[InvalidValueError, (DepositId, Curator)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      curator <- parseCurator(resultSet)
    } yield depositId -> curator
  }

  private def parseCuratorEntryIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      curatorEntryId = resultSet.getString("curatorEntryId")
    } yield curatorEntryId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[Curator]] = {
    trace(ids)

    executeGetById(parseCurator)(QueryGenerator.getElementsById(tableName, "curatorEntryId"))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Curator)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndCurator)(QueryGenerator.getCurrentElementByDepositId(tableName))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[Curator])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndCurator)(QueryGenerator.getAllElementsByDepositId(tableName))(ids)
  }

  override def store(id: DepositId, curator: InputCurator): MutationErrorOr[Curator] = {
    trace(id, curator)
    val query = QueryGenerator.storeCurator

    managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      .getResultSetForUpdateWith(id, curator.datamanagerUserId, curator.datamanagerEmail, curator.timestamp)
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert curator (${ curator.datamanagerUserId }, ${ curator.datamanagerEmail }, ${ curator.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, curator.timestamp, objName = "curator")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(curator.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = {
    trace(ids)

    executeGetDepositById(parseCuratorEntryIdAndDeposit)(QueryGenerator.getDepositsById(tableName, "curatorEntryId"))(ids)
  }
}
