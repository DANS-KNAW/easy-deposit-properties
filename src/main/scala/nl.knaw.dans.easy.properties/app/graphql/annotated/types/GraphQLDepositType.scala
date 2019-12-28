package nl.knaw.dans.easy.properties.app.graphql.annotated.types

import nl.knaw.dans.easy.properties.app.model.Origin
import sangria.macros.derive.{ DocumentValue, EnumTypeDescription, deriveEnumType }
import sangria.schema.EnumType

trait GraphQLDepositType {

  implicit lazy val OriginType: EnumType[Origin.Value] = deriveEnumType(
    EnumTypeDescription("The origin of the deposit."),
    DocumentValue("SWORD2", "easy-sword2"),
    DocumentValue("API", "easy-deposit-api"),
    DocumentValue("SMD", "easy-split-multi-deposit"),
  )
}
