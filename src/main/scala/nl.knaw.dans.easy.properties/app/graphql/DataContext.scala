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

import nl.knaw.dans.easy.properties.app.Deleter
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesRegistration
import nl.knaw.dans.easy.properties.app.repository.Repository

import scala.concurrent.ExecutionContext

trait DataContext {

  val executionContext: ExecutionContext

  def isLoggedIn: Boolean

  def repo: Repository

  def registration: DepositPropertiesRegistration

  def deleter: Deleter
}
