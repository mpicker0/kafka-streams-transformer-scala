package mike.test.kafka

import org.apache.kafka.streams.{StreamsConfig, KafkaStreams}
// Scala-specific imports
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import java.time.Duration
import java.util.Properties

/** Read from multiple source topics; produce to a single output topic */
object MultiSource extends App with Stream1Config with Stream2Config {
    val bootstrap = if (args.length > 0) args(0) else "localhost:9092"
    val stream1 = getStream1(bootstrap)
    val stream2 = getStream2(bootstrap)

    stream1.start()
    stream2.start()

    sys.ShutdownHookThread {
        stream1.close(Duration.ofSeconds(10))
        stream2.close(Duration.ofSeconds(10))
    }
}

trait Stream1Config {
    def getStream1(bootstrap: String): KafkaStreams = {
        val props = new Properties
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-transformer-scala-stream1")

        val builder = new StreamsBuilder
        val input = builder.stream[String, String]("source-topic")

        input.map((_, value) => (value, value.toLowerCase()))
          .to("transformed-topic")

        new KafkaStreams(builder.build, props)
    }
}

trait Stream2Config {
    def getStream2(bootstrap: String): KafkaStreams = {
        val props = new Properties
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-transformer-scala-stream2")

        val builder = new StreamsBuilder
        val input = builder.stream[String, String]("other-source-topic")

        input.map((_, value) => (value, value.toUpperCase()))
          .to("transformed-topic")

        new KafkaStreams(builder.build, props)
    }
}