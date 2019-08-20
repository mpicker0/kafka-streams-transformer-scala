package mike.test.kafka.serdes

import org.json4s.ParserUtil.ParseException
import org.scalatest.{FunSpecLike, Matchers}

class GreetingJsonSerdeTest extends FunSpecLike with Matchers {
  val serializer = new GreetingJsonSerializer
  val deserializer = new GreetingJsonDeserializer
  describe("serializer/deserializer") {
    it("should convert a Greeting into a byte array and back again") {
      val greeting = Greeting("Hello world")
      val bytes = serializer.serialize("not used", greeting)
      deserializer.deserialize("not used", bytes) shouldBe greeting
    }
    it("should convert a Greeting into a byte array") {
      val bytes = serializer.serialize("not used", Greeting("Hello world"))
      bytes shouldBe """{"message":"Hello world"}""".toCharArray
    }
    it("should convert a byte array into a Greeting") {
      val greeting = deserializer.deserialize("not used", """{"message":"Hello world"}""".getBytes)
      greeting shouldBe Greeting("Hello world")
    }
  }
  describe("exceptions") {
    it("should fail to convert a byte array into a Greeting") {
      a[ParseException] shouldBe thrownBy {
        deserializer.deserialize("not used", "this isn't valid JSON".getBytes)
      }
    }
  }
}