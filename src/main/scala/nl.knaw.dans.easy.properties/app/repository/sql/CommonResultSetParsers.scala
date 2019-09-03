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

import java.sql.{ Connection, ResultSet }
import java.util.UUID

import cats.data.NonEmptyList
import cats.instances.either._
import cats.instances.option._
import cats.instances.stream._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.traverse._
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, Origin, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.{ InvalidValueError, QueryErrorOr }
import org.joda.time.{ DateTime, DateTimeZone }
import resource.managed
import sangria.relay.Node

import scala.language.higherKinds

private[sql] trait CommonResultSetParsers {

  private def validateId(s: String): Either[InvalidValueError, String] = {
    Either.catchOnly[NumberFormatException](s.toLong)
      .leftMap(_ => InvalidValueError(s"invalid id '$s'"))
      .map(_ => s)
  }

  private[sql] def parseDepositId(s: String): Either[InvalidValueError, DepositId] = {
    Either.catchOnly[IllegalArgumentException] { UUID.fromString(s) }
      .leftMap(_ => InvalidValueError(s"Invalid depositId value: '$s'"))
  }

  private[sql] def parseDateTime(t: java.sql.Timestamp, timeZone: DateTimeZone): Either[InvalidValueError, Timestamp] = {
    Either.catchOnly[IllegalArgumentException] { new DateTime(t, timeZone) }
      .leftMap(_ => InvalidValueError(s"Invalid timestamp value: '$t'"))
  }

  private[sql] def parseEnumValue[E <: Enumeration](enum: E, enumDescription: String)(s: String) = {
    Either.catchOnly[NoSuchElementException] { enum.withName(s) }
      .leftMap(_ => InvalidValueError(s"Invalid $enumDescription value: '$s'"))
  }

  private[sql] def parseDeposit(resultSet: ResultSet): Either[InvalidValueError, Deposit] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      bagName = Option(resultSet.getString("bagName"))
      creationTimestamp <- parseDateTime(resultSet.getTimestamp("creationTimestamp", timeZone), timeZone)
      depositorId = resultSet.getString("depositorId")
      origin = Origin.withName(resultSet.getString("origin"))
    } yield Deposit(depositId, bagName, creationTimestamp, depositorId, origin)
  }

  private def extractResults[T, X](parseResult: ResultSet => Either[InvalidValueError, T])
                                  (collectResults: Stream[T] => Seq[X])
                                  (result: ResultSet): QueryErrorOr[Seq[X]] = {
    Stream.continually(result.next())
      .takeWhile(b => b)
      .traverse[Either[InvalidValueError, ?], T](_ => parseResult(result))
      .map(collectResults)
  }

  private[sql] def executeQuery[T, R](parseResult: ResultSet => Either[InvalidValueError, T])
                                     (collectResults: Stream[T] => Seq[R])
                                     (queryAndValues: (String, Seq[PrepStatementResolver]))
                                     (implicit connection: Connection): QueryErrorOr[Seq[R]] = {
    val (query, values) = queryAndValues
    val resultSet = for {
      prepStatement <- managed(connection.prepareStatement(query))
      _ = values.zipWithIndex.foreach { case (filler, index) => filler(prepStatement, index + 1) }
      resultSet <- managed(prepStatement.executeQuery())
    } yield resultSet

    resultSet.map(extractResults(parseResult)(collectResults))
      .either
      .either
      .leftMap(InvalidValueError(_, query))
      .flatMap(identity)
  }

  private[sql] def executeGetById[T <: Node](extract: ResultSet => Either[InvalidValueError, T])
                                            (queryGen: NonEmptyList[String] => (String, Seq[PrepStatementResolver]))
                                            (ids: Seq[String])
                                            (implicit connection: Connection): QueryErrorOr[Seq[(String, Option[T])]] = {
    def collectResults(stream: Stream[T]): Seq[(String, Option[T])] = {
      val results = stream.toList
        .groupBy(_.id)
        .flatMap { case (id, ss) =>
          assert(ss.size == 1)
          ss.headOption.tupleLeft(id)
        }
      ids.map(id => id -> results.get(id))
    }

    NonEmptyList.fromList(ids.toList)
      .map(_
        .traverse[Either[InvalidValueError, ?], String](validateId)
        .map(queryGen)
        .flatMap(executeQuery(extract)(collectResults))
      )
      .getOrElse(Seq.empty.asRight)
  }

  private[sql] def executeGetCurrent[T](extract: ResultSet => Either[InvalidValueError, (DepositId, T)])
                                       (queryGen: NonEmptyList[DepositId] => (String, Seq[PrepStatementResolver]))
                                       (ids: Seq[DepositId])
                                       (implicit connection: Connection): QueryErrorOr[Seq[(DepositId, Option[T])]] = {
    def collectResults(stream: Stream[(DepositId, T)]): Seq[(DepositId, Option[T])] = {
      val results = stream.toList
        .groupBy { case (depositId, _) => depositId }
        .flatMap {
          case (id, ss) =>
            assert(ss.size == 1)
            ss.headOption.map { case (_, t) => t }.tupleLeft(id)
        }
      ids.map(id => id -> results.get(id))
    }

    NonEmptyList.fromList(ids.toList)
      .map(queryGen)
      .map(executeQuery(extract)(collectResults))
      .getOrElse(Seq.empty.asRight)
  }

  private[sql] def executeGetAll[T](extract: ResultSet => Either[InvalidValueError, (DepositId, T)])
                                   (queryGen: NonEmptyList[DepositId] => (String, Seq[PrepStatementResolver]))
                                   (ids: Seq[DepositId])
                                   (implicit connection: Connection): QueryErrorOr[Seq[(DepositId, Seq[T])]] = {
    def collectResults(stream: Stream[(DepositId, T)]): Seq[(DepositId, Seq[T])] = {
      val results = stream.toList
        .groupBy { case (depositId, _) => depositId }
        .map {
          case (id, ss) => id -> ss.map { case (_, state) => state }
        }
      ids.map(id => id -> results.getOrElse(id, Seq.empty))
    }

    NonEmptyList.fromList(ids.toList)
      .map(queryGen)
      .map(executeQuery(extract)(collectResults))
      .getOrElse(Seq.empty.asRight)
  }

  private[sql] def executeGetDepositById(extract: ResultSet => Either[InvalidValueError, (String, Deposit)])
                                        (queryGen: NonEmptyList[String] => (String, Seq[PrepStatementResolver]))
                                        (ids: Seq[String])
                                        (implicit connection: Connection): QueryErrorOr[Seq[(String, Option[Deposit])]] = {
    def collectResults(stream: Stream[(String, Deposit)]): Seq[(String, Option[Deposit])] = {
      val results = stream.toList
        .groupBy { case (stateId, _) => stateId }
        .flatMap { case (id, ss) =>
          assert(ss.size == 1)
          ss.headOption.map { case (_, deposit) => deposit }.tupleLeft(id)
        }
      ids.map(id => id -> results.get(id))
    }

    NonEmptyList.fromList(ids.toList)
      .map(_
        .traverse[Either[InvalidValueError, ?], String](validateId)
        .map(queryGen)
        .flatMap(executeQuery(extract)(collectResults))
      )
      .getOrElse(Seq.empty.asRight)
  }
}
