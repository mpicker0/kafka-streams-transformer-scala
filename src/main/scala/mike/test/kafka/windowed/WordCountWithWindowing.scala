package mike.test.kafka.windowed

import org.apache.kafka.streams.{StreamsConfig, KafkaStreams}
import org.apache.kafka.streams.kstream.TimeWindows
// Scala-specific imports
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.StreamsBuilder
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.Properties
import javax.management.ObjectName
import java.lang.management.ManagementFactory

object WordCountWithWindowing extends App {
  val WordCount = "word-count"
  val WindowedWordCount = "windowed-word-count"

  val bootstrap = if (args.length > 0) args(0) else "localhost:9092"

  // Stream setup
  val props = new Properties
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-transformer-scala-wordcount-windowed")
  val example = Files.createTempDirectory(new File("/tmp").toPath, "example").toFile
  props.put(StreamsConfig.STATE_DIR_CONFIG, example.getPath)

  val builder = new StreamsBuilder
  val textLines = builder.stream[String, String]("streams-plaintext-input")
  val groupedByWord = textLines
    .flatMapValues(textLine => textLine.toLowerCase.split("\\W+"))
    .groupBy((_, word) => word)

  // Create a State Store for with the all time word count
  val myMaterialized: org.apache.kafka.streams.scala.kstream.Materialized[String, Long, org.apache.kafka.streams.scala.ByteArrayKeyValueStore] =
  org.apache.kafka.streams.scala.kstream.Materialized.as[String, Long, org.apache.kafka.streams.scala.ByteArrayKeyValueStore](WordCount)
  groupedByWord.count()(myMaterialized)

  // Create a Windowed State Store that contains the word count for every
  // 1 minute
  groupedByWord.windowedBy(TimeWindows.of(Duration.ofMinutes(1))).count()

  val topology = builder.build
//  println("topology:")
//  println(topology.describe())
  val streams = new KafkaStreams(topology, props)

  val consumerControl = new Query(streams)
  val name = new ObjectName("mike.test.kafka.windowed:type=WordCountWithWindowing")
  val mbs = ManagementFactory.getPlatformMBeanServer
  mbs.registerMBean(consumerControl, name)

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}
