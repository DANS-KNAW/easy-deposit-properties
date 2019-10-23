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
package nl.knaw.dans.easy.properties.server

import better.files.File
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.Auth
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.json4s.JsonAST.JNull
import org.json4s.JsonDSL._
import org.json4s.ext.UUIDSerializer
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.json4s.{ DefaultFormats, Formats }
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.EmbeddedJettyContainer
import org.scalatra.test.scalatest.ScalatraSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class GraphQLExamplesSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with BeforeAndAfterEach
  with EmbeddedJettyContainer
  with ScalatraSuite {

  private val authHeader = "Authorization" -> "Basic bXktdXNlcm5hbWU6bXktcGFzc3dvcmQ="
  private val servlet = new GraphQLServlet(
    database = databaseAccess,
    repository = repository(_),
    profilingThreshold = 1 minute,
    expectedAuth = Auth("my-username", "my-password"),
  )
  implicit val jsonFormats: Formats = new DefaultFormats {} + UUIDSerializer

  addServlet(servlet, "/*")

  "graphQL examples" should behave like {
    val graphqlExamplesDir = File(getClass.getResource("/graphql-examples"))

    def findJsonOutput(graphQLFile: File): File = {
      graphqlExamplesDir / graphqlExamplesDir.relativize(graphQLFile.parent).toString / s"${ graphQLFile.nameWithoutExtension }.json"
    }

    for (graphQLExample <- graphqlExamplesDir.walk()
         if graphQLExample.isRegularFile
         if graphQLExample.name.endsWith(".graphql");
         expectedJsonOutput = findJsonOutput(graphQLExample);
         relativeGraphQLPath = s"${ graphqlExamplesDir.relativize(graphQLExample.parent) }/${ graphQLExample.name }";
         relativeJsonOutputPath = s"${ graphqlExamplesDir.relativize(expectedJsonOutput.parent) }/${ expectedJsonOutput.name }") {
      it should s"check that the result of GraphQL example '$relativeGraphQLPath' is as expected in '$relativeJsonOutputPath'" in {
        assume(graphQLExample.exists, s"input file does not exist: $graphQLExample")
        assume(expectedJsonOutput.exists, s"output file does not exist: $expectedJsonOutput")

        val inputBody = compact(render("query" -> graphQLExample.contentAsString))
        val expectedOutput = writePretty(parse(expectedJsonOutput.contentAsString))

        post(uri = "/", body = inputBody.getBytes, headers = Seq(authHeader)) {
          body shouldBe expectedOutput
          status shouldBe 200
        }
      }
    }
  }

  "graphQL" should "return the updated value after doing a sequence of 'query', 'mutation', 'query'" in {
    val query =
      """query {
        |  deposit(id: "00000000-0000-0000-0000-000000000001") {
        |    state {
        |      label
        |      description
        |      timestamp
        |    }
        |  }
        |}""".stripMargin
    val mutation =
      """mutation {
        |  updateState(input: {clientMutationId: "Hello Internet", depositId: "00000000-0000-0000-0000-000000000001", label: FEDORA_ARCHIVED, description: "the deposit is archived in Fedora as easy-dataset:13", timestamp: "2019-07-02T08:15:00.000+02:00"}) {
        |    clientMutationId
        |    state {
        |      label
        |      description
        |      timestamp
        |    }
        |  }
        |}""".stripMargin

    val queryBody = compact(render("query" -> query))
    val mutationBody = compact(render("query" -> mutation))

    val expectedQueryOutput1 = writePretty {
      "data" -> {
        "deposit" -> {
          "state" -> {
            ("label" -> "ARCHIVED") ~
              ("description" -> "deposit is archived") ~
              ("timestamp" -> "2019-01-01T05:05:00.000Z")
          }
        }
      }
    }
    val expectedMutationOutput = writePretty {
      "data" -> {
        "updateState" -> {
          ("clientMutationId" -> "Hello Internet") ~
            ("state" -> {
              ("label" -> "FEDORA_ARCHIVED") ~
                ("description" -> "the deposit is archived in Fedora as easy-dataset:13") ~
                ("timestamp" -> "2019-07-02T06:15:00.000Z")
            })
        }
      }
    }
    val expectedQueryOutput2 = writePretty {
      "data" -> {
        "deposit" -> {
          "state" -> {
            ("label" -> "FEDORA_ARCHIVED") ~
              ("description" -> "the deposit is archived in Fedora as easy-dataset:13") ~
              ("timestamp" -> "2019-07-02T06:15:00.000Z")
          }
        }
      }
    }

    post(uri = "/", body = queryBody.getBytes) {
      body shouldBe expectedQueryOutput1
      status shouldBe 200
    }

    post(uri = "/", body = mutationBody.getBytes, headers = Seq(authHeader)) {
      body shouldBe expectedMutationOutput
      status shouldBe 200
    }

    post(uri = "/", body = queryBody.getBytes) {
      body shouldBe expectedQueryOutput2
      status shouldBe 200
    }
  }

  it should "use variables as they are sent with the query" in {
    val query =
      """query GetDeposit($id: UUID!) {
        |  deposit(id: $id) {
        |    state {
        |      label
        |      description
        |      timestamp
        |    }
        |  }
        |}""".stripMargin

    val json =
      ("query" -> query) ~ {
        "variables" -> {
          "id" -> "00000000-0000-0000-0000-000000000001"
        }
      }
    val queryBody = compact(render(json))

    val expectedQueryOutput = writePretty {
      "data" -> {
        "deposit" -> {
          "state" -> {
            ("label" -> "ARCHIVED") ~
              ("description" -> "deposit is archived") ~
              ("timestamp" -> "2019-01-01T05:05:00.000Z")
          }
        }
      }
    }

    post(uri = "/", body = queryBody.getBytes) {
      body shouldBe expectedQueryOutput
      status shouldBe 200
    }
  }

  it should "use variables as they are sent with the 'query', 'mutation', 'query' sequence" in {
    val query =
      """query GetDeposit($id: UUID!) {
        |  deposit(id: $id) {
        |    state {
        |      label
        |      description
        |      timestamp
        |    }
        |  }
        |}""".stripMargin
    val mutation =
      """mutation UpdateState($input: UpdateStateInput!) {
        |  updateState(input: $input) {
        |    clientMutationId
        |    state {
        |      label
        |      description
        |      timestamp
        |    }
        |  }
        |}""".stripMargin

    val queryJson =
      ("query" -> query) ~ {
        "variables" -> {
          "id" -> "00000000-0000-0000-0000-000000000001"
        }
      }
    val mutationJson =
      ("query" -> mutation) ~ {
        "variables" -> {
          "input" -> {
            ("clientMutationId" -> "Hello Internet") ~
              ("depositId" -> "00000000-0000-0000-0000-000000000001") ~
              ("label" -> "FEDORA_ARCHIVED") ~
              ("description" -> "the deposit is archived in Fedora as easy-dataset:13") ~
              ("timestamp" -> "2019-07-02T08:15:00.000+02:00")
          }
        }
      }
    val queryBody = compact(render(queryJson))
    val mutationBody = compact(render(mutationJson))

    val expectedQueryOutput1 = writePretty {
      "data" -> {
        "deposit" -> {
          "state" -> {
            ("label" -> "ARCHIVED") ~
              ("description" -> "deposit is archived") ~
              ("timestamp" -> "2019-01-01T05:05:00.000Z")
          }
        }
      }
    }
    val expectedMutationOutput = writePretty {
      "data" -> {
        "updateState" -> {
          ("clientMutationId" -> "Hello Internet") ~
            ("state" -> {
              ("label" -> "FEDORA_ARCHIVED") ~
                ("description" -> "the deposit is archived in Fedora as easy-dataset:13") ~
                ("timestamp" -> "2019-07-02T06:15:00.000Z")
            })
        }
      }
    }
    val expectedQueryOutput2 = writePretty {
      "data" -> {
        "deposit" -> {
          "state" -> {
            ("label" -> "FEDORA_ARCHIVED") ~
              ("description" -> "the deposit is archived in Fedora as easy-dataset:13") ~
              ("timestamp" -> "2019-07-02T06:15:00.000Z")
          }
        }
      }
    }

    post(uri = "/", body = queryBody.getBytes) {
      body shouldBe expectedQueryOutput1
      status shouldBe 200
    }

    post(uri = "/", body = mutationBody.getBytes, headers = Seq(authHeader)) {
      body shouldBe expectedMutationOutput
      status shouldBe 200
    }

    post(uri = "/", body = queryBody.getBytes) {
      body shouldBe expectedQueryOutput2
      status shouldBe 200
    }
  }

  it should "return an error in the body when no authentication is given for a mutation" in {
    val mutation =
      """mutation {
        |  updateState(input: {clientMutationId: "Hello Internet", depositId: "00000000-0000-0000-0000-000000000001", label: FEDORA_ARCHIVED, description: "the deposit is archived in Fedora as easy-dataset:13", timestamp: "2019-07-02T08:15:00.000+02:00"}) {
        |    clientMutationId
        |    state {
        |      label
        |      description
        |      timestamp
        |    }
        |  }
        |}""".stripMargin
    val mutationBody = compact(render("query" -> mutation))

    val expectedMutationOutput = writePretty {
      ("data" -> ("updateState" -> JNull)) ~
        ("errors" -> Seq(
          ("message" -> "you must be logged in!") ~
            ("path" -> Seq(
              "updateState",
            )) ~
            ("locations" -> Seq(
              ("line" -> 2) ~
                ("column" -> 3)
            )),
        ))
    }

    post(uri = "/", body = mutationBody.getBytes /* no authentication header */) {
      body shouldBe expectedMutationOutput

      // Yes, we don't provide authentication, but in GraphQL we still return a 200. See:
      //   * https://github.com/rmosolgo/graphql-ruby/issues/1130#issuecomment-347373937
      //   * https://www.graph.cool/docs/faq/api-eep0ugh1wa/#how-does-error-handling-work-with-graphcool
      //   * https://medium.com/@mczachurski/graphql-error-handling-17979dc571da
      status shouldBe 200
    }
  }
}
