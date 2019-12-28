package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.model.springfield.Springfield
import sangria.relay.Node

class GraphQLSpringfield(springfield: Springfield) extends Node {

  override val id: String = springfield.id

}
