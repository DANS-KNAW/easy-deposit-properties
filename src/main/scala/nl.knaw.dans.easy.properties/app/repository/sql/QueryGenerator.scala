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

import cats.data.NonEmptyList
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.{ DepositFilter, DepositId, SeriesFilter }
import nl.knaw.dans.easy.properties.app.repository.{ DepositFilters, DepositorIdFilters }

object QueryGenerator {

  lazy val getAllDeposits: String = "SELECT * FROM Deposit;"

  def findDeposits(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT * FROM Deposit WHERE depositId IN (${ ids.toList.map(_ => "?").mkString(", ") });"

    query -> ids.map(setDepositId).toList
  }

  private type TableName = String
  private type KeyName = String
  private type LabelName = String
  private type Query = String

  private def createSearchSubQuery[T <: DepositFilter](filter: T)(tableName: TableName, labelName: LabelName, labelValue: T => String): (TableName, Query, List[PrepStatementResolver]) = {
    val query = filter.filter match {
      case SeriesFilter.ALL =>
        s"SELECT DISTINCT depositId FROM $tableName WHERE $labelName = ?"
      case SeriesFilter.LATEST =>
        s"SELECT $tableName.depositId FROM $tableName INNER JOIN (SELECT depositId, max(timestamp) AS max_timestamp FROM $tableName GROUP BY depositId) AS ${ tableName }WithMaxTimestamp ON $tableName.timestamp = ${ tableName }WithMaxTimestamp.max_timestamp WHERE $labelName = ?"
    }

    (tableName, query, setString(labelValue(filter)) :: Nil)
  }

  private def createSearchSimplePropertiesSubQuery[T <: DepositFilter](filter: T)(keyValue: String, labelValue: T => String): (TableName, Query, List[PrepStatementResolver]) = {
    val tableName = "SimpleProperties"
    val query = filter.filter match {
      case SeriesFilter.ALL =>
        s"SELECT DISTINCT depositId FROM $tableName WHERE key = ? AND value = ?"
      case SeriesFilter.LATEST =>
        s"SELECT $tableName.depositId FROM $tableName INNER JOIN (SELECT depositId, max(timestamp) AS max_timestamp FROM $tableName WHERE key = ? GROUP BY depositId) AS ${ tableName }WithMaxTimestamp ON $tableName.timestamp = ${ tableName }WithMaxTimestamp.max_timestamp WHERE value = ?"
    }

    (tableName, query, setString(labelValue(filter)) :: setString(keyValue) :: Nil)
  }

