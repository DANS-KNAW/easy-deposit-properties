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

import java.sql.Connection

import nl.knaw.dans.easy.properties.app.Deleter
import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.app.graphql.types.{ Mutation, Query }
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesRegistration
import nl.knaw.dans.easy.properties.app.repository.Repository

import scala.concurrent.ExecutionContext

case class SQLDataContext(private val connection: Connection,
                          private val repoGen: Connection => Repository,
                          private val auth: Option[Auth],
                          private val expectedAuth: Auth,
                         )(override implicit val executionContext: ExecutionContext) extends DataContext {

  override def isLoggedIn: Boolean = auth contains expectedAuth

  override val query: Query = new Query
  override val mutation: Mutation = new Mutation
  override lazy val repo: Repository = repoGen(connection)
  override lazy val registration: DepositPropertiesRegistration = new DepositPropertiesRegistration(repo)
  override lazy val deleter: Deleter = new Deleter(repo)
}
