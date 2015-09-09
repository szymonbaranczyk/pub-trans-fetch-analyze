package api

import akka.actor.ActorSystem
import akka.pattern.ask
import core.DefaultTimeout
import org.slf4j.LoggerFactory
import service.RefreshLines
import service.service.{StopFetching, StartFetching}
import shapeless.~>
import spray.http._
import spray.routing.Directives
import domain.Location
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Not used currently
 *
 */
/** class LocationApi(implicit val actorSystem: ActorSystem) extends Directives with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  val locationActor = actorSystem.actorSelection("/user/application/location")

  val route =
    path("Location") {
      post {
        respondWithStatus(StatusCodes.Created) {
          handleWith { location : Location =>
            (locationActor ? CreateLocationList(location)).mapTo[Location]
          }
        }
      }
    }

} **/
/**
 * This class binds following paths:
 * - /startFetching, which launches scheduler fetching vehicles location every 10 seconds
 * - /stopFetching, which stops said scheduler
 * - /refreshLines, which fetches html page and parses it for lines number, then passes them for /startFetching use
 * - /test, used to test spray-client capabilities
 * @param actorSystem
 */
class LocationFetchControlApi(implicit val actorSystem: ActorSystem) extends Directives with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global

  val log = LoggerFactory.getLogger("Api")
  val locationFetchControlActor = actorSystem.actorSelection("/user/application/locationFetchControl")
  log.info("sending everything to " + locationFetchControlActor.toSerializationFormat)
  val route =
    path("startFetching") {
      post {
        respondWithStatus(StatusCodes.OK) {
          complete {
            log.info("send start fetching message")
            locationFetchControlActor ! StartFetching()
            "OK"
          }
        }
      }
    } ~
      path("stopFetching") {
        post {
          respondWithStatus(StatusCodes.OK) {
            complete {
              log.info("send stop fetching message")
              locationFetchControlActor ! StopFetching()
              "OK"
            }
          }
        }
      } ~
      path("refreshLines") {
        post {
          respondWithStatus(StatusCodes.OK) {
            complete {
              log.info("send refresh lines message")
              locationFetchControlActor ! RefreshLines()
              "OK"
            }
          }
        }
      } ~
      path("test") {
        post {
          respondWithStatus(StatusCodes.OK) {
            handleWith { formData: FormData =>
              formData.fields.foreach(s => log.info(s._1+"="+s._2))
              "OK"
            }
          }
        }
      }


}