  def searchDeposits(filters: DepositFilters): (String, Seq[PrepStatementResolver]) = {
    val (queryWherePart, whereValues) = List(
      filters.depositorId.map("depositorId" -> _),
      filters.bagName.map("bagName" -> _),
      filters.originFilter.map("origin" -> _.toString),
    )
      .collect {
        case Some((labelName, null)) => s"$labelName IS NULL" -> Nil
        case Some((labelName, value)) => s"$labelName = ?" -> List(setString(value))
      }
      .foldLeft(("", List.empty[PrepStatementResolver])) {
        case (("", vs), (subQuery, values)) => subQuery -> (values ::: vs)
        case ((q, vs), (subQuery, values)) => s"$q AND $subQuery" -> (values ::: vs)
      }
    val (queryJoinPart, joinValues) = List(
      filters.stateFilter.map(createSearchSubQuery(_)("State", "label", _.label.toString)),
      filters.ingestStepFilter.map(createSearchSimplePropertiesSubQuery(_)("ingest-step", _.label.toString)),
      filters.doiRegisteredFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-registered", _.value.toString)),
      filters.doiActionFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-action", _.value.toString)),
      filters.curatorFilter.map(createSearchSubQuery(_)("Curation", "datamanagerUserId", _.curator)),
      filters.isNewVersionFilter.map(createSearchSubQuery(_)("Curation", "isNewVersion", _.isNewVersion.toString)),
      filters.curationRequiredFilter.map(createSearchSubQuery(_)("Curation", "isRequired", _.curationRequired.toString)),
      filters.curationPerformedFilter.map(createSearchSubQuery(_)("Curation", "isPerformed", _.curationPerformed.toString)),
      filters.contentTypeFilter.map(createSearchSimplePropertiesSubQuery(_)("content-type", _.value.toString)),
    )
      .collect {
        case Some((tableName, q, values)) if queryWherePart.isEmpty => s"INNER JOIN ($q) AS ${ tableName }SearchResult ON Deposit.depositId = ${ tableName }SearchResult.depositId" -> values
        case Some((tableName, q, values)) => s"INNER JOIN ($q) AS ${ tableName }SearchResult ON SelectedDeposits.depositId = ${ tableName }SearchResult.depositId" -> values
      }
      .foldLeft(("", List.empty[PrepStatementResolver])) {
        case (("", vs), (subQuery, values)) => subQuery -> (values ::: vs)
        case ((q, vs), (subQuery, values)) => s"$q $subQuery" -> (values ::: vs)
      }

    (queryJoinPart, queryWherePart) match {
      case ("", "") =>
        val query = s"SELECT * FROM Deposit;"
        query -> Nil
      case ("", _) =>
        val query = s"SELECT * FROM Deposit WHERE $queryWherePart;"
        query -> whereValues.reverse
      case (_, "") =>
        val query = s"SELECT * FROM Deposit $queryJoinPart;"
        query -> joinValues.reverse
      case (_, _) =>
        val query = s"SELECT * FROM (SELECT * FROM Deposit WHERE $queryWherePart) AS SelectedDeposits $queryJoinPart;"
        query -> (whereValues.reverse ::: joinValues.reverse)
    }
  }

  def searchDepositors(filters: DepositorIdFilters): (String, Seq[PrepStatementResolver]) = {
    val (queryWherePart, whereValues) = List(
      filters.originFilter.map("origin" -> _.toString),
    )
      .collect {
        case Some((labelName, null)) => s"$labelName IS NULL" -> Nil
        case Some((labelName, value)) => s"$labelName = ?" -> List(setString(value))
      }
      .foldLeft(("", List.empty[PrepStatementResolver])) {
        case (("", vs), (subQuery, values)) => subQuery -> (values ::: vs)
        case ((q, vs), (subQuery, values)) => s"$q AND $subQuery" -> (values ::: vs)
      }
    val (queryJoinPart, joinValues) = List(
      filters.stateFilter.map(createSearchSubQuery(_)("State", "label", _.label.toString)),
      filters.ingestStepFilter.map(createSearchSimplePropertiesSubQuery(_)("ingest-step", _.label.toString)),
      filters.doiRegisteredFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-registered", _.value.toString)),
      filters.doiActionFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-action", _.value.toString)),
      filters.curatorFilter.map(createSearchSubQuery(_)("Curation", "datamanagerUserId", _.curator)),
      filters.isNewVersionFilter.map(createSearchSubQuery(_)("Curation", "isNewVersion", _.isNewVersion.toString)),
      filters.curationRequiredFilter.map(createSearchSubQuery(_)("Curation", "isRequired", _.curationRequired.toString)),
      filters.curationPerformedFilter.map(createSearchSubQuery(_)("Curation", "isPerformed", _.curationPerformed.toString)),
      filters.contentTypeFilter.map(createSearchSimplePropertiesSubQuery(_)("content-type", _.value.toString)),
    )
      .collect {
        case Some((tableName, q, values)) if queryWherePart.isEmpty => s"INNER JOIN ($q) AS ${ tableName }SearchResult ON Deposit.depositId = ${ tableName }SearchResult.depositId" -> values
        case Some((tableName, q, values)) => s"INNER JOIN ($q) AS ${ tableName }SearchResult ON SelectedDeposits.depositId = ${ tableName }SearchResult.depositId" -> values
      }
      .foldLeft(("", List.empty[PrepStatementResolver])) {
        case (("", vs), (subQuery, values)) => subQuery -> (values ::: vs)
        case ((q, vs), (subQuery, values)) => s"$q $subQuery" -> (values ::: vs)
      }
    
    (queryJoinPart, queryWherePart) match {
      case ("", "") =>
        val query = "SELECT DISTINCT depositorId FROM Deposit;"
        query -> Nil
      case ("", _) =>
        val query = s"SELECT DISTINCT depositorId FROM Deposit WHERE $queryWherePart;"
        query -> whereValues.reverse
      case (_, "") =>
        val query = s"SELECT DISTINCT depositorId FROM Deposit $queryJoinPart;"
        query -> joinValues.reverse
      case (_, _) =>
        val query = s"SELECT DISTINCT depositorId FROM (SELECT depositId, depositorId FROM Deposit WHERE $queryWherePart) AS SelectedDeposits $queryJoinPart;"
        query -> (whereValues.reverse ::: joinValues.reverse)
    }
  }

