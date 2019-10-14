package nl.knaw.dans.easy.properties.app.register

import java.util.UUID

import better.files.StringOps
import cats.scalatest.{ EitherMatchers, EitherValues }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseFixture, FileSystemSupport, ImportTestData, TestSupportFixture }

class DepositPropertiesRegistrationSpec extends TestSupportFixture
  with EitherValues
  with EitherMatchers
  with FileSystemSupport
  with DatabaseFixture
  with ImportTestData {

  val registration = new DepositPropertiesRegistration(databaseAccess, repository(_))

  "register" should "import the data from the deposit.properties in the inputstream" in {
    val is = validDepositPropertiesBody.inputStream
    // @formatter:off
    val DepositProperties(
      deposit, Some(state), Some(ingestStep), Seq(fedora, doi, urn, bagstore), Some(doiAction),
      Some(doiRegistered), Some(curation), Some(springfield), Some(contentType),
    ) = validDepositProperties
    // @formatter:on
    val depositId = deposit.id

    registration.register(is).value shouldBe depositId

    repository.deposits.find(Seq(depositId)).value should contain only deposit
    repository.states.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only state.toOutput("0")
    repository.ingestSteps.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only ingestStep.toOutput("0")
    repository.identifiers.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only(
      fedora.toOutput("0"),
      doi.toOutput("1"),
      urn.toOutput("2"),
      bagstore.toOutput("3"),
    )
    repository.doiAction.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only doiAction
    repository.doiRegistered.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only doiRegistered
    repository.curation.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only curation.toOutput("0")
    repository.springfield.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only springfield.toOutput("0")
    repository.contentType.getAll(Seq(depositId)).value.toMap.apply(depositId) should contain only contentType.toOutput("3")
  }

  it should "import the minimal example" in {
    val is = minimalDepositPropertiesBody.inputStream
    val DepositProperties(deposit, None, None, Seq(), None, None, None, None, None) = minimalDepositProperties
    val depositId = deposit.id

    registration.register(is).value shouldBe depositId

    repository.deposits.find(Seq(depositId)).value should contain only deposit
    repository.states.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.ingestSteps.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.identifiers.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.doiAction.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.doiRegistered.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.curation.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.springfield.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
    repository.contentType.getAll(Seq(depositId)).value.toMap.apply(depositId) shouldBe empty
  }

  it should "not import data into the database when the deposit.properties is not valid" in {
    val invalidProps = """creation.timestamp = 2019-01-01T00:00:00.000Z""".stripMargin.inputStream

    inside(registration.register(invalidProps).leftValue) {
      case ValidationImportErrors(depositIdError :: userIdError :: originError :: Nil) =>
        depositIdError shouldBe PropertyNotFoundError("depositId")
        userIdError shouldBe PropertyNotFoundError("depositor.userId")
        originError shouldBe PropertyNotFoundError("deposit.origin")
    }

    repository.deposits.getAll.value shouldBe empty
  }

  it should "not import data into the database when the deposit is already registered in the database" in {
    val props1 =
      """depositId = 9d507261-3b79-22e7-86d0-6fb9417d930d
        |creation.timestamp = 2019-01-01T01:01:01.000Z
        |depositor.userId = user001
        |deposit.origin = SWORD2""".stripMargin.inputStream
    val props2 =
      """depositId = 9d507261-3b79-22e7-86d0-6fb9417d930d
        |creation.timestamp = 2019-02-02T02:02:02.000Z
        |depositor.userId = user002
        |deposit.origin = SMD""".stripMargin.inputStream
    
    registration.register(props1) shouldBe right
    registration.register(props2).leftValue shouldBe DepositAlreadyExistsError(UUID.fromString("9d507261-3b79-22e7-86d0-6fb9417d930d"))
  }
}
