import KafkaClientActor.DescribeKafkaClusterConsumer
import akka.actor.{Actor, ActorLogging, Props}
import kafka.admin.ConsumerGroupCommand.ConsumerGroupCommandOptions

object KafkaClientActor {

  sealed trait Command

  case class DescribeKafkaClusterConsumer(consumerGroupName: String) extends Command

  def props(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) = Props(classOf[KafkaClientActor], kafkaConsumerGroupService)

}

class KafkaClientActor(kafkaSettings: Array[String]) extends Actor with ActorLogging {

  override def receive: Receive = {

    case DescribeKafkaClusterConsumer(consumerGroupName) =>
      val settings = kafkaSettings.clone()
      val opts = new ConsumerGroupCommandOptions(kafkaSettings)
      sender() ! consumerGroupName
  }
}