  def getLastModifiedDate(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val whereClause = ids.toList.map(_ => "?").mkString("WHERE depositId IN (", ", ", ")")
    val tablesAndMaxFields = List(
      "Deposit" -> "creationTimestamp",
      "State" -> "timestamp",
      "Identifier" -> "timestamp",
      "Curation" -> "timestamp",
      "Springfield" -> "timestamp",
      "SimpleProperties" -> "timestamp",
    )
    val query = tablesAndMaxFields
      .map { case (table, maxField) => s"(SELECT depositId, MAX($maxField) AS max FROM $table $whereClause GROUP BY depositId)" }
      .mkString("SELECT depositId, MAX(max) AS max_timestamp FROM (", " UNION ALL ", ") AS max_timestamps GROUP BY depositId;")

    val stringIds = ids.map(setDepositId)
    val values = tablesAndMaxFields.map(_ => stringIds).reduce(_ ::: _).toList

    query -> values
  }

  def getElementsById(tableName: String, idColumnName: String)(ids: NonEmptyList[String]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT * FROM $tableName WHERE $idColumnName IN (${ ids.toList.map(_ => "?").mkString(", ") });"

    query -> ids.map(setInt).toList
  }

  def getCurrentElementByDepositId(tableName: String)(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val query =
      s"""SELECT *
         |FROM $tableName
         |INNER JOIN (
         |  SELECT depositId, max(timestamp) AS max_timestamp
         |  FROM $tableName
         |  WHERE depositId IN (${ ids.toList.map(_ => "?").mkString(", ") })
         |  GROUP BY depositId
         |) AS deposit_with_max_timestamp USING (depositId)
         |WHERE timestamp = max_timestamp;""".stripMargin

    query -> ids.map(setDepositId).toList
  }

  def getAllElementsByDepositId(tableName: String)(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT * FROM $tableName WHERE depositId IN (${ ids.toList.map(_ => "?").mkString(", ") });"

    query -> ids.map(setDepositId).toList
  }

  def getDepositsById(tableName: String, idColumnName: String)(ids: NonEmptyList[String]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT $idColumnName, Deposit.depositId, bagName, creationTimestamp, depositorId, origin FROM Deposit INNER JOIN $tableName ON Deposit.depositId = $tableName.depositId WHERE $idColumnName IN (${ ids.toList.map(_ => "?").mkString(", ") });"

    query -> ids.map(setInt).toList
  }

  def getIdentifierByDepositIdAndType(ids: NonEmptyList[(DepositId, IdentifierType)]): (String, Seq[PrepStatementResolver]) = {
    val (queryWherePart, valuesWherePart) = ids
      .map {
        case (depositId, idType) => "(depositId = ? AND identifierSchema = ?)" -> (setDepositId(depositId) :: setString(idType.toString) :: Nil)
      }
      .foldLeft(("", List.empty[PrepStatementResolver])) {
        case (("", vs), (subQuery, values)) => subQuery -> (vs ::: values)
        case ((q, vs), (subQuery, values)) => s"$subQuery OR $q" -> (vs ::: values)
      }

    s"SELECT identifierId, depositId, identifierSchema, identifierValue, timestamp FROM Identifier WHERE $queryWherePart;" -> valuesWherePart
  }

