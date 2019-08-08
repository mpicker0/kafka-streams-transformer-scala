package mike.test.kafka.windowed

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.QueryableStoreTypes
import scala.collection.JavaConverters._
import mike.test.kafka.windowed.WordCountWithWindowing.{WordCount, WindowedWordCount}

case class KV(key: String, value: Long)

trait QueryMBean {
  def allValues(): Unit
  def windowedValues(key: String): Unit
}

class Query(streams: KafkaStreams) extends QueryMBean {

  override def allValues(): Unit = {
    System.out.println("All values for store " + WordCount + ":")
    val keyValues = rangeForKeyValueStore(WordCount)
    keyValues.foreach(println(_))
  }

  override def windowedValues(key: String): Unit = {
    System.out.println("All windowed values for key " + key + " store " + WindowedWordCount + ":")
//    val keyValues = rangeForWindowedKeyValueStore(WindowedWordCount, key)
//    keyValues.foreach(println(_))
  }

  private def rangeForKeyValueStore(storeName: String) = {
    val store = streams.store(storeName, QueryableStoreTypes.keyValueStore)
    store.all.asScala map (kv => KV(kv.key, kv.value))
  }

//  private def rangeForWindowedKeyValueStore(storeName: String, key: String) = {
//    val store = streams.store(storeName, QueryableStoreTypes.windowStore)
//
//    // Query from the beginning of time until now
//    val timeFrom = Instant.ofEpochMilli(0)
//    val timeTo = Instant.now
//    store.fetch(key, timeFrom, timeTo).asScala map { kv =>
//      KV(key + "@" + kv.key, kv.value)
//    }
//  }
}
