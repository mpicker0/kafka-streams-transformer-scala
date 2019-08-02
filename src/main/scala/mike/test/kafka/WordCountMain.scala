package mike.test.kafka

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{StreamsConfig, KafkaStreams}
// Scala-specific imports
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import java.time.Duration
import java.util.Properties

object WordCountMain {

    def main(args: Array[String]) {
        val bootstrap = if (args.length > 0) args(0) else "localhost:9092"

        // Stream setup
        val props = new Properties
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-transformer-scala-wordcount")

        val builder = new StreamsBuilder
        val textLines = builder.stream[String, String]("streams-plaintext-input")
        val wordCounts = textLines
          .flatMapValues(textLine => textLine.toLowerCase.split("\\W+"))
          .groupBy((_, word) => word)
          .count()
        wordCounts.toStream.to("streams-wordcount-output")

        val streams = new KafkaStreams(builder.build, props)
        streams.start()

        sys.ShutdownHookThread {
            streams.close(Duration.ofSeconds(10))
        }
    }

}