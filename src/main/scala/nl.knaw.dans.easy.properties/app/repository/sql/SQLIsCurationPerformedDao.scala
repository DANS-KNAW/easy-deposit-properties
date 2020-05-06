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
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.{ InputIsCurationPerformed, IsCurationPerformed }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, IsCurationPerformedDao, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.lang.BooleanUtils
import resource.managed

class SQLIsCurationPerformedDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends IsCurationPerformedDao with SQLDeletableProperty with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val tableName = "SimpleProperties"
  override private[sql] val key = "is-curation-performed"

  private def parseIsCurationPerformed(resultSet: ResultSet): Either[InvalidValueError, IsCurationPerformed] = {
    for {
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
      value = BooleanUtils.toBoolean(resultSet.getString("value"))
      id = resultSet.getString("propertyId")
    } yield IsCurationPerformed(id, value, timestamp)
  }

  private def parseDepositIdAndIsCurationPerformed(resultSet: ResultSet): Either[InvalidValueError, (DepositId, IsCurationPerformed)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      contentType <- parseIsCurationPerformed(resultSet)
    } yield depositId -> contentType
  }

  private def parseIsCurationPerformedIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      contentTypeId = resultSet.getString("propertyId")
    } yield contentTypeId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[IsCurationPerformed]] = { 
    trace(ids)
    
    executeGetById(parseIsCurationPerformed)(QueryGenerator.getSimplePropsElementsById(key))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, IsCurationPerformed)]] = { 
    trace(ids)
    
    executeGetCurrent(parseDepositIdAndIsCurationPerformed)(QueryGenerator.getSimplePropsCurrentElementByDepositId(key))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[IsCurationPerformed])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndIsCurationPerformed)(QueryGenerator.getSimplePropsAllElementsByDepositId(key))(ids)
  }

  override def store(id: DepositId, isCurationPerformed: InputIsCurationPerformed): MutationErrorOr[IsCurationPerformed] = { 
    trace(id, isCurationPerformed)
    
    val query = QueryGenerator.storeSimpleProperty
    
    managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      .getResultSetForUpdateWith(id, key, BooleanUtils.toStringTrueFalse(isCurationPerformed.value), isCurationPerformed.timestamp)
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert content type (${ isCurationPerformed.value }, ${ isCurationPerformed.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, isCurationPerformed.timestamp, objName = "is curation performed")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(isCurationPerformed.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = { 
    trace(ids)

    executeGetDepositById(parseIsCurationPerformedIdAndDeposit)(QueryGenerator.getSimplePropsDepositsById(key))(ids)
  }
}
