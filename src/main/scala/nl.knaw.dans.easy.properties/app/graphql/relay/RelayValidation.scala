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

import nl.knaw.dans.easy.properties.app.graphql.relay.RelayViolations._
import sangria.ast._
import sangria.validation.{ ValidationContext, ValidationRule }

object RelayValidation {

  val allRules: List[ValidationRule] = List(
    new ConnectionHasEitherFirstOrLast,
  )

  class ConnectionHasEitherFirstOrLast extends ValidationRule {
    override def visitor(ctx: ValidationContext): AstValidatingVisitor = new AstValidatingVisitor {

      private var visitedNodes = List.empty[AstNode]

      override def onEnter: ValidationVisit = {
        case f @ Field(_, "edges", _, _, _, _, _, _) =>
          val stackHead = visitedNodes.headOption
          visitedNodes = f :: visitedNodes
          stackHead
            .fold(AstVisitorCommand.RightContinue) {
              case Field(_, fieldName, arguments, _, _, _, _, pos) =>
                arguments.map(_.name).filter {
                  case "first" | "last" => true
                  case _ => false
                }.toSet.toList match {
                  case Nil =>
                    Left(Vector(MissingFirstOrLastArgumentViolation(fieldName, ctx.sourceMapper, pos.toList)))
                  case "first" :: Nil | "last" :: Nil =>
                    AstVisitorCommand.RightContinue
                  case _ =>
                    Left(Vector(BothFirstOrLastArgumentViolation(fieldName, ctx.sourceMapper, pos.toList)))
                }
              case _ => AstVisitorCommand.RightContinue
            }
        case astNode =>
          visitedNodes = astNode :: visitedNodes
          AstVisitorCommand.RightContinue
      }

      override def onLeave: ValidationVisit = {
        case d: Document =>
          visitedNodes match {
            case `d` :: Nil => AstVisitorCommand.RightContinue
            case _ => throw new Exception(s"unexpected non-empty stack on exiting $d")
          }
        case astNode if visitedNodes.isEmpty =>
          throw new Exception(s"unexpected empty stack on exiting $astNode")
        case _ =>
          visitedNodes = visitedNodes.tail
          AstVisitorCommand.RightContinue
      }
    }
  }
}
