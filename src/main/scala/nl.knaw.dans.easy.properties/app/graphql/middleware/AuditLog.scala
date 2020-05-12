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
package nl.knaw.dans.easy.properties.app.graphql.middleware

import cats.syntax.option._
import org.json4s.JsonAST.{ JObject, JValue }
import org.json4s.ext.UUIDSerializer
import org.json4s.native.Serialization
import org.json4s.{ DefaultFormats, Formats }
import org.slf4j.LoggerFactory
import sangria.ast.{ Field, OperationDefinition }
import sangria.execution.{ Middleware, MiddlewareQueryContext }
import sangria.renderer.QueryRenderer.Pretty
import sangria.renderer.{ Indent, QueryRenderer }

object AuditLog extends Middleware[Any] {

  private val auditLog = LoggerFactory.getLogger(AuditLog.getClass)
  private implicit val jsonFormats: Formats = new DefaultFormats {} + UUIDSerializer

  override type QueryVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[Any, _, _]): Unit = {
    val variables = context.variables.asInstanceOf[JValue]

    for (operation <- context.queryAst.operations.values)
      auditLog.info(operationDefinitionToString(variables)(operation))
  }

  private def operationDefinitionToString(variables: JValue)(opDef: OperationDefinition): String = {
    val opType = QueryRenderer.renderOpType(opDef.operationType)
    val selections = opDef.selections
      .flatMap {
        case Field(_, name, arguments, _, _, _, _, _) =>
          s"$name(${ arguments.map(_.renderCompact).mkString(",") })".some
        case _ => None
      }
      .mkString(",")
    val funcDef = (opDef.name, opDef.variables) match {
      case (None, Vector()) => " "
      case (None, vs) => " " + QueryRenderer.renderVarDefs(vs, Indent(Pretty, 0, 0), Pretty)
      case (Some(name), Vector()) => " " + name + " "
      case (Some(name), vs) => " " + name + QueryRenderer.renderVarDefs(vs, Indent(Pretty, 0, 0), Pretty)
    }
    val variableValues = variables match {
      case JObject(Nil) => ""
      case _ => " with variables " + Serialization.write(variables)
    }

    s"$opType$funcDef{ $selections }$variableValues"
  }

  override def afterQuery(queryVal: Unit, context: MiddlewareQueryContext[Any, _, _]): Unit = {}
}
