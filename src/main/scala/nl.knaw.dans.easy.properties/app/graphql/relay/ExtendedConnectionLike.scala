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
package nl.knaw.dans.easy.properties.app.graphql.relay

import sangria.relay.{ ConnectionLike, Edge, PageInfo }

import scala.annotation.implicitNotFound
import scala.language.higherKinds

@implicitNotFound("Type ${T} can't be used as a ExtendedConnection. Please consider defining implicit instance of nl.dans.knaw.easy.properties.app.relay.ExtendedConnectionLike for type ${T} or extending nl.dans.knaw.easy.properties.app.relay.ExtendedConnection trait.")
private[relay] trait ExtendedConnectionLike[T[_], Val, E <: Edge[Val]] extends ConnectionLike[T, PageInfo, Val, E] {
  def totalCount(conn: T[Val]): Int
}

private[relay] object ExtendedConnectionLike {
  private object ExtendedConnectionIsExtendedConnectionLike extends ExtendedConnectionLike[ExtendedConnection, Any, Edge[Any]] {
    override def pageInfo(conn: ExtendedConnection[Any]): PageInfo = conn.pageInfo

    override def edges(conn: ExtendedConnection[Any]): Seq[Edge[Any]] = conn.edges

    override def totalCount(conn: ExtendedConnection[Any]): Int = conn.totalCount
  }

  implicit def extendedConnectionIsExtendedConnectionLike[E <: Edge[Val], P <: PageInfo, Val, T[_]]: ExtendedConnectionLike[T, Val, E] =
    ExtendedConnectionIsExtendedConnectionLike.asInstanceOf[ExtendedConnectionLike[T, Val, E]]
}
