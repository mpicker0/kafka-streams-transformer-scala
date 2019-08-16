package mike.test.kafka

import java.util.Properties

import mike.test.kafka.serdes.{GreetingJsonSerializer, Greeting, GreetingJsonDeserializer}
import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer, Serde}
import org.apache.kafka.common.serialization.Serdes.serdeFrom
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.scalatest.{FunSpecLike, Matchers}
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.OutputVerifier
import org.apache.kafka.streams.test.ConsumerRecordFactory

class CustomSerdesTest extends FunSpecLike with Matchers with CustomSerdesTestConfig {
  describe("map") {
    it("should add a ! to the end of the greeting") {
      val inRecord = factory.create(SourceTopic, "not used", Greeting("Hello world"))

      testDriver.pipeInput(inRecord)

      val outRecord = testDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer)
      OutputVerifier.compareKeyValue(outRecord, "not used", Greeting("Hello world!"))
      testDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer) shouldBe null
    }
  }
}

trait CustomSerdesTestConfig {
  val SourceTopic = "source-topic"
  val OutputTopic = "custom-serde-topic"
  val props = new Properties
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused in test")
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "filter-transform-test")
  val stringSerializer = new StringSerializer
  val stringDeserializer = new StringDeserializer
  val greetingSerializer = new GreetingJsonSerializer
  val greetingDeserializer = new GreetingJsonDeserializer
  // This is the important part; it makes the Serde available at the appropriate times (.stream, .to)
  implicit val greetingSerde: Serde[Greeting] = serdeFrom(greetingSerializer, greetingDeserializer)
  val builder = new StreamsBuilder
  val input = builder.stream[String, Greeting](SourceTopic)

  // the actual code we're testing
  input.mapValues { greeting =>
    Greeting(s"${greeting.message}!")
  }.to(OutputTopic)

  val topology = builder.build
  val testDriver = new TopologyTestDriver(topology, props)
  val factory = new ConsumerRecordFactory[String, Greeting](SourceTopic, stringSerializer, greetingSerializer)
}