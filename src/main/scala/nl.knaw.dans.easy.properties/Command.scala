/**
 * Copyright (C) 2019 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.properties

import java.util.concurrent.Executors

import better.files.File
import nl.knaw.dans.easy.DataciteService
import nl.knaw.dans.easy.properties.app.database.{ DatabaseAccess, SQLErrorHandler }
import nl.knaw.dans.easy.properties.app.legacyImport.{ ImportProps, Interactor }
import nl.knaw.dans.easy.properties.app.repository.sql.SQLRepo
import nl.knaw.dans.easy.properties.server._
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }
import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration(File(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }

  runSubcommand()
    .doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }

  private def runSubcommand(): Try[FeedBackMessage] = {
    commandLine.subcommand
      .collect {
        case loadProps @ commandLine.loadProps => runLoadProps(loadProps.properties(), loadProps.doUpdate())
        case commandLine.runService => runAsService()
      }
      .getOrElse(Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }")))
  }

  private def runLoadProps(propsFile: File, doUpdate: Boolean): Try[FeedBackMessage] = {
    val database = new DatabaseAccess(configuration.databaseConfig)
    implicit val sqlErrorHandler: SQLErrorHandler = SQLErrorHandler(configuration.databaseConfig)

    for {
      _ <- database.initConnectionPool()
      msg <- database.doTransaction(implicit connection => Try {
        new ImportProps(
          repository = new SQLRepo().repository,
          interactor = new Interactor,
          datacite = new DataciteService(configuration.dataciteConfig),
          testMode = !doUpdate,
        )
          .loadDepositProperties(propsFile)
          .fold(_.msg, identity)
      })
        .doIfSuccess(_ => database.closeConnectionPool())
        .doIfFailure {
          case _ => database.closeConnectionPool().unsafeGetOrThrow
        }
    } yield msg
  }

  private def runAsService(): Try[FeedBackMessage] = Try {
    val database = new DatabaseAccess(configuration.databaseConfig)
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(16))
    implicit val sqlErrorHandler: SQLErrorHandler = SQLErrorHandler(configuration.databaseConfig)

    val service = new EasyDepositPropertiesService(configuration.serverPort, Map(
      "/" -> new EasyDepositPropertiesServlet(configuration.version),
      "/register" -> new ImportServlet(
        databaseAccess = database,
        repository = implicit conn => new SQLRepo().repository,
        expectedAuth = configuration.auth,
      ),
      "/graphql" -> new GraphQLServlet(
        database = database,
        repository = implicit conn => new SQLRepo().repository,
        profilingThreshold = configuration.profilingThreshold,
        expectedAuth = configuration.auth,
      ),
      "/graphiql" -> new GraphiQLServlet("/graphql"),
    ))
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        service.stop().unsafeGetOrThrow
        database.closeConnectionPool().unsafeGetOrThrow
        service.destroy().unsafeGetOrThrow
      }
    })

    database.initConnectionPool().unsafeGetOrThrow
    service.start().unsafeGetOrThrow
    Thread.currentThread.join()
    "Service terminated normally."
  }
}
