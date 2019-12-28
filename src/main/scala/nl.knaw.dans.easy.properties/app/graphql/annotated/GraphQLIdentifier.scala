package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.resolvers.IdentifierResolver
import nl.knaw.dans.easy.properties.app.model.Timestamp
import nl.knaw.dans.easy.properties.app.model.identifier.Identifier
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.Node
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("Identifier")
class GraphQLIdentifier(identifier: Identifier) extends Node {

  @GraphQLField
  @GraphQLName("type")
  @GraphQLDescription("The type of identifier.")
  val idType: IdentifierType = identifier.idType

  @GraphQLField
  @GraphQLName("value")
  @GraphQLDescription("The value of the identifier.")
  val idValue: String = identifier.idValue

  @GraphQLField
  @GraphQLDescription("The timestamp at which the identifier got added to this deposit.")
  val timestamp: Timestamp = identifier.timestamp

  override val id: String = idValue

  @GraphQLField
  @GraphQLDescription("Returns the deposit that is associated with this particular identifier.")
  def deposit(implicit ctx: Context[DataContext, GraphQLIdentifier]): DeferredValue[DataContext, Option[GraphQLDeposit]] = {
    IdentifierResolver.depositByIdentifierId(ctx.value.id)
      .map(_.map(new GraphQLDeposit(_)))
  }
}
