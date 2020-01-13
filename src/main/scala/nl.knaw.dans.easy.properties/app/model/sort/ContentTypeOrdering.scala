package nl.knaw.dans.easy.properties.app.model.sort

import nl.knaw.dans.easy.properties.app.model.contentType.ContentType
import nl.knaw.dans.easy.properties.app.model.contentType.ContentTypeValue.ContentTypeValue
import nl.knaw.dans.easy.properties.app.model.{ Timestamp, timestampOrdering }

object ContentTypeOrderField extends Enumeration {
  type ContentTypeOrderField = Value

  // @formatter:off
  val VALUE:     ContentTypeOrderField = Value("VALUE")
  val TIMESTAMP: ContentTypeOrderField = Value("TIMESTAMP")
  // @formatter:on
}

case class ContentTypeOrder(field: ContentTypeOrderField.ContentTypeOrderField,
                            direction: OrderDirection.OrderDirection) extends Ordering[ContentType] {
  def compare(x: ContentType, y: ContentType): Int = {
    val orderByField: Ordering[ContentType] = field match {
      case ContentTypeOrderField.VALUE =>
        Ordering[ContentTypeValue].on(_.value)
      case ContentTypeOrderField.TIMESTAMP =>
        Ordering[Timestamp].on(_.timestamp)
    }

    direction.withOrder(orderByField).compare(x, y)
  }
}
