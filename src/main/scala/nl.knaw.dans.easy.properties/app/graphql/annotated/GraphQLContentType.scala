package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.model.contentType.ContentType
import sangria.relay.Node

class GraphQLContentType(contentType: ContentType) extends Node {

  override val id: String = contentType.id

}
