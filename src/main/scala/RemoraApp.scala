import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object RemoraApp extends App {

  private val actorSystemName: String = "remora"
  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  val kafkaSettings = KafkaSettings(actorSystem.settings.config)
  val kafkaClientActor = actorSystem.actorOf(KafkaClientActor.props(kafkaSettings), name = "kafka-client-actor")

  Api(kafkaClientActor).start()

}