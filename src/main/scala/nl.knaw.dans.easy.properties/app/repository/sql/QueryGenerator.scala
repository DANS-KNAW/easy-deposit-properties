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
import nl.knaw.dans.easy.properties.app.model.sort.DepositOrder
import nl.knaw.dans.easy.properties.app.model.{ AtTime, Between, DepositFilter, DepositId, EarlierThan, LaterThan, NotBetween, SeriesFilter, TimeFilter }
import nl.knaw.dans.easy.properties.app.repository.{ DepositFilters, DepositorIdFilters }
import org.apache.commons.lang.BooleanUtils

object QueryGenerator {

  lazy val getAllDeposits: String = "SELECT * FROM Deposit;"

  def findDeposits(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val query = s"SELECT * FROM Deposit WHERE depositId IN (${ ids.toList.map(_ => "?").mkString(", ") });"

    query -> ids.map(setDepositId).toList
  }

  private type TableName = String
  private type KeyName = String
  private type ColumnName = String
  private type Query = String

  private def createSearchSubQuery[T <: DepositFilter](filter: T)(tableName: TableName, columnName: ColumnName, labelValue: T => String): (TableName, Query, List[PrepStatementResolver]) = {
    val query = filter.filter match {
      case SeriesFilter.ALL =>
        s"SELECT DISTINCT depositId FROM $tableName WHERE $columnName = ?"
      case SeriesFilter.LATEST =>
        s"SELECT $tableName.depositId FROM $tableName INNER JOIN (SELECT depositId, max(timestamp) AS max_timestamp FROM $tableName GROUP BY depositId) AS ${ tableName }WithMaxTimestamp ON $tableName.timestamp = ${ tableName }WithMaxTimestamp.max_timestamp WHERE $columnName = ?"
    }

    (tableName, query, setString(labelValue(filter)) :: Nil)
  }

  private def createSearchSimplePropertiesSubQuery[T <: DepositFilter](filter: T)(keyValue: String, labelValue: T => String): (TableName, Query, List[PrepStatementResolver]) = {
    val tableName = "SimpleProperties"
    filter.filter match {
      case SeriesFilter.ALL =>
        val query = s"SELECT DISTINCT depositId FROM $tableName WHERE key = ? AND value = ?"
        (tableName, query, setString(labelValue(filter)) :: setString(keyValue) :: Nil)
      case SeriesFilter.LATEST =>
        val query = s"SELECT $tableName.depositId FROM $tableName INNER JOIN (SELECT depositId, max(timestamp) AS max_timestamp FROM $tableName WHERE key = ? GROUP BY depositId) AS ${ tableName }WithMaxTimestamp ON $tableName.timestamp = ${ tableName }WithMaxTimestamp.max_timestamp WHERE key = ? AND value = ?"
        (tableName, query, setString(labelValue(filter)) :: setString(keyValue) :: setString(keyValue) :: Nil)
    }
  }

  private def createTimeFilter(columnName: String): TimeFilter => List[(String, List[PrepStatementResolver])] = {
    case EarlierThan(timestamp) => List(s"$columnName < ?::timestamp with time zone" -> List(setTimestamp(timestamp)))
    case LaterThan(timestamp) => List(s"$columnName > ?::timestamp with time zone" -> List(setTimestamp(timestamp)))
    case AtTime(timestamp) => List(s"$columnName = ?::timestamp with time zone" -> List(setTimestamp(timestamp)))
    case Between(earlier, later) => List(
      s"$columnName < ?::timestamp with time zone" -> List(setTimestamp(earlier)),
      s"$columnName > ?::timestamp with time zone" -> List(setTimestamp(later)),
    )
    case NotBetween(earlier, later) => List(
      s"($columnName > ?::timestamp with time zone OR $columnName < ?::timestamp with time zone)" -> List(setTimestamp(earlier), setTimestamp(later)),
    )
  }

  private def reduceOption[A, B](xs: List[(A, B)])(mergeL: (A, A) => A, mergeR: (B, B) => B): Option[(A, B)] = {
    xs.reduceOption[(A, B)] {
      case ((q, vs), (subQuery, values)) => mergeL(q, subQuery) -> mergeR(values, vs)
    }
  }

