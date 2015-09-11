package service

/**
 * Created by SBARANCZ on 01.09.2015.
 */

package service

import akka.actor._
import akka.event.LoggingReceive
import api.Marshalling
import org.slf4j.Logger
import spray.http.StatusCodes
import spray.json.ProductFormats
import core.DatabaseCfg._
import slick.driver.H2Driver.api._
import spray.util.LoggingContext
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Case classes for Akka messaging
 */

case class StartFetching()
case class StopFetching()
/**
 * Akka actor starting and stopping fetching. Sadly I couldn't extract business logic from actor to trait,
 * because we need access to ActorContext
 */
class LocationFetchControlActor(implicit val log: Logger) extends Actor {

  var fetchActor:Option[ActorRef]=None
  var locationActor: Option[ActorRef] = None
  var cancellable:Option[Cancellable]=None
  override def preStart()={
    context.actorSelection("../fetch") ! Identify()
  }
  override def receive:Receive = {

    case ActorIdentity(path, Some(ref)) =>
      ref.path.name match {
        case "fetch" =>
          fetchActor = Some(ref)
          context.actorSelection("../location") ! Identify()
          log.info("successfully binded FetchActor reference")
        case "location" =>
          fetchActor match {
            case Some(x) =>
              locationActor = Some(ref)
              x ! AddRoutee(locationActor.get)
              log.info("successfully binded LocationActor reference")
            case None => log.info("fetchActor is None")
          }
        case lol => log.info("path= " + lol.toString)
      }
    case ActorIdentity(path, None) => log.info("path is unavailable at " + path)
    case StartFetching() => fetchActor match {
      case Some(x) => context.system.scheduler.schedule(0 seconds, 10 seconds, x , Fetch())
        log.info("Starting fetching")
        sender() ! true
      case None => log.error("couldn't identify FetchActor")
        sender() ! false
    }
    case StopFetching() => cancellable match {
      case Some(x) => x.cancel()
        log.info("Stopping fetching")
        sender() ! true
      case None => log.debug("tried to stop not working scheduler")
        sender() ! false
    }
    case RefreshLines() => fetchActor match {
      case Some(x) =>
        x ! RefreshLines()
      case None => log.error("couldn't identify FetchActor")


    }
  }
}

