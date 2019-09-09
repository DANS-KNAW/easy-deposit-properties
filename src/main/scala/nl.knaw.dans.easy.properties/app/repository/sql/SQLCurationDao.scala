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
import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.curation.{ Curation, InputCuration }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ CurationDao, DepositIdAndTimestampAlreadyExistError, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLCurationDao(implicit connection: Connection, errorHandler: SQLErrorHandler) extends CurationDao with CommonResultSetParsers with DebugEnhancedLogging {

  private def parseCuration(resultSet: ResultSet): Either[InvalidValueError, Curation] = {
    for {
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
      curationId = resultSet.getString("curationId")
      isNewVersion = {
        // `getBoolean` returns `true` or `false` and does not allow for nullable values
        // https://stackoverflow.com/a/39561156/2389405 provides a solution to this by using `wasNull`
        val isNewVersion = resultSet.getBoolean("isNewVersion")
        if (resultSet.wasNull()) none
        else isNewVersion.some
      }
      isRequired = resultSet.getBoolean("isRequired")
      isPerformed = resultSet.getBoolean("isPerformed")
      userId = resultSet.getString("datamanagerUserId")
      email = resultSet.getString("datamanagerEmail")
    } yield Curation(curationId, isNewVersion, isRequired, isPerformed, userId, email, timestamp)
  }

  private def parseDepositIdAndCuration(resultSet: ResultSet): Either[InvalidValueError, (DepositId, Curation)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      curation <- parseCuration(resultSet)
    } yield depositId -> curation
  }

  private def parseCurationIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      curationId = resultSet.getString("curationId")
    } yield curationId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[Curation]] = {
    trace(ids)

    executeGetById(parseCuration)(QueryGenerator.getElementsById("Curation", "curationId"))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Option[Curation])]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndCuration)(QueryGenerator.getCurrentElementByDepositId("Curation"))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[Curation])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndCuration)(QueryGenerator.getAllElementsByDepositId("Curation"))(ids)
  }

  override def store(id: DepositId, curation: InputCuration): MutationErrorOr[Curation] = {
    trace(id, curation)
    val query = QueryGenerator.storeCuration(isNewVersionDefined = curation.isNewVersion.isDefined)

    val managedResultSet = for {
      prepStatement <- managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      _ = prepStatement.setString(1, id.toString)
      _ = prepStatement.setBoolean(2, curation.isRequired)
      _ = prepStatement.setBoolean(3, curation.isPerformed)
      _ = prepStatement.setString(4, curation.datamanagerUserId)
      _ = prepStatement.setString(5, curation.datamanagerEmail)
      _ = prepStatement.setTimestamp(6, curation.timestamp, timeZone)
      _ = curation.isNewVersion.foreach(prepStatement.setBoolean(7, _)) // only include this parameter if isNewVersion is defined; the query keeps this optional parameter into account
      _ = prepStatement.executeUpdate()
      resultSetForKey <- managed(prepStatement.getGeneratedKeys)
    } yield resultSetForKey

    managedResultSet
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert curation data (isNewVersion = ${ curation.isNewVersion.getOrElse("none") }, curation required = ${ curation.isRequired }, curation performed = ${ curation.isPerformed }, datamanager userId = ${ curation.datamanagerUserId }, datamanager email = ${ curation.datamanagerEmail }, timestamp = ${ curation.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, curation.timestamp, "curation")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(curation.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Option[Deposit])]] = {
    trace(ids)

    executeGetDepositById(parseCurationIdAndDeposit)(QueryGenerator.getDepositsById("Curation", "curationId"))(ids)
  }
}
