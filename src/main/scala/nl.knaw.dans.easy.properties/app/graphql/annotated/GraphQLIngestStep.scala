package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.model.ingestStep.IngestStep
import sangria.relay.Node

class GraphQLIngestStep(ingestStep: IngestStep) extends Node {

  override val id: String = ingestStep.id

}
