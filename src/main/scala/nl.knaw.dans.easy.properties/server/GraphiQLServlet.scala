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

import org.scalatra.{ Ok, ScalatraServlet }

class GraphiQLServlet(backendPath: String) extends ScalatraServlet {

  get("/") {
    /*
     * this html response was copied from
     *     https://github.com/graphql/graphiql/blob/master/example/index.html
     *
     * after which the following changes were made:
     *   - add meta elements, title and favicon.ico to <head>
     *   - upgrade graphiql, react and react-dom libraries
     *   - removed unnecessary comments
     *   - add property 'schema: undefined' to the rendered GraphiQL Component
     *   - add property 'response: parameters.response' to the rendered GraphiQL Component
     */
    contentType = "text/html"
    Ok {
      s"""|<!DOCTYPE html>
          |<html lang="en">
          |<head>
          |    <meta charset="utf-8">
          |    <meta http-equiv="X-UA-Compatible" content="IE=edge">
          |    <meta name="viewport" content="width=device-width, initial-scale=1">
          |
          |    <title>GraphiQL</title>
          |
          |    <link rel="shortcut icon" type="image/x-icon" href="https://easy.dans.knaw.nl/ui/images/lay-out/favicon.ico">
          |    <link href="//cdn.jsdelivr.net/npm/graphiql@0.13.0/graphiql.css" rel="stylesheet" />
          |
          |    <script src="//cdn.jsdelivr.net/es6-promise/4.0.5/es6-promise.auto.min.js"></script>
          |    <script src="//cdn.jsdelivr.net/fetch/0.9.0/fetch.min.js"></script>
          |    <script src="//cdn.jsdelivr.net/npm/react@16.8.6/umd/react.production.min.js"></script>
          |    <script src="//cdn.jsdelivr.net/npm/react-dom@16.8.6/umd/react-dom.production.min.js"></script>
          |    <script src="//cdn.jsdelivr.net/npm/graphiql@0.13.0/graphiql.min.js"></script>
          |
          |    <style>
          |      body {
          |        height: 100%;
          |        margin: 0;
          |        width: 100%;
          |        overflow: hidden;
          |      }
          |      #graphiql {
          |        height: calc(100vh - 72px); /* 72px is the height of the `authentication` div */
          |      }
          |      .authentication {
          |        background: linear-gradient(#f7f7f7, #e2e2e2);
          |        border-bottom: 1px solid #d0d0d0;
          |        padding: 7px 14px 6px;
          |        font-size: 24px;
          |      }
          |      .authentication label {
          |        display: inline-block;
          |        width: 100px;
          |      }
          |      .authentication input {
          |        padding-left: 3px;
          |        font-size: 20px;
          |        width: 50%;
          |      }
          |    </style>
          |</head>
          |<body>
          |    <div class="authentication">
          |      <div>
          |        <label for="username">Username</label>
          |        <input type="text" id="username" placeholder="Username">
          |      </div>
          |      <div>
          |        <label for="password">Password</label>
          |        <input type="password" id="password" placeholder="Password">
          |      </div>
          |    </div>
          |    
          |    <div id="graphiql">Loading...</div>
          |
          |    <script type="text/javascript">
          |      // Parse the search string to get url parameters.
          |      var search = window.location.search;
          |      var parameters = {};
          |      search.substr(1).split('&').forEach(function (entry) {
          |        var eq = entry.indexOf('=');
          |        if (eq >= 0) {
          |          parameters[decodeURIComponent(entry.slice(0, eq))] =
          |            decodeURIComponent(entry.slice(eq + 1));
          |        }
          |      });
          |      // if variables was provided, try to format it.
          |      if (parameters.variables) {
          |        try {
          |          parameters.variables =
          |            JSON.stringify(JSON.parse(parameters.variables), null, 2);
          |        } catch (e) {
          |          // Do nothing, we want to display the invalid JSON as a string, rather
          |          // than present an error.
          |        }
          |      }
          |      // When the query and variables string is edited, update the URL bar so
          |      // that it can be easily shared
          |      function onEditQuery(newQuery) {
          |        parameters.query = newQuery;
          |        updateURL();
          |      }
          |      function onEditVariables(newVariables) {
          |        parameters.variables = newVariables;
          |        updateURL();
          |      }
          |      function onEditOperationName(newOperationName) {
          |        parameters.operationName = newOperationName;
          |        updateURL();
          |      }
          |      function updateURL() {
          |        var newSearch = '?' + Object.keys(parameters).filter(function (key) {
          |          return Boolean(parameters[key]);
          |        }).map(function (key) {
          |          return encodeURIComponent(key) + '=' +
          |            encodeURIComponent(parameters[key]);
          |        }).join('&');
          |        history.replaceState(null, null, newSearch);
          |      }
          |      function graphQLFetcher(graphQLParams) {
          |        var username = document.getElementById("username").value;
          |        var password = document.getElementById("password").value;
          |        var encoded = window.btoa(username + ":" + password);
          |        var auth = "Basic " + encoded;
          |        
          |        return fetch('$backendPath', {
          |          method: 'post',
          |          headers: {
          |            'Accept': 'application/json',
          |            'Content-Type': 'application/json',
          |            'Authorization': auth,
          |          },
          |          body: JSON.stringify(graphQLParams),
          |          credentials: 'include',
          |        }).then(function (response) {
          |          return response.text();
          |        }).then(function (responseBody) {
          |          try {
          |            return JSON.parse(responseBody);
          |          } catch (error) {
          |            return responseBody;
          |          }
          |        });
          |      }
          |      ReactDOM.render(
          |        React.createElement(GraphiQL, {
          |          fetcher: graphQLFetcher,
          |          schema: undefined,
          |          query: parameters.query,
          |          variables: parameters.variables,
          |          response: parameters.response,
          |          operationName: parameters.operationName,
          |          onEditQuery: onEditQuery,
          |          onEditVariables: onEditVariables,
          |          onEditOperationName: onEditOperationName
          |        }),
          |        document.getElementById('graphiql')
          |      );
          |    </script>
          |</body>
          |</html>""".stripMargin
    }
  }
}
