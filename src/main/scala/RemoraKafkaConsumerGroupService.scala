import java.util.Properties

import kafka.admin.AdminClient
import kafka.admin.ConsumerGroupCommand.{LogEndOffsetResult, ConsumerGroupCommandOptions, KafkaConsumerGroupService}
import kafka.common.TopicAndPartition
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerConfig}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import scala.collection.JavaConverters._

/*based on kafka.admin.ConsumerGroupCommand which gives an example of the below
*
* ➜  bin ./kafka-run-class.sh kafka.admin.ConsumerGroupCommand --new-consumer --describe --bootstrap-server localhost:9092 --group console-consumer-96416
GROUP                          TOPIC                          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             OWNER
console-consumer-96416         test                           0          3               3               0               consumer-1_/192.168.0.102
console-consumer-96416         test                           1          4               4               0               consumer-1_/192.168.0.102
*
* */

case class GroupInfo(group: String, topic: String, partition: Int, offsetOpt: Option[Long], logEndOffset: Option[Long], lag: Option[Long], ownerOpt: Option[String])

class RemoraKafkaConsumerGroupService(kafkaSettings: KafkaSettings) {

  private def createAdminClient(): AdminClient = {
    val props = new Properties()
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaSettings.address)
    AdminClient.create(props)
  }

  private val adminClient = createAdminClient()

  private def createNewConsumer(group: String): KafkaConsumer[String, String] = {
    val properties = new Properties()
    val deserializer = (new StringDeserializer).getClass.getName
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaSettings.address)
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, group)
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserializer)
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer)

    new KafkaConsumer(properties)
  }

  //looking at future versions of describeConsumerGroup this will have to change dramatically
  def describeConsumerGroup(group: String): List[GroupInfo] = {
    val consumerSummaries = adminClient.describeConsumerGroup(group)
    implicit val consumer = createNewConsumer(group)
    consumerSummaries.flatMap { consumerSummary =>
      val topicPartitions = consumerSummary.assignment.map(tp => TopicAndPartition(tp.topic, tp.partition))
      val partitionOffsets = topicPartitions.flatMap { topicPartition =>
        Option(consumer.committed(new TopicPartition(topicPartition.topic, topicPartition.partition))).map { offsetAndMetadata =>
          topicPartition -> offsetAndMetadata.offset
        }
      }.toMap

      topicPartitions.sortBy { case topicPartition => topicPartition.partition }

      describeTopicPartition(group, topicPartitions, partitionOffsets.get, _ => Some(s"${consumerSummary.clientId}_${consumerSummary.clientHost}"))
    }
  }

  private def describeTopicPartition(group: String,
                                     topicPartitions: Seq[TopicAndPartition],
                                     getPartitionOffset: TopicAndPartition => Option[Long],
                                     getOwner: TopicAndPartition => Option[String])
                                    (implicit consumer: KafkaConsumer[String, String]): Seq[GroupInfo] = {
    topicPartitions
      .sortBy { case topicPartition => topicPartition.partition }
      .flatMap { topicPartition =>
        describePartition(group, topicPartition.topic, topicPartition.partition, getPartitionOffset(topicPartition),
          getOwner(topicPartition))
      }
  }

  protected def getLogEndOffset(topic: String, partition: Int)
                               (implicit consumer: KafkaConsumer[String, String]): LogEndOffsetResult = {
    val topicPartition = new TopicPartition(topic, partition)
    consumer.assign(List(topicPartition).asJava)
    consumer.seekToEnd(List(topicPartition).asJava)
    val logEndOffset = consumer.position(topicPartition)
    LogEndOffsetResult.LogEndOffset(logEndOffset)
  }

  private def describePartition(group: String,
                                topic: String,
                                partition: Int,
                                offsetOpt: Option[Long],
                                ownerOpt: Option[String])
                               (implicit consumer: KafkaConsumer[String, String]): Option[GroupInfo] = {
    def convert(logEndOffset: Option[Long]): GroupInfo = {
      val lag = offsetOpt.filter(_ != -1).flatMap(offset => logEndOffset.map(_ - offset))
      GroupInfo(group, topic, partition, offsetOpt, logEndOffset, lag, ownerOpt)
    }
    getLogEndOffset(topic, partition) match {
      case LogEndOffsetResult.LogEndOffset(logEndOffset) => Some(convert(Some(logEndOffset)))
      case LogEndOffsetResult.Unknown => None
      case LogEndOffsetResult.Ignore => None
    }
  }
}

object Test extends App {
  print(new RemoraKafkaConsumerGroupService(new KafkaSettings("localhost:9092")).describeConsumerGroup("console-consumer-41109"))
}
