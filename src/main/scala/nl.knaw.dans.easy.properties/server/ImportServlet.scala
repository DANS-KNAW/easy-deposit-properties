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
package nl.knaw.dans.easy.properties.server

import java.sql.Connection

import nl.knaw.dans.easy.properties.app.database.DatabaseAccess
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.app.register._
import nl.knaw.dans.easy.properties.app.repository.Repository
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet.{ LogResponseBodyOnError, PlainLogFormatter, ServletLogger }
import org.scalatra._
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest

import scala.util.Try

class ImportServlet(databaseAccess: DatabaseAccess,
                    repository: Connection => Repository,
                    expectedAuth: Auth,
                   ) extends ScalatraServlet
  with ServletLogger
  with PlainLogFormatter
  with LogResponseBodyOnError
  with DebugEnhancedLogging {

  before() {
    basicAuth()
  }

  post("/") {
    databaseAccess.doTransaction(conn => Try {
      val registrator = new DepositPropertiesRegistration(repository(conn))
      registrator.register(request.inputStream)
        .fold(recoverError, depositId => Ok(s"Deposit $depositId has been registered"))
    })
      .getOrRecover(e => {
        logger.error(e.getMessage, e)
        ServiceUnavailable(e.getMessage)
      })
  }

  private def basicAuth(): Unit = {
    val baReq = new BasicAuthRequest(request)
    if (!baReq.providesAuth) unauthenticated
    else if (!baReq.isBasicAuth) badRequest
    else if (!validate(baReq.username, baReq.password)) {
      logger.info("invalid user name password combination")
      unauthenticated
    }
  }

  private val realm = "easy-deposit-properties"

  private def unauthenticated = {
    val headers = Map("WWW-Authenticate" -> s"""Basic realm="$realm"""")
    halt(Unauthorized("Unauthenticated", headers))
  }

  private def badRequest = {
    logger.info(s"${ request.getMethod } did not have basic authentication")
    halt(BadRequest("Bad Request"))
  }

  private def validate(userName: String, password: String): Boolean = {
    userName == expectedAuth.username && password == expectedAuth.password
  }

  private def recoverError(err: ImportError): ActionResult = {
    err match {
      case ReadImportError(msg, _) => BadRequest(msg)
      case e: ValidationImportErrors => BadRequest(e.getMessage)
      case e: DepositAlreadyExistsError => Conflict(e.getMessage)
      case DBImportError(msg, cause) =>
        logger.error(msg, cause)
        InternalServerError(msg)
    }
  }
}