  def searchDeposits(filters: DepositFilters): (String, Seq[PrepStatementResolver]) = {
    val whereClauses = List(
      filters.depositorId.map("depositorId" -> _),
      filters.bagName.map("bagName" -> _),
      filters.originFilter.map("origin" -> _.toString),
    )
      .collect {
        case Some((labelName, null)) => s"$labelName IS NULL" -> Nil
        case Some((labelName, value)) => s"$labelName = ?" -> List(setString(value))
      }
    val creationTimeClause = filters.creationTimeFilter.toList.flatMap(createTimeFilter("creationTimestamp"))
    val maybeWherePart = reduceOption(whereClauses ::: creationTimeClause)(_ + " AND " + _, _ ::: _)

    val lastModifiedTimeClause = filters.lastModifiedTimeFilter.toList.flatMap(createTimeFilter("max_timestamp"))
    val maybeLastModifiedPart = reduceOption(lastModifiedTimeClause)(_ + " AND " + _, _ ::: _)
      .map {
        case (queryWherePart, resolvers) =>
          (depositTableName: String) => s"INNER JOIN LastModified ON $depositTableName.depositId = LastModified.depositId WHERE $queryWherePart" -> resolvers
      }

    val maybeJoinPart = List(
      filters.stateFilter.map(createSearchSubQuery(_)("State", "label", _.label.toString)),
      filters.ingestStepFilter.map(createSearchSimplePropertiesSubQuery(_)("ingest-step", _.label.toString)),
      filters.doiRegisteredFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-registered", _.value.toString)),
      filters.doiActionFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-action", _.value.toString)),
      filters.curatorFilter.map(createSearchSubQuery(_)("Curator", "datamanagerUserId", _.curator)),
      filters.isNewVersionFilter.map(createSearchSimplePropertiesSubQuery(_)("is-new-version", filter => BooleanUtils.toStringTrueFalse(filter.isNewVersion))),
      filters.curationRequiredFilter.map(createSearchSimplePropertiesSubQuery(_)("is-curation-required", filter => BooleanUtils.toStringTrueFalse(filter.curationRequired))),
      filters.curationPerformedFilter.map(createSearchSimplePropertiesSubQuery(_)("is-curation-performed", filter => BooleanUtils.toStringTrueFalse(filter.curationPerformed))),
      filters.contentTypeFilter.map(createSearchSimplePropertiesSubQuery(_)("content-type", _.value)),
    )
      .collect {
        case Some((tableName, q, resolvers)) =>
          (depositTableName: String) => s"INNER JOIN ($q) AS ${ tableName }SearchResult ON $depositTableName.depositId = ${ tableName }SearchResult.depositId" -> resolvers
      }
      .reduceOption((f, g) => (depositTableName: String) => {
        val (queryWherePartF, valuesF) = f(depositTableName)
        val (queryWherePartG, valuesG) = g(depositTableName)

        s"$queryWherePartF $queryWherePartG" -> (valuesG ::: valuesF)
      })

    val baseQuery = "SELECT * FROM Deposit"
    val (query, resolvers) = (maybeJoinPart, maybeLastModifiedPart, maybeWherePart) match {
      case (None, None, None) =>
        baseQuery -> Nil
      case (None, None, Some((queryWherePart, whereResolvers))) =>
        val query = s"$baseQuery WHERE $queryWherePart"
        query -> whereResolvers.reverse
      case (None, Some(lastModifiedF), None) =>
        val (lastModifiedQuery, lastModifiedResolvers) = lastModifiedF("Deposit")
        val query = s"$baseQuery $lastModifiedQuery"
        query -> lastModifiedResolvers.reverse
      case (None, Some(lastModifiedF), Some((queryWherePart, whereResolvers))) =>
        val (lastModifiedQuery, lastModifiedResolvers) = lastModifiedF("Deposit")
        val query = s"$baseQuery $lastModifiedQuery AND $queryWherePart"
        query -> (whereResolvers.reverse ::: lastModifiedResolvers.reverse)
      case (Some(queryJoinF), None, None) =>
        val (queryJoinPart, joinResolvers) = queryJoinF("Deposit")
        val query = s"$baseQuery $queryJoinPart"
        query -> joinResolvers.reverse
      case (Some(queryJoinF), None, Some((queryWherePart, whereResolvers))) =>
        val tableName = "SelectedDeposits"
        val (queryJoinPart, joinResolvers) = queryJoinF(tableName)
        val query = s"SELECT * FROM ($baseQuery WHERE $queryWherePart) AS $tableName $queryJoinPart"
        query -> (whereResolvers.reverse ::: joinResolvers.reverse)
      case (Some(queryJoinF), Some(lastModifiedF), None) =>
        val (queryJoinPart, joinResolvers) = queryJoinF("Deposit")
        val (lastModifiedQuery, lastModifiedResolvers) = lastModifiedF("Deposit")
        val query = s"$baseQuery $queryJoinPart $lastModifiedQuery"
        query -> (joinResolvers.reverse ::: lastModifiedResolvers.reverse)
      case (Some(queryJoinF), Some(lastModifiedF), Some((queryWherePart, whereResolvers))) =>
        val tableName = "SelectedDeposits"
        val (queryJoinPart, joinResolvers) = queryJoinF(tableName)
        val (lastModifiedQuery, lastModifiedResolvers) = lastModifiedF(tableName)
        val query = s"SELECT * FROM ($baseQuery WHERE $queryWherePart) AS $tableName $queryJoinPart $lastModifiedQuery"
        query -> (whereResolvers.reverse ::: joinResolvers.reverse ::: lastModifiedResolvers.reverse)
    }

    filters.sort.fold(s"$query;") {
      case DepositOrder(field, direction) => s"$query ORDER BY $field $direction;"
    } -> resolvers
  }

