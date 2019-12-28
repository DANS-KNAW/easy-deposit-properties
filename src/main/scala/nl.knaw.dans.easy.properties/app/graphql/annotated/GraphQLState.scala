package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.model.state.State
import sangria.relay.Node

class GraphQLState(state: State) extends Node {

  override val id: String = state.id
}
