package mike.test.kafka

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
// Scala-specific imports
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import java.time.Duration
import java.util.Properties

object FilterTransform {

    def main(args: Array[String]) {
        val bootstrap = if (args.length > 0) args(0) else "localhost:9092"

        // Stream setup
        val props = new Properties
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-transformer-scala")

        val builder = new StreamsBuilder
        val input = builder.stream[String, String]("source-topic")

        input.map((_, value) => (value, value.toLowerCase()))
          .to("transformed-topic")

        input.filter((_, value) => value.length > 0 && (value.charAt(0) != '#')).to("filtered-topic")

        val streams = new KafkaStreams(builder.build, props)
        streams.start()

        sys.ShutdownHookThread {
            streams.close(Duration.ofSeconds(10))
        }
    }

}