  def searchDepositors(filters: DepositorIdFilters): (String, Seq[PrepStatementResolver]) = {
    val whereClauses = List(
      filters.originFilter.map("origin" -> _.toString),
    )
      .collect {
        case Some((labelName, null)) => s"$labelName IS NULL" -> Nil
        case Some((labelName, value)) => s"$labelName = ?" -> List(setString(value))
      }
    val maybeWherePart = reduceOption(whereClauses)(_ + " AND " + _, _ ::: _)

    val maybeJoinPart = List(
      filters.stateFilter.map(createSearchSubQuery(_)("State", "label", _.label.toString)),
      filters.ingestStepFilter.map(createSearchSimplePropertiesSubQuery(_)("ingest-step", _.label.toString)),
      filters.doiRegisteredFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-registered", _.value.toString)),
      filters.doiActionFilter.map(createSearchSimplePropertiesSubQuery(_)("doi-action", _.value.toString)),
      filters.curatorFilter.map(createSearchSubQuery(_)("Curator", "datamanagerUserId", _.curator)),
      filters.isNewVersionFilter.map(createSearchSimplePropertiesSubQuery(_)("is-new-version", filter => BooleanUtils.toStringTrueFalse(filter.isNewVersion))),
      filters.curationRequiredFilter.map(createSearchSimplePropertiesSubQuery(_)("is-curation-required", filter => BooleanUtils.toStringTrueFalse(filter.curationRequired))),
      filters.curationPerformedFilter.map(createSearchSimplePropertiesSubQuery(_)("is-curation-performed", filter => BooleanUtils.toStringTrueFalse(filter.curationPerformed))),
      filters.contentTypeFilter.map(createSearchSimplePropertiesSubQuery(_)("content-type", _.value)),
    )
      .collect {
        case Some((tableName, q, resolvers)) =>
          (depositTableName: String) => s"INNER JOIN ($q) AS ${ tableName }SearchResult ON $depositTableName.depositId = ${ tableName }SearchResult.depositId" -> resolvers
      }
      .reduceOption((f, g) => (depositTableName: String) => {
        val (queryWherePartF, valuesF) = f(depositTableName)
        val (queryWherePartG, valuesG) = g(depositTableName)

        s"$queryWherePartF $queryWherePartG" -> (valuesG ::: valuesF)
      })

    val select = "SELECT DISTINCT depositorId"
    (maybeJoinPart, maybeWherePart) match {
      case (None, None) =>
        s"$select FROM Deposit;" -> Nil
      case (None, Some((queryWherePart, whereResolvers))) =>
        val query = s"$select FROM Deposit WHERE $queryWherePart;"
        query -> whereResolvers.reverse
      case (Some(queryJoinF), None) =>
        val (queryJoinPart, joinResolvers) = queryJoinF("Deposit")
        val query = s"$select FROM Deposit $queryJoinPart;"
        query -> joinResolvers.reverse
      case (Some(queryJoinF), Some((queryWherePart, whereResolvers))) =>
        val tableName = "SelectedDeposits"
        val (queryJoinPart, joinResolvers) = queryJoinF(tableName)
        val query = s"$select FROM (SELECT depositId, depositorId FROM Deposit WHERE $queryWherePart) AS $tableName $queryJoinPart;"
        query -> (whereResolvers.reverse ::: joinResolvers.reverse)
    }
  }

  def getLastModifiedDate(ids: NonEmptyList[DepositId]): (String, Seq[PrepStatementResolver]) = {
    val whereClause = ids.toList.map(_ => "?").mkString("WHERE depositId IN (", ", ", ")")
    val query = s"SELECT * FROM LastModified $whereClause;"

    query -> ids.map(setDepositId).toList
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
        case (idType, idValue) => "(identifierSchema = ? AND identifierValue = ?)" -> (setString(idType.toString) :: setString(idValue) :: Nil)
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

  lazy val storeIdentifier: String = "INSERT INTO Identifier (depositId, identifierSchema, identifierValue, timestamp) VALUES (?, ?, ?, ?);"

  lazy val storeSimpleProperty: String = "INSERT INTO SimpleProperties (depositId, key, value, timestamp) VALUES (?, ?, ?, ?);"

  lazy val storeSpringfield: String = "INSERT INTO Springfield (depositId, domain, springfield_user, collection, playmode, timestamp) VALUES (?, ?, ?, ?, ?, ?);"

  lazy val storeState: String = "INSERT INTO State (depositId, label, description, timestamp) VALUES (?, ?, ?, ?);"

  lazy val storeCurator: String = "INSERT INTO Curator (depositId, datamanagerUserId, datamanagerEmail, timestamp) VALUES (?, ?, ?, ?);"

  def deleteByDepositId(tableName: String)(ids: NonEmptyList[DepositId]): String = {
    s"DELETE FROM $tableName WHERE depositId IN (${ ids.toList.map(_ => "?").mkString(", ") });"
  }

  def deleteByDepositId(tableName: String, key: String)(ids: NonEmptyList[DepositId]): String = {
    s"DELETE FROM $tableName WHERE key = '$key' AND depositId IN (${ ids.toList.map(_ => "?").mkString(", ") });"
  }
}
