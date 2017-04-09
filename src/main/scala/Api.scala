import KafkaClientActor.{Command, DescribeKafkaClusterConsumer}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.reflect.ClassTag

class Api(kafkaClientActorRef: ActorRef)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  import actorSystem.dispatcher

  implicit val duration: Timeout = 90.seconds

  val settings = ApiSettings(actorSystem.settings.config)

  private def askFor[RES](command: Command)(implicit tag: ClassTag[RES]) =
    (kafkaClientActorRef ? command).mapTo[RES]

  val route =
    redirectToNoTrailingSlashIfPresent(StatusCodes.Found) {
      path("health") {
        complete("OK")
      } ~ path("consumer" / Segment) { consumerGroup =>
        complete(askFor[String](DescribeKafkaClusterConsumer(consumerGroup)))
      }
    }

  def start() = Http().bindAndHandle(route, "0.0.0.0", settings.port)
}

object Api {
  def apply(kafkaClientActorRef: ActorRef)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) = new Api(kafkaClientActorRef)
}

case class ApiSettings(port: Int)

object ApiSettings {
  def apply(config: Config): ApiSettings = ApiSettings(config.getInt("api.port"))
}
