Filter/Transform
================

This is an example of transforming and filtering records originating in one
Kafka topic to two different output topics using Kafka Streams.  I have a Java
version of this project in a separate git repo. 

Layout
------

The Kakfa streams topology looks like this:

    +------------------+
    |  source-topic    |
    +----+----+--------+
         |    |
         |    | filter (remove blank lines and comments)
         |    |
         |    v
         |  +------------------+
         |  |  filtered-topic  |
         |  +------------------+
         |
         | map (lower-case)
         |
         v
     +---------------------+
     |  transformed-topic  |
     +---------------------+


Configuring the environment
---------------------------

The environment settings are currently hardcoded in each of the consumer
classes.  The bootstrap server list defaults to `localhost:9092`, but may
be overridden if desired.  This bootstrap server is also referenced in several
commands below.

If running the Kafka client commands via Docker, a Zookeeper port of `2181` is
assumed in the commands below.

### Docker

The `docker-compose.yml` in the `docker` directory will create a single-broker
Docker configuration with the appropriate topics.  It is based on
[this Docker image](https://github.com/wurstmeister/kafka-docker).

Start it with:

    cd docker
    docker-compose up

### Manual

The following steps start Zookeeper, a broker, and create the topics required
for these examples, each with three partitions and a replication factor of 1.

* Setup

        export KAFKA_HOME=~/local/kafka_2.11-1.1.0  # or wherever Kafka was unpacked

* Start Zookeeper

        $KAFKA_HOME/bin/zookeeper.sh $KAFKA_HOME/config/zookeeper.properties

* Start the broker

        $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties

* Create the topics

        $KAFKA_HOME/bin/kafka-topics.sh --create --topic source-topic --partitions 3 --replication-factor 1 --zookeeper localhost:2181
        $KAFKA_HOME/bin/kafka-topics.sh --create --topic transformed-topic --partitions 3 --replication-factor 1 --zookeeper localhost:2181
        $KAFKA_HOME/bin/kafka-topics.sh --create --topic filtered-topic --partitions 3 --replication-factor 1 --zookeeper localhost:2181

### Confirming the status

Once the environment is up, either created manually or via Docker, check its
status with

    $KAFKA_HOME/bin/kafka-topics.sh --describe --topic source-topic,transformed-topic,filtered-topic --zookeeper localhost:2181
    # or simply
    $KAFKA_HOME/bin/kafka-topics.sh --describe --zookeeper localhost:2181

If you don't have any of the Kafka binaries installed locally, it is also
possible to do this via Docker if you've checked out the
`wurstmeister/kafka-docker` repo linked above:

    cd $KAFKA_DOCKER_HOME
    ./start-kafka-shell.sh $(hostname) $(hostname):2181

    # once inside the container:
    kafka-topics.sh --describe --zookeeper $ZK

_Note_: the first argument to `start-kafka-shell.sh` is the hostname where
the Kafka broker is running, and is used to feed the `broker-list.sh` script
inside the container.  The second is the Zookeeper location, and sets the `$ZK`
environment variable inside the container.

Running
-------

Once the Kafka environment is running, start the transformer with:

    sbt run

and choose the option for `mike.test.kafka.FilterTransform`

The transformer will not have anything to do initially, since the source topic
will be empty.  Attach to the output topics to monitor the output:

    cd $KAFKA_DOCKER_HOME
    bin/kafka-console-consumer.sh \
      --topic transformed-topic \
      --from-beginning \
      --bootstrap-server localhost:9092

    bin/kafka-console-consumer.sh \
      --topic filtered-topic \
      --from-beginning \
      --bootstrap-server localhost:9092

If you want, you can also attach to the source topic to see the unmodified
data:

    bin/kafka-console-consumer.sh \
      --topic source-topic \
      --from-beginning \
      --bootstrap-server localhost:9092

Kafkacat equivalents for the above:

    kafkacat -b localhost -t transformed-topic
    kafkacat -b localhost -t filtered-topic
    kafkacat -b localhost -t source-topic

With the consumers running, put data into the source topic.  To have some
variation in the data, we will use lines from the `kafka-console-consumer.sh`
script itself as the data.

    bin/kafka-producer-perf-test.sh  \
      --topic=source-topic \
      --num-records 500 \
      --throughput 2 \
      --payload-file bin/kafka-producer-perf-test.sh \
      --producer-props \
        bootstrap.servers=localhost:9092

Kafakcat equivalent:

    kafkacat -b localhost -t source-topic < $KAFKA_HOME/bin/kafka-producer-perf-test.sh

At this point, you should see data appear on all three consumers.  The data
from `transformed-topic` will be the lower-cased version of the input.  The
data from `filtered-topic` contains all the non-blank, non-comment lines from
the input.

Word count
==========

This example is taken from the list of examples at
<https://github.com/confluentinc/kafka-streams-examples>.  It reads a list of
words from an input topic and keeps a count of each word, which it produces as
a `KTable`.

Layout
------

           +---------------------------+
           |  streams-plaintext-input  |
           +----------+----------------+
                      |
                      |
                      |
                      v
                +-------------------------------------+
                |  intermediate KTable[String, Long]  |
                +-----+-------------------------------+
                      |
                      |
                      |
                      v
           +----------------------------+
           |  streams-wordcount-output  |
           +----------------------------+


Configuration
-------------

Either use the same Docker configuration as above, or start up a local Kafka
instance and create the topics `streams-plaintext-input` and
`streams-wordcount-output`.

Running
-------

Once the Kafka environment is running, start the transformer with:

    sbt run

and choose the option for `mike.test.kafka.WordCountMain`

Attach to the output topic to review the output; note that Kafkacat won't
know how to deserialize the `Long` output so use the built-in Kafka console
consumer:

    $KAFKA_HOME/bin/kafka-console-consumer.sh \
      --bootstrap-server localhost:9092 \
      --topic streams-wordcount-output \
      --from-beginning \
      --property print.key=true \
      --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer

Then, attach a producer to the input topic:

    kafkacat -b localhost -t streams-plaintext-input -P

Type some words into the producer, such as

    here is a line
    another one I entered
    third line
    last one

You should see summary output with the word counts on the output topic,
though it may be a few seconds before it actually prints.
