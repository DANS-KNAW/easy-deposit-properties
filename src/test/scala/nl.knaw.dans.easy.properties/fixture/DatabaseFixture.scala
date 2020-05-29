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
package nl.knaw.dans.easy.properties.fixture

import java.sql.Connection

import better.files.File
import nl.knaw.dans.easy.properties.app.database.{ DatabaseAccess, DatabaseConfiguration, SQLErrorHandler }
import nl.knaw.dans.easy.properties.app.repository.Repository
import nl.knaw.dans.easy.properties.app.repository.sql.SQLRepo
import org.scalatest.BeforeAndAfterEach
import resource.{ constant, managed }

trait DatabaseFixture extends BeforeAndAfterEach {
  this: TestSupportFixture with FileSystemSupport =>

  private val databaseDir: File = testDir / "database"

  implicit var connection: Connection = _

  private val dbConfig = DatabaseConfiguration(
    dbDriverClassName = "org.hsqldb.jdbcDriver",
    dbUrl = s"jdbc:hsqldb:file:${ databaseDir.toString }/db"
  )
  val databaseAccess: DatabaseAccess = new DatabaseAccess(dbConfig) {
    override protected def createConnectionPool: ConnectionPool = {
      val pool = super.createConnectionPool

      val scripts = List("database/database.sql", "database/views.sql")
      for (script <- scripts) {
        managed(pool.getConnection)
          .flatMap(connection => managed(connection.createStatement))
          .and(constant(File(getClass.getClassLoader.getResource(script).toURI).contentAsString))
          .map { case (statement, query) => statement.executeUpdate(query) }
          .tried
          .recover { case e => println(e.getMessage); fail(s"could not create database for testing from script $script", e) }
      }

      connection = pool.getConnection

      pool
    }
  }
  implicit lazy val errorHandler: SQLErrorHandler = SQLErrorHandler(dbConfig)

  def repository(implicit conn: Connection): Repository = new SQLRepo()(conn, errorHandler).repository

  override def beforeEach(): Unit = {
    super.beforeEach()

    if (databaseDir.exists)
      databaseDir.delete()

    databaseAccess.initConnectionPool()
  }

  override def afterEach(): Unit = {
    managed(connection.createStatement).acquireAndGet(_.execute("SHUTDOWN"))
    connection.close()
    databaseAccess.closeConnectionPool()
    super.afterEach()
  }
}
