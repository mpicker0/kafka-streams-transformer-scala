package mike.test.kafka

import java.util.Properties

import mike.test.kafka.serdes.{GreetingJsonSerializer, Greeting, GreetingJsonDeserializer}
import org.apache.kafka.common.serialization.{StringSerializer, Serde, StringDeserializer}
import org.apache.kafka.common.serialization.Serdes.serdeFrom
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.scalatest.{FunSpecLike, Matchers}
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.errors.{StreamsException, LogAndContinueExceptionHandler}
import org.apache.kafka.streams.test.OutputVerifier
import org.apache.kafka.streams.test.ConsumerRecordFactory

class CustomSerdesTest extends FunSpecLike with Matchers with CustomSerdesTestConfig {
  describe("normal serde testing") {
    it("map should add a ! to the end of the greeting") {
      val inRecord = standardSerdeFactory.create(SourceTopic, "not used", Greeting("Hello world"))

      testDriver.pipeInput(inRecord)

      val outRecord = testDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer)
      OutputVerifier.compareKeyValue(outRecord, "not used", Greeting("Hello world!"))
      testDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer) shouldBe null
    }
  }
  // In these tests, we create data on the input topic as a string; this allows for easier testing of errors
  describe("strings on the input topic testing") {
    // this is the happy path, and is just like the test above except we specify the Greeting directly as
    // a JSON string
    it("should add a ! to the end of the greeting") {
      val inRecord = stringInputFactory.create(SourceTopic, "not used", """{"message":"Hello world"}""")

      testDriver.pipeInput(inRecord)

      val outRecord = testDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer)
      OutputVerifier.compareKeyValue(outRecord, "not used", Greeting("Hello world!"))
      testDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer) shouldBe null
    }
    // error testing
    it("should throw if no exception handler is configured") {
      val inRecord = stringInputFactory.create(SourceTopic, "not used", "{this is not valid json}")

      a[StreamsException] shouldBe thrownBy {
        testDriver.pipeInput(inRecord)
      }
    }
    it("should continue when the exception handler is configured") {
      val inRecord = stringInputFactory.create(SourceTopic, "not used", "{this is not valid json}")

      exceptionHandlingTestDriver.pipeInput(inRecord)

      exceptionHandlingTestDriver.readOutput(OutputTopic, stringDeserializer, greetingDeserializer) shouldBe null
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

  // for testing the normal path, mapping Greeting to Greeting
  val topology = builder.build
  val testDriver = new TopologyTestDriver(topology, props)
  val standardSerdeFactory = new ConsumerRecordFactory[String, Greeting](SourceTopic, stringSerializer, greetingSerializer)

  // for testing errors, mapping String to Greeting
  val stringInputFactory = new ConsumerRecordFactory[String, String](SourceTopic, stringSerializer, stringSerializer)

  // for testing that errors are properly intercepted
  val exceptionHandlingProps = new Properties()
  exceptionHandlingProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused in test")
  exceptionHandlingProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "filter-transform-test")
  exceptionHandlingProps.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler])
  val exceptionHandlingTestDriver = new TopologyTestDriver(topology, exceptionHandlingProps)
}