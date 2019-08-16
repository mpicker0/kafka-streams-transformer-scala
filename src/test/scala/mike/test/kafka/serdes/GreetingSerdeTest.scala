package mike.test.kafka.serdes

import org.scalatest.{Matchers, FunSpecLike}

class GreetingSerdeTest extends FunSpecLike with Matchers {
  val serializer = new GreetingSerializer
  val deserializer = new GreetingDeserializer
  describe("serializer/deserializer") {
    it("should convert a Greeting into a byte array and back again") {
      val greeting = Greeting("Hello world")
      val bytes = serializer.serialize("not used", greeting)
      deserializer.deserialize("not used", bytes) shouldBe greeting
    }
    it("should convert a Greeting into a byte array") {
      val bytes = serializer.serialize("not used", Greeting("Hello world"))
      bytes shouldBe "Hello world".toCharArray
    }
    it("should convert a byte array into a Greeting") {
      val greeting = deserializer.deserialize("not used", "Hello world".getBytes)
      greeting shouldBe Greeting("Hello world")
    }
  }
}