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
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.model.isnewversion.{ InputIsNewVersion, IsNewVersion }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, IsNewVersionDao, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.lang.BooleanUtils
import resource.managed

class SQLIsNewVersionDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends IsNewVersionDao with SQLDeletableProperty with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val tableName = "SimpleProperties"
  override private[sql] val key = "is-new-version"

  private def parseIsNewVersion(resultSet: ResultSet): Either[InvalidValueError, IsNewVersion] = {
    for {
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
      value = BooleanUtils.toBoolean(resultSet.getString("value"))
      id = resultSet.getString("propertyId")
    } yield IsNewVersion(id, value, timestamp)
  }

  private def parseDepositIdAndIsNewVersion(resultSet: ResultSet): Either[InvalidValueError, (DepositId, IsNewVersion)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      contentType <- parseIsNewVersion(resultSet)
    } yield depositId -> contentType
  }

  private def parseIsNewVersionIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      contentTypeId = resultSet.getString("propertyId")
    } yield contentTypeId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[IsNewVersion]] = { 
    trace(ids)
    
    executeGetById(parseIsNewVersion)(QueryGenerator.getSimplePropsElementsById(key))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, IsNewVersion)]] = { 
    trace(ids)
    
    executeGetCurrent(parseDepositIdAndIsNewVersion)(QueryGenerator.getSimplePropsCurrentElementByDepositId(key))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[IsNewVersion])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndIsNewVersion)(QueryGenerator.getSimplePropsAllElementsByDepositId(key))(ids)
  }

  override def store(id: DepositId, isNewVersion: InputIsNewVersion): MutationErrorOr[IsNewVersion] = { 
    trace(id, isNewVersion)
    
    val query = QueryGenerator.storeSimpleProperty
    
    managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      .getResultSetForUpdateWith(id, key, BooleanUtils.toStringTrueFalse(isNewVersion.value), isNewVersion.timestamp)
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert content type (${ isNewVersion.value }, ${ isNewVersion.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, isNewVersion.timestamp, objName = "is new version")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(isNewVersion.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = { 
    trace(ids)

    executeGetDepositById(parseIsNewVersionIdAndDeposit)(QueryGenerator.getSimplePropsDepositsById(key))(ids)
  }
}
