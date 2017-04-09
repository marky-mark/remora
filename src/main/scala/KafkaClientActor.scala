import KafkaClientActor.{DescribeKafkaCluster, DescribeKafkaClusterConsumer}
import akka.actor.{Actor, ActorLogging, Props}

object KafkaClientActor {

  sealed trait Command

  case class DescribeKafkaCluster(clusterName: String) extends Command
  case class DescribeKafkaClusterConsumer(clusterName: String, consumerGroupName: String) extends Command

  def props(kafkaSettings: KafkaSettings) = Props(classOf[KafkaClientActor], kafkaSettings)

}

class KafkaClientActor(kafkaSettings: KafkaSettings) extends Actor with ActorLogging {

  override def receive: Receive = {
    case DescribeKafkaCluster(clusterName) => sender() ! clusterName
    case DescribeKafkaClusterConsumer(clusterName, consumerGroupName) => sender() ! consumerGroupName
  }
}
