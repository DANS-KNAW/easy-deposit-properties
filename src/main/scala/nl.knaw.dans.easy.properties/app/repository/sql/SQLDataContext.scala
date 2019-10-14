package nl.knaw.dans.easy.properties.app.repository.sql

import java.sql.Connection

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.app.register.DepositPropertiesRegistration
import nl.knaw.dans.easy.properties.app.repository.Repository

import scala.concurrent.ExecutionContext

case class SQLDataContext(private val connection: Connection,
                          private val repoGen: Connection => Repository,
                          private val auth: Option[Auth],
                          private val expectedAuth: Auth,
                         )(implicit val executionContext: ExecutionContext) extends DataContext {

  def isLoggedIn: Boolean = auth contains expectedAuth

  lazy val repo: Repository = repoGen(connection)
  lazy val registration: DepositPropertiesRegistration = new DepositPropertiesRegistration(repo)
}
