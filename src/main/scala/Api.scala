import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._

class Api()(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  import actorSystem.dispatcher

  implicit val duration: Timeout = 90.seconds

  val settings = ApiSettings(actorSystem.settings.config)

  val route =
    redirectToNoTrailingSlashIfPresent(StatusCodes.Found) {
      path("health") {
        complete("OK")
      } ~ pathPrefix("kafka" / Segment) { clusterName: String =>
        pathEnd {
          complete(clusterName)
        } ~ pathPrefix("consumer") {
          pathEnd {
            complete("consumer cluster")
          } ~ path(Segment) { consumerGroup =>
            complete(consumerGroup)
          }
        }
      }
    }

  def start() = Http().bindAndHandle(route, "0.0.0.0", settings.port)
}

object Api {
  def apply()(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) = new Api()
}

case class ApiSettings(port: Int)

object ApiSettings {
  def apply(config: Config): ApiSettings = ApiSettings(config.getInt("api.port"))
}
