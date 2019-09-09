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

import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet._
import org.json4s.JsonDSL._
import org.json4s.ext.UUIDSerializer
import org.json4s.native.{ JsonMethods, Serialization }
import org.json4s.{ DefaultFormats, Formats, JValue }
import org.scalatra._
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import sangria.ast.Document
import sangria.execution._
import sangria.execution.deferred.DeferredResolver
import sangria.marshalling.json4s.native._
import sangria.parser._
import sangria.schema.Schema

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.{ higherKinds, postfixOps }

class GraphQLServlet[Ctx, Conn](schema: Schema[Ctx, Unit],
                                connGen: (Conn => Future[ActionResult]) => Future[ActionResult],
                                ctxProvider: Conn => Option[Auth] => Ctx,
                                deferredResolver: DeferredResolver[Ctx] = DeferredResolver.empty,
                                exceptionHandler: ExceptionHandler = defaultExceptionHandler,
                                middlewares: List[Middleware[Ctx]] = List.empty)
                               (implicit protected val executor: ExecutionContext)
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

    val GraphQLInput(query, variables, operation) = Serialization.read[GraphQLInput](request.body)
    QueryParser.parse(query)(DeliveryScheme.Either)
      .fold({
        case e: SyntaxError => Future.successful(BadRequest(syntaxError(e)))
        case e => Future.failed(e)
      }, execute(variables, operation, auth))
  }

  private def getAuthentication: Option[Auth] = {
    val baReq = new BasicAuthRequest(request)
    if (baReq.providesAuth && baReq.isBasicAuth)
      Option(Auth(baReq.username, baReq.password))
    else
      Option.empty
  }

  private def execute(variables: Option[String], operation: Option[String], auth: Option[Auth])(queryAst: Document): Future[ActionResult] = {
    connGen(conn => {
      Executor.execute(
        schema = schema,
        queryAst = queryAst,
        userContext = ctxProvider(conn)(auth),
        operationName = operation,
        variables = parseVariables(variables),
        deferredResolver = deferredResolver,
        exceptionHandler = exceptionHandler,
        middleware = middlewares,
      )
        .map(Serialization.writePretty(_))
        .map(Ok(_))
        .recover {
          case error: QueryAnalysisError => BadRequest(Serialization.write(error.resolveError))
          case error: ErrorWithResolver => InternalServerError(Serialization.write(error.resolveError))
        }
    })
      .recover {
        case error => InternalServerError(Serialization.write(error.getMessage))
      }
  }

  private def parseVariables(optS: Option[String]): JValue = {
    optS.filter(s => s.trim != "" && s.trim != "null")
      .map(JsonMethods.parse(_))
      .getOrElse(Nil)
  }

  private def syntaxError(error: SyntaxError): String = {
    Serialization.write {
      ("syntaxError" -> error.getMessage) ~
        ("locations" -> List(
          ("line" -> error.originalError.position.line) ~
            ("column" -> error.originalError.position.column)
        ))
    }
  }
}
