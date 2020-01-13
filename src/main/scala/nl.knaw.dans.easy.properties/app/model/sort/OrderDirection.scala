package nl.knaw.dans.easy.properties.app.model.sort

import sangria.macros.derive.GraphQLDescription

import scala.language.implicitConversions

object OrderDirection extends Enumeration {
  type OrderDirection = Value

  // @formatter:off
  @GraphQLDescription("Specifies an ascending order for a given orderBy argumen.")
  val ASC : OrderDirection = Value("ASC")
  @GraphQLDescription("Specifies a descending order for a given orderBy argument")
  val DESC: OrderDirection = Value("DESC")
  // @formatter:on

  case class OrderDirectionValue(value: OrderDirection) {
    def withOrder[T](ordering: Ordering[T]): Ordering[T] = {
      value match {
        case ASC => ordering
        case DESC => ordering.reverse
      }
    }
  }
  implicit def value2OrderDirectionValue(value: OrderDirection): OrderDirectionValue = OrderDirectionValue(value)
}
