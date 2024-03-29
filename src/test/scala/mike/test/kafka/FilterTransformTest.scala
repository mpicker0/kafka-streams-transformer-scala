package mike.test.kafka

import java.util.Properties

import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer}
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.scalatest.{FunSpecLike, Matchers}
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.OutputVerifier
import org.apache.kafka.streams.test.ConsumerRecordFactory

class FilterTransformTest extends FunSpecLike with Matchers with FilterTransformTestConfig {
  describe("filtering") {
    it("should pass a non-comment line") {
      val inRecord = factory.create(SourceTopic, "not used", "some line without a comment")

      testDriver.pipeInput(inRecord)

      val outRecord = testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer)
      // this works
      outRecord.key shouldBe "not used"
      outRecord.value shouldBe "some line without a comment"
      // but this is probably better
      OutputVerifier.compareKeyValue(outRecord, "not used", "some line without a comment")
    }

    it("should filter a comment line") {
      val inRecord = factory.create(SourceTopic, "not used", "#some line with a comment")

      testDriver.pipeInput(inRecord)

      val outRecord = testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer)
      outRecord shouldBe null
    }
  }
}

trait FilterTransformTestConfig {
  // TODO this heavily duplicates FilterTransform.scala; see about reuse
  val SourceTopic = "source-topic"
  val OutputTopic = "filtered-topic"
  val props = new Properties
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused in test")
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "filter-transform-test")
  val builder = new StreamsBuilder
  val input = builder.stream[String, String](SourceTopic)

  // the actual code we're testing
  input.filter((_, value) => value.length > 0 && (value.charAt(0) != '#')).to(OutputTopic)

  val topology = builder.build
  val testDriver = new TopologyTestDriver(topology, props)
  val stringSerializer = new StringSerializer
  val stringDeserializer = new StringDeserializer
  val factory = new ConsumerRecordFactory[String, String](SourceTopic, stringSerializer, stringSerializer)
}