import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object RemoraApp extends App {

  private val actorSystemName: String = "remora"
  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  val kafkaSettings = KafkaSettings(actorSystem.settings.config)
  val consumer = new RemoraKafkaConsumerGroupService(kafkaSettings)
  val kafkaClientActor = actorSystem.actorOf(KafkaClientActor.props(consumer), name = "kafka-client-actor")

  Api(kafkaClientActor).start()

}