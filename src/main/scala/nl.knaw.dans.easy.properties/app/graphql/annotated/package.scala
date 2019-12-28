package nl.knaw.dans.easy.properties.app.graphql

import sangria.schema.Context

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

package object annotated {

  implicit def dataContextFromContext(implicit ctx: Context[DataContext, _]): DataContext = ctx.ctx

  implicit def executionContext(implicit ctx: DataContext): ExecutionContext = ctx.executionContext
}
