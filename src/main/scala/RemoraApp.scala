import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object RemoraApp extends App {

  private val actorSystemName: String = "remora"
  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  Api().start()

}