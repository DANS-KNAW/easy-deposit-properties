package nl.knaw.dans.easy.properties.app.graphql.annotated

import sangria.macros.derive.GraphQLField

class Mutation {

  @GraphQLField
  def foo(): String = ""
}
