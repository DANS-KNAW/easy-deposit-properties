package nl.knaw.dans.easy.properties.app.graphql.annotated.types

import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType
import sangria.macros.derive.{ DocumentValue, EnumTypeDescription, deriveEnumType }
import sangria.schema.EnumType

trait GraphQLIdentifierType {

  implicit val IdentifierTypeType: EnumType[IdentifierType.Value] = deriveEnumType(
    EnumTypeDescription("The type of the identifier."),
    DocumentValue("DOI", "The doi identifier."),
    DocumentValue("URN", "The 'urn:nbn' identifier."),
    DocumentValue("FEDORA", "The Fedora identifier."),
    DocumentValue("BAG_STORE", "The bagstore identifier."),
  )
}
