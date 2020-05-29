GraphQL Examples
================

This directory contains various examples of GraphQL queries. They (together with their `*.json` counterparts) are being used as the input and expected output for [automated tests](../../scala/nl.knaw.dans.easy.properties/server).

Due to a [bug](https://github.com/sangria-graphql/sangria/issues/470) in [Sangria](https://github.com/sangria-graphql/sangria), not all tests currently succeed. Those tests should be skipped for now. These are indicated by an 'expected output file' ending in `.ignore.json`.

Currently this test set does not contain any tests with time filters (`createdEarlierThan`, etc.). This is due to differing syntax between PostgreSQL (used in production) and HSQLDB (used in local testing). To test the time filters, build and deploy the project on your development VM.
