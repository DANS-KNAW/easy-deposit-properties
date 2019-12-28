package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.model.curator.Curator
import sangria.relay.Node

class GraphQLCurator(curator: Curator) extends Node {

  override val id: String = curator.id

}
