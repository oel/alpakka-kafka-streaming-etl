# Streaming ETL & Pipelining with Alpakka Kafka

This is an [Akka Stream](https://doc.akka.io/docs/akka/2.6/stream/index.html) based application in Scala that demonstrates how to create a scalable real-time streaming system using [Alpakka](https://doc.akka.io/docs/alpakka/3.0.3/index.html) APIs for ETL/pipelining on a distributed platform.

Central to the system are configurable Apache Kafka brokers which provide the publish-subscribe machinery for durable stream data to be produced or consumed by various data processing/storage systems.  Built on top of Akka Stream, the Alpakka APIs allow the data storage system components and data processing pipelines to be constructed as composable streaming stages and stream processing operators.  In addition, it enables the streaming system to be [Reactive Streams](https://www.reactive-streams.org/) compliant with non-blocking backpressure.

For an overview of the application, please visit Genuine Blog (URL to be provided soon).

## Kafka producers and consumers for real estate property listings

With [Alpakka Kafka](https://doc.akka.io/docs/alpakka-kafka/2.1.1/index.html) serving as the Akka Stream wrapper of Apache Kafka, the application has a couple of producers for streaming property listing data out of a PostgreSQL database and CSV data files using [Alpakka Slick](https://doc.akka.io/docs/alpakka/3.0.3/slick.html) and [Alpakka Csv](https://doc.akka.io/docs/alpakka/3.0.3/data-transformations/csv.html), respectively.  For stream consumption, the application consists of a few variants of consumers that ETL along with a processing pipeline into a Cassandra database using [Alpakka Cassandra](https://doc.akka.io/docs/alpakka/3.0.3/cassandra.html) and another consumer with custom stream flow/destination.

## Systems requirement: Kafka brokers, PostgreSQL and Cassandra databases

Existence of one or more working Kafka broker(s) is the minimal systems requirement.  For ETL/pipelining from a PostgreSQL database to a Cassandra data warehouse, both the Postgres and Cassandra servers would have to be set up first.

Note that scaling up the streaming system to run on Kafka brokers spanning multiple nodes would just need configurative changes.  Likewise, no code change should be needed to run the application on a multi-node Cassandra database.

To run the application that comes with sample real estate property listing data on a computer, Git-clone this repo to a local disk.

## Schema creation for property listings in PostgreSQL & Cassandra

Command-line scripts for creating schemas for property listings in PostgreSQL (*"schema_postgres.script.txt"*) and Cassandra (*"schema_cassandra.script.txt"*) are provided under "*{project-root}/src/main/resources/*".

A couple of TSV (tab separated values) files (*property_listing_db_postgres_500.tsv* & *property_listing_file_csv_500.tsv*) with identical data format, each consisting of sample real estate property listings, have been created and saved under "*{project-root}/src/main/resources/*".  One of them can be used for populating the Postgres database before running the Postgres producer and the other one as direct input for the CSV producer.

## Running Alpakka Kafka producers & consumers on one or more JVMs

Open up one or more shell command-line terminal(s), launch a mix of the producers and consumers from the *project-root* on the terminal(s).

To run *Postgres producer using Alpakka Kafka Consumer.PlainSink*:
```bash
$ sbt "runMain alpakkafka.producer.PostgresPlain [offset [limit]]"
```

To run *CSV-file producer using Alpakka Kafka Consumer.PlainSink*:
```bash
$ sbt "runMain alpakkafka.producer.CsvPlain [offset [limit]]"
```

To run *Cassandra consumer using Alpakka Kafka Consumer.PlainSource*:
```bash
$ sbt "runMain alpakkafka.consumer.CassandraPlain"
```

To run *Cassandra consumer using Alpakka Kafka Consumer.CommittableSource*:
```bash
$ sbt "runMain alpakkafka.consumer.CassandraCommittable"
```

To run *Cassandra consumer using Alpakka Kafka Consumer.CommittableSource with a rating pipeline*:
```bash
$ sbt "runMain alpakkafka.consumer.CassandraCommittableWithRatings"
```

To run *Custom-flow consumer using Alpakka Kafka Consumer.CommittableSource with a rating pipeline*:
```bash
$ sbt "runMain alpakkafka.consumer.CustomFlowCommittableWithRatings"
```

## Querying Cassandra property listing tables

Run queries to verify data that get ETL-ed to the Cassandra tables from the *project-root* on the terminal(s)

To query *Cassandra propertydb.property_listing*:
```bash
$ sbt "runMain alpakkafka.query.CassandraPropertyListing [partitionKey [limit]]"
```

To query *Cassandra propertydb.rated_property_listing*:
```bash
$ sbt "runMain alpakkafka.query.CassandraRatedPropertyListing [partitionKey [limit]]"
```
