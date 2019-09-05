easy-deposit-properties
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-deposit-properties.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-deposit-properties)

<!-- Remove this comment and extend the descriptions below -->


SYNOPSIS
--------

    easy-deposit-properties load-props [--doUpdate] <properties-file>
    easy-deposit-properties run-service


DESCRIPTION
-----------

Service for keeping track of the deposit properties


ARGUMENTS
---------

    Options:

       -h, --help      Show help message
       -v, --version   Show version of this program

    Subcommand: load-props - Load a deposit.properties file and import it in the backend repository.
          --doUpdate   Without this argument the properties are not imported, the
                       default is a test mode that logs the intended changes
      -h, --help       Show help message
    
     trailing arguments:
      <properties-file> (required)   The deposit.properties file to be read.
    ---
    
    Subcommand: run-service - Starts EASY Deposit Properties as a daemon that services HTTP requests
       -h, --help   Show help message
    ---

EXAMPLES
--------

    easy-deposit-properties load-props <properties-file>
    easy-deposit-properties run-service


GRAPHQL INTERFACE
-----------------

1. build `easy-deposit-properties` using `mvn clean install`
2. make sure the [dans-dev-tools](https://github.com/DANS-KNAW/dans-dev-tools) are installed properly
3. call `run-reset-env.sh` from the root of the project
4. call `run-service.sh` from the root of the project
5. in your browser, go to http://localhost:20200/graphiql


GRAPHIQL TOOLS
--------------
To interact with the GraphQL servlet, use the internal http://localhost:20200/graphiql interface.
Alternatively, on Mac, use the [GraphiQL.app](https://github.com/skevy/graphiql-app).


GRAPHQL SCHEMA
--------------

To generate the latest version of the GraphQL schema for `easy-deposit-properties`:

    #install get-graphql-schema
    npm install -g get-graphql-schema
    
    # (re)start the service (after: mvn clean install -DskipTests=true): see GRAPHQL INTERFACE above
    
    get-graphql-schema http://<base-url>/graphql > docs/schema.graphql


INSTALLATION AND CONFIGURATION
------------------------------


1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-deposit-properties-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-deposit-properties-<version>/bin/easy-deposit-properties /usr/bin



General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-deposit-properties.git
        cd easy-deposit-properties
        mvn install
