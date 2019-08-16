package mike.test.kafka.serdes

import org.apache.kafka.common.serialization.{Deserializer, StringSerializer, Serializer, StringDeserializer}

class GreetingSerializer extends Serializer[Greeting] {
  override def serialize(topic: String, data: Greeting): Array[Byte] = {
    (new StringSerializer).serialize(topic, data.message)
  }
}

class GreetingDeserializer extends Deserializer[Greeting] {
  override def deserialize(topic: String, data: Array[Byte]): Greeting = {
    Greeting((new StringDeserializer).deserialize(topic, data))
  }
}

