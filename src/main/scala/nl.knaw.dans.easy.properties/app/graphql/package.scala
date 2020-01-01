package nl.knaw.dans.easy.properties.app

import sangria.schema.Context

import scala.concurrent.ExecutionContext

package object graphql {

  private[graphql] implicit def dataContextFromContext(implicit ctx: Context[DataContext, _]): DataContext = ctx.ctx

  private[graphql] implicit def executionContext(implicit ctx: DataContext): ExecutionContext = ctx.executionContext
}
