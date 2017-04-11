import KafkaClientActor.DescribeKafkaClusterConsumer
import akka.actor.{Actor, ActorLogging, Props}
import kafka.admin.ConsumerGroupCommand.ConsumerGroupCommandOptions

object KafkaClientActor {

  sealed trait Command

  case class DescribeKafkaClusterConsumer(consumerGroupName: String) extends Command

  def props(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) = Props(classOf[KafkaClientActor], kafkaConsumerGroupService)

}

class KafkaClientActor(kafkaSettings: KafkaSettings) extends Actor with ActorLogging {

  private val kafkaConsumerGroupService : RemoraKafkaConsumerGroupService = new RemoraKafkaConsumerGroupService(kafkaSettings)

  override def receive: Receive = {

    case DescribeKafkaClusterConsumer(consumerGroupName) =>
      sender() ! kafkaConsumerGroupService.describeConsumerGroup(consumerGroupName)
  }
}
