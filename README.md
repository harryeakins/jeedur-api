## Summary

Jeedur API

Scala SBT project with Scalatra, lift-json and ScalaTest.

## Dependencies

Neo4j 1.8.2 (Community Edition) - [Download Page](http://www.neo4j.org/download)

## Setup

Launch [SBT](https://github.com/harrah/xsbt/wiki) (embedded in project):

    ./sbt

(optionally) generate IDEA configuration:

    gen-idea

Run tests

    ~test

Start Jetty server so that it boots after each source file change:

    ~;container:start; container:reload /

## Thanks

Miki Leskinen - github.com/mileskin/scalatra-rest.g8