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

import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.database.DatabaseAccess
import nl.knaw.dans.easy.properties.app.graphql.GraphQLSchema
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.app.graphql.middleware.{ Middlewares, ProfilingConfiguration }
import nl.knaw.dans.easy.properties.app.graphql.relay.RelayValidation
import nl.knaw.dans.easy.properties.app.repository.Repository
import nl.knaw.dans.easy.properties.app.repository.sql.SQLDataContext
import nl.knaw.dans.easy.properties.server.GraphQLServlet.GraphQLInput
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet.{ LogResponseBodyOnError, MaskedLogFormatter, ServletLogger }
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.ext.UUIDSerializer
import org.json4s.native.Serialization
import org.json4s.{ DefaultFormats, Formats, JValue }
import org.scalatra._
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.json4s.native._
import sangria.parser.{ DeliveryScheme, ParserConfig, QueryParser, SyntaxError }
import sangria.validation.QueryValidator

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

class GraphQLServlet(database: DatabaseAccess,
                     repository: Connection => Repository,
                     profilingThreshold: FiniteDuration,
                     expectedAuth: Auth,
                    )(implicit protected val executor: ExecutionContext)
  extends ScalatraServlet
    with CorsSupport
    with FutureSupport
    with ServletLogger
    with MaskedLogFormatter
    with LogResponseBodyOnError
    with DebugEnhancedLogging {

  private implicit val jsonFormats: Formats = new DefaultFormats {} + UUIDSerializer

  post("/") {
    contentType = "application/json"
    val auth = getAuthentication
    val profiling = if (request.queryString contains "doProfile")
                      ProfilingConfiguration(profilingThreshold).some
                    else none

    val GraphQLInput(query, variables, operation) = Serialization.read[GraphQLInput](request.body)
    val middlewares = new Middlewares(profiling)
    QueryParser.parse(query, ParserConfig.default.withoutComments)(DeliveryScheme.Either)
      .fold({
        case e: SyntaxError => Future.successful(BadRequest(syntaxError(e)))
        case e => Future.failed(e)
      }, execute(variables, operation, auth, middlewares))
  }

  private def getAuthentication: Option[Auth] = {
    val baReq = new BasicAuthRequest(request)
    if (baReq.providesAuth && baReq.isBasicAuth)
      Option(Auth(baReq.username, baReq.password))
    else
      Option.empty
  }

  private val defaultExceptionHandler = ExceptionHandler(
    onException = {
      case (_, e) =>
        logger.error(s"Exception: ${ e.getMessage }", e)
        HandledException(e.getMessage)
    },
    onViolation = {
      case (_, e) =>
        logger.error(s"Violation: ${ e.errorMessage }", e)
        HandledException(e.errorMessage)
    },
    onUserFacingError = {
      case (_, e) =>
        logger.error(s"User facing error: ${ e.getMessage }", e)
        HandledException(e.getMessage)
    },
  )

  private def execute(variables: Option[JValue], operation: Option[String], auth: Option[Auth], middlewares: Middlewares)(queryAst: Document): Future[ActionResult] = {
    database.futureTransaction(conn => {
      Executor.execute(
        schema = GraphQLSchema.schema,
        queryAst = queryAst,
        userContext = SQLDataContext(conn, repository, auth, expectedAuth),
        operationName = operation,
        variables = variables.getOrElse(JObject(Nil)),
        deferredResolver = GraphQLSchema.deferredResolver,
        exceptionHandler = defaultExceptionHandler,
        middleware = middlewares.values,
        queryValidator = QueryValidator.ruleBased(QueryValidator.allRules ::: RelayValidation.allRules),
      )
        .map(Serialization.writePretty(_))
        .map(Ok(_))
        .recover {
          case error: QueryAnalysisError => BadRequest(Serialization.writePretty(error.resolveError))
          case error: ErrorWithResolver => InternalServerError(Serialization.writePretty(error.resolveError))
        }
    })
      .recover {
        case error => InternalServerError(Serialization.writePretty(error.getMessage))
      }
  }

  private def syntaxError(error: SyntaxError): String = {
    Serialization.writePretty {
      ("syntaxError" -> error.getMessage) ~
        ("locations" -> List(
          ("line" -> error.originalError.position.line) ~
            ("column" -> error.originalError.position.column)
        ))
    }
  }
}

object GraphQLServlet {
  case class GraphQLInput(query: String, variables: Option[JValue], operationName: Option[String])
}