  def getIdentifierByTypeAndValue(ids: NonEmptyList[(IdentifierType, String)]): (String, Seq[PrepStatementResolver]) = {
    val (queryWherePart, valuesWherePart) = ids
      .map {
        case (idType, idValue) => "(identifierSchema = ? AND identifierValue = ?)" -> (setString(idType.toString) :: setString(idValue.toString) :: Nil)
      }
      .foldLeft(("", List.empty[PrepStatementResolver])) {
        case (("", vs), (subQuery, values)) => subQuery -> (vs ::: values)
        case ((q, vs), (subQuery, values)) => s"$subQuery OR $q" -> (vs ::: values)
      }

    s"SELECT identifierId, identifierSchema, identifierValue, timestamp FROM Identifier WHERE $queryWherePart;" -> valuesWherePart
  }

  def getSimplePropsElementsById(key: String)(ids: NonEmptyList[String]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT * FROM SimpleProperties WHERE key = ? AND propertyId IN (${ ids.toList.map(_ => "?").mkString(", ") });"
    val values = setString(key) :: ids.map(setInt)

    query -> values.toList
  }

  def getSimplePropsCurrentElementByDepositId(key: String)(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val query =
      s"""SELECT *
         |FROM SimpleProperties
         |INNER JOIN (
         |  SELECT depositId, max(timestamp) AS max_timestamp
         |  FROM SimpleProperties
         |  WHERE key = ?
         |  AND depositId IN (${ ids.toList.map(_ => "?").mkString(", ") })
         |  GROUP BY depositId
         |) AS deposit_with_max_timestamp USING (depositId)
         |WHERE timestamp = max_timestamp
         |AND key = ?;""".stripMargin
    val values = key :: (ids.map(_.toString) :+ key)

    query -> values.map(setString).toList
  }

  def getSimplePropsAllElementsByDepositId(key: String)(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT * FROM SimpleProperties WHERE key = ? AND depositId IN (${ ids.toList.map(_ => "?").mkString(", ") });"
    val values = key :: ids.map(_.toString)

    query -> values.map(setString).toList
  }

  def getSimplePropsDepositsById(key: String)(ids: NonEmptyList[String]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT propertyId, Deposit.depositId, bagName, creationTimestamp, depositorId, origin FROM Deposit INNER JOIN SimpleProperties ON Deposit.depositId = SimpleProperties.depositId WHERE key = ? AND propertyId IN (${ ids.toList.map(_ => "?").mkString(", ") });"
    val values = setString(key) :: ids.map(setInt)

    query -> values.toList
  }

  lazy val storeDeposit: String = "INSERT INTO Deposit (depositId, bagName, creationTimestamp, depositorId, origin) VALUES (?, ?, ?, ?, ?);"

  lazy val storeBagName: String = "UPDATE Deposit SET bagName = ? WHERE depositId = ? AND (bagName IS NULL OR bagName='');"

  def storeCuration(isNewVersionDefined: Boolean): String = {
    // Note: this is a hack: as `isNewVersion` is an optional property, but it is also a `Boolean`, we cannot use `null` in `prepStatement.setBoolean`. This is not allowed by Scala.
    // Workaround applied here is to add the `isNewVersion` to the end, only if it is defined.
    if (isNewVersionDefined)
      "INSERT INTO Curation (depositId, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp, isNewVersion) VALUES (?, ?, ?, ?, ?, ?, ?);"
    else
      "INSERT INTO Curation (depositId, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp) VALUES (?, ?, ?, ?, ?, ?);"
  }

  lazy val storeIdentifier: String = "INSERT INTO Identifier (depositId, identifierSchema, identifierValue, timestamp) VALUES (?, ?, ?, ?);"

  lazy val storeSimpleProperty: String = "INSERT INTO SimpleProperties (depositId, key, value, timestamp) VALUES (?, ?, ?, ?);"

  lazy val storeSpringfield: String = "INSERT INTO Springfield (depositId, domain, springfield_user, collection, playmode, timestamp) VALUES (?, ?, ?, ?, ?, ?);"

  lazy val storeState: String = "INSERT INTO State (depositId, label, description, timestamp) VALUES (?, ?, ?, ?);"
}
