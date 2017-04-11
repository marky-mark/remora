import KafkaClientActor.{Command, DescribeKafkaClusterConsumer}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.reflect.ClassTag

class Api(kafkaClientActorRef: ActorRef)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  import actorSystem.dispatcher
  import JsonOps._

  implicit val duration: Timeout = 90.seconds

  val settings = ApiSettings(actorSystem.settings.config)

  private def askFor[RES](command: Command)(implicit tag: ClassTag[RES]) =
    (kafkaClientActorRef ? command).mapTo[RES]

  val route =
    redirectToNoTrailingSlashIfPresent(StatusCodes.Found) {
      path("health") {
        complete("OK")
      } ~ path("consumer" / Segment) { consumerGroup =>
        complete(askFor[List[GroupInfo]](DescribeKafkaClusterConsumer(consumerGroup)).map(Json.toJson(_).toString))
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

object JsonOps {

  implicit val currentStateWrites: Writes[GroupInfo] = (
    (__ \ "group").write[String] and
      (__ \ "topic").write[String] and
      (__ \ "partition").write[Int] and
      (__ \ "offset").writeNullable[Long] and
      (__ \ "log_end_offset").writeNullable[Long] and
      (__ \ "lag").writeNullable[Long] and
      (__ \ "owner").writeNullable[String]
    ) (unlift(GroupInfo.unapply))
}
