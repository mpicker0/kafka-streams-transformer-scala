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

import scala.util.Try

class FlatMapTest extends FunSpecLike with Matchers with FlatMapTestConfig {
  describe("flatMapValues") {
    it("should write a single record for a negative number") {
      val inRecord = factory.create(SourceTopic, "not used", "-1")

      testDriver.pipeInput(inRecord)

      val outRecord = testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer)
      OutputVerifier.compareKeyValue(outRecord, "not used", "-1")
      testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer) shouldBe null
    }

    it("should write two records for a positive number") {
      val inRecord = factory.create(SourceTopic, "not used", "1")

      testDriver.pipeInput(inRecord)

      val outRecord1 = testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer)
      OutputVerifier.compareKeyValue(outRecord1, "not used", "1")
      val outRecord2 = testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer)
      OutputVerifier.compareKeyValue(outRecord2, "not used", "1")
      testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer) shouldBe null
    }

    it("should write nothing for zero") {
      val inRecord = factory.create(SourceTopic, "not used", "0")

      testDriver.pipeInput(inRecord)

      testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer) shouldBe null
    }

    it("should write nothing if the input can't be parsed") {
      val inRecord = factory.create(SourceTopic, "not used", "not an integer")

      testDriver.pipeInput(inRecord)

      testDriver.readOutput(OutputTopic, stringDeserializer, stringDeserializer) shouldBe null
    }
  }
}

trait FlatMapTestConfig {
  val SourceTopic = "source-topic"
  val OutputTopic = "flatmap-topic"
  val props = new Properties
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused in test")
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "filter-transform-test")
  val builder = new StreamsBuilder
  val input = builder.stream[String, String](SourceTopic)

  // the actual code we're testing
  // If we read an integer < 0 we output it once
  // if we read a zero, or not an integer, we output nothing
  // if we read an integer > 0 we output it twice
  input.flatMapValues { value =>
    Try(value.toInt).toOption match {
      case Some(i) if i > 0 => Seq(i.toString, i.toString)
      case Some(i) if i < 0 => Seq(i.toString)
      case _ => Nil
    }
  }.to(OutputTopic)

  val topology = builder.build
  val testDriver = new TopologyTestDriver(topology, props)
  val stringSerializer = new StringSerializer
  val stringDeserializer = new StringDeserializer
  val factory = new ConsumerRecordFactory[String, String](SourceTopic, stringSerializer, stringSerializer)
}