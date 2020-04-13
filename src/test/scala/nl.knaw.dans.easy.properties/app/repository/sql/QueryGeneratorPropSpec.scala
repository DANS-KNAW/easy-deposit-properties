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
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.contentType.DepositContentTypeFilter
import nl.knaw.dans.easy.properties.app.model.curator.DepositCuratorFilter
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ DepositIngestStepFilter, IngestStepLabel }
import nl.knaw.dans.easy.properties.app.model.sort.{ DepositOrder, DepositOrderField, OrderDirection }
import nl.knaw.dans.easy.properties.app.model.state.{ DepositStateFilter, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ AtTime, Between, DepositCurationPerformedFilter, DepositCurationRequiredFilter, DepositDoiActionFilter, DepositDoiRegisteredFilter, DepositIsNewVersionFilter, DoiAction, EarlierThan, LaterThan, NotBetween, Origin, SeriesFilter, TimeFilter, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.{ DepositFilters, DepositorIdFilters }
import org.joda.time.DateTime
import org.scalacheck.Arbitrary._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class QueryGeneratorPropSpec extends AnyPropSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  def genFromEnum[E <: Enumeration](enum: E): Arbitrary[E#Value] = Arbitrary {
    Gen.oneOf(enum.values.toList)
  }

  private def genDepositFilter[E <: Enumeration, T](valueEnum: E)(f: (E#Value, SeriesFilter) => T): Arbitrary[T] = Arbitrary {
    for {
      value <- genFromEnum(valueEnum).arbitrary
      filter <- arbitrarySeriesFilter.arbitrary
    } yield f(value, filter)
  }

  private def genDepositFilter[T: Arbitrary, S](f: (T, SeriesFilter) => S): Arbitrary[S] = Arbitrary {
    arbitrary[(T, SeriesFilter)].map(f.tupled)
  }

  implicit val arbitrarySeriesFilter: Arbitrary[SeriesFilter] = genFromEnum(SeriesFilter)
  implicit val arbitraryIdentifierType: Arbitrary[IdentifierType] = genFromEnum(IdentifierType)
  implicit val arbitraryOrigin: Arbitrary[Origin.Value] = genFromEnum(Origin)

  implicit val arbitraryStateFilter: Arbitrary[DepositStateFilter] = genDepositFilter(StateLabel)(DepositStateFilter)
  implicit val arbitraryIngestStepFilter: Arbitrary[DepositIngestStepFilter] = genDepositFilter(IngestStepLabel)(DepositIngestStepFilter)
  implicit val arbitraryDoiRegisteredFilter: Arbitrary[DepositDoiRegisteredFilter] = genDepositFilter(DepositDoiRegisteredFilter)
  implicit val arbitraryDoiActionFilter: Arbitrary[DepositDoiActionFilter] = genDepositFilter(DoiAction)(DepositDoiActionFilter)
  implicit val arbitraryCurationFilter: Arbitrary[DepositCuratorFilter] = genDepositFilter(DepositCuratorFilter)
  implicit val arbitraryIsNewVersionFilter: Arbitrary[DepositIsNewVersionFilter] = genDepositFilter(DepositIsNewVersionFilter)
  implicit val arbitraryCurationRequiredFilter: Arbitrary[DepositCurationRequiredFilter] = genDepositFilter(DepositCurationRequiredFilter)
  implicit val arbitraryCurationPerformedFilter: Arbitrary[DepositCurationPerformedFilter] = genDepositFilter(DepositCurationPerformedFilter)
  implicit val arbitraryContentTypeFilter: Arbitrary[DepositContentTypeFilter] = genDepositFilter(DepositContentTypeFilter)
  implicit val arbitraryDepositOrder: Arbitrary[DepositOrder] = Arbitrary {
    for {
      field <- genFromEnum(DepositOrderField).arbitrary
      direction <- genFromEnum(OrderDirection).arbitrary
    } yield DepositOrder(field, direction)
  }

  implicit val arbitraryTimestamp: Arbitrary[Timestamp] = Arbitrary {
    Gen.calendar.map(c => new DateTime(c.getTime))
  }
  implicit val arbitraryTimeFilter: Arbitrary[TimeFilter] = Arbitrary {
    Gen.oneOf[TimeFilter](
      arbitraryTimestamp.arbitrary.map(EarlierThan),
      arbitraryTimestamp.arbitrary.map(LaterThan),
      arbitraryTimestamp.arbitrary.map(AtTime),
      for {
        earlier <- arbitraryTimestamp.arbitrary
        later <- arbitraryTimestamp.arbitrary
      } yield Between(earlier, later),
      for {
        earlier <- arbitraryTimestamp.arbitrary
        later <- arbitraryTimestamp.arbitrary
      } yield NotBetween(earlier, later),
    )
  }

  implicit val arbitraryDepositFilters: Arbitrary[DepositFilters] = Arbitrary {
    arbitrary[(
      Option[String],
        Option[String],
        Option[DepositStateFilter],
        Option[DepositIngestStepFilter],
        Option[DepositDoiRegisteredFilter],
        Option[DepositDoiActionFilter],
        Option[DepositCuratorFilter],
        Option[DepositIsNewVersionFilter],
        Option[DepositCurationRequiredFilter],
        Option[DepositCurationPerformedFilter],
        Option[DepositContentTypeFilter],
        Option[Origin.Origin],
        Option[TimeFilter],
        Option[DepositOrder],
      )]
      .map(DepositFilters.tupled)
  }

  implicit val arbitraryDepositorFilters: Arbitrary[DepositorIdFilters] = Arbitrary {
    arbitrary[(
      Option[DepositStateFilter],
        Option[DepositIngestStepFilter],
        Option[DepositDoiRegisteredFilter],
        Option[DepositDoiActionFilter],
        Option[DepositCuratorFilter],
        Option[DepositIsNewVersionFilter],
        Option[DepositCurationRequiredFilter],
        Option[DepositCurationPerformedFilter],
        Option[DepositContentTypeFilter],
        Option[Origin.Origin],
      )]
      .map(DepositorIdFilters.tupled)
  }

  implicit def arbitraryNonEmptyList[T: Arbitrary]: Arbitrary[NonEmptyList[T]] = Arbitrary {
    arbitrary[(T, List[T])].map { case (t, ts) => NonEmptyList(t, ts) }
  }

  def testNumberOfQuestionMarks[T: Arbitrary](queryGen: T => (String, Seq[PrepStatementResolver])): Unit = {
    forAll(arbitrary[T])(t => {
      val (query, values) = queryGen(t)
      query.count(_ == '?') shouldBe values.size
    })
  }

  property("findDeposits") {
    testNumberOfQuestionMarks(QueryGenerator.findDeposits)
  }

  property("searchDeposits") {
    testNumberOfQuestionMarks(QueryGenerator.searchDeposits)
  }

  property("searchDepositors") {
    testNumberOfQuestionMarks(QueryGenerator.searchDepositors)
  }

  property("getLastModifiedDate") {
    testNumberOfQuestionMarks(QueryGenerator.getLastModifiedDate)
  }

  property("getElementsById") {
    testNumberOfQuestionMarks(QueryGenerator.getElementsById("table", "idColumn"))
  }

  property("getCurrentElementByDepositId") {
    testNumberOfQuestionMarks(QueryGenerator.getCurrentElementByDepositId("table"))
  }

  property("getAllElementsByDepositId") {
    testNumberOfQuestionMarks(QueryGenerator.getAllElementsByDepositId("table"))
  }

  property("getDepositsById") {
    testNumberOfQuestionMarks(QueryGenerator.getDepositsById("table", "idColumn"))
  }

  property("getIdentifierByDepositIdAndType") {
    testNumberOfQuestionMarks(QueryGenerator.getIdentifierByDepositIdAndType)
  }

  property("getIdentifierByTypeAndValue") {
    testNumberOfQuestionMarks(QueryGenerator.getIdentifierByTypeAndValue)
  }

  property("getSimplePropsElementsById") {
    testNumberOfQuestionMarks(QueryGenerator.getSimplePropsElementsById("key"))
  }

  property("getSimplePropsCurrentElementByDepositId") {
    testNumberOfQuestionMarks(QueryGenerator.getSimplePropsCurrentElementByDepositId("key"))
  }

  property("getSimplePropsAllElementsByDepositId") {
    testNumberOfQuestionMarks(QueryGenerator.getSimplePropsAllElementsByDepositId("key"))
  }

  property("getSimplePropsDepositsById") {
    testNumberOfQuestionMarks(QueryGenerator.getSimplePropsDepositsById("key"))
  }
}
