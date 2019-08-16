package mike.test.kafka.serdes

import org.apache.kafka.common.serialization.{Deserializer, StringSerializer, Serializer, StringDeserializer}
import org.json4s.native.Serialization.{read, write}

class GreetingJsonSerializer extends Serializer[Greeting] {
  implicit val formats = org.json4s.DefaultFormats
  override def serialize(topic: String, data: Greeting): Array[Byte] = {
    (new StringSerializer).serialize(topic, write(data))
  }
}

class GreetingJsonDeserializer extends Deserializer[Greeting] {
  implicit val formats = org.json4s.DefaultFormats
  override def deserialize(topic: String, data: Array[Byte]): Greeting = {
    read[Greeting]((new StringDeserializer).deserialize(topic, data))
  }
}

