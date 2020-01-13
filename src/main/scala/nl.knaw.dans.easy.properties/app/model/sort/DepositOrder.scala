package nl.knaw.dans.easy.properties.app.model.sort

object DepositOrderField extends Enumeration {
  type DepositOrderField = Value

  // @formatter:off
  val DEPOSIT_ID         : DepositOrderField = Value("depositId")
  val BAG_NAME           : DepositOrderField = Value("bagName")
  val CREATION_TIMESTAMP : DepositOrderField = Value("creationTimestamp")
  val ORIGIN             : DepositOrderField = Value("origin")
  // @formatter:on
}

case class DepositOrder(field: DepositOrderField.DepositOrderField,
                        direction: OrderDirection.OrderDirection)
