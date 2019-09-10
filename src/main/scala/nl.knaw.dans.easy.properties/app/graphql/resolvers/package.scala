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
package nl.knaw.dans.easy.properties.app.graphql

import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.QueryErrorOr
import sangria.execution.deferred.{ Fetcher, HasId }
import sangria.relay.Node

import scala.concurrent.ExecutionContext

package object resolvers {

  implicit def executionContext(implicit ctx: DataContext): ExecutionContext = ctx.executionContext

  private[resolvers] implicit def keyBasedHasId[K, V]: HasId[(K, V), K] = HasId { case (id, _) => id }
  private[resolvers] implicit val depositHasId: HasId[Deposit, DepositId] = HasId[Deposit, DepositId](_.id)
  private[resolvers] implicit def nodeHasId[T <: Node]: HasId[T, String] = HasId(_.id)

  type ByIdFetcher[T] = Fetcher[DataContext, T, T, String]

  private[resolvers] def fetchById[T <: Node](f: DataContext => Seq[String] => QueryErrorOr[Seq[T]]): ByIdFetcher[T] = {
    Fetcher.caching(f(_)(_).toFuture)
  }

  type CurrentFetcher[T] = Fetcher[DataContext, (DepositId, T), (DepositId, T), DepositId]

  private[resolvers] def fetchCurrent[T](f: DataContext => Seq[DepositId] => QueryErrorOr[Seq[(DepositId, T)]]): CurrentFetcher[T] = {
    Fetcher.caching(f(_)(_).toFuture)
  }

  type AllFetcher[T] = Fetcher[DataContext, (DepositId, Seq[T]), (DepositId, Seq[T]), DepositId]

  private[resolvers] def fetchAll[T](f: DataContext => Seq[DepositId] => QueryErrorOr[Seq[(DepositId, Seq[T])]]): AllFetcher[T] = {
    Fetcher.caching(f(_)(_).toFuture)
  }

  type DepositByIdFetcher = Fetcher[DataContext, (String, Option[Deposit]), (String, Option[Deposit]), String]

  private[resolvers] def fetchDepositsById(f: DataContext => Seq[String] => QueryErrorOr[Seq[(String, Option[Deposit])]]): DepositByIdFetcher = {
    Fetcher.caching(f(_)(_).toFuture)
  }

  private[resolvers] implicit class CollectionExtensions[T](val xs: Seq[T]) extends AnyVal {
    def distinctUntilChanged: Seq[T] = distinctUntilChanged(identity)

    def distinctUntilChanged[S](f: T => S): Seq[T] = {
      var latest: Option[S] = None
      val builder = Seq.newBuilder[T]

      for (x <- xs;
           s = f(x)) {
        latest match {
          case Some(`s`) => // do nothing
          case _ =>
            builder += x
            latest = Some(s)
        }
      }

      builder.result()
    }
  }
}
