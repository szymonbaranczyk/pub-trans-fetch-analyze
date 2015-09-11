package core

import akka.actor.{ Props, Actor }
import org.slf4j.LoggerFactory
import service.service.LocationFetchControlActor
import service.{LocationActor, WroclawPositionFetcher, FetchActor}

/**
 * This actor:
 * - when receive Startup message it creates actors that will handle our requests
 * - when receive Shutdown message it stops all actors from context
 */
case class Startup()
case class Shutdown()

class ApplicationActor extends Actor {
  implicit val system= context.system
  implicit val log=LoggerFactory.getLogger("application")
  log.info(self.path.toStringWithAddress(self.path.address))
  def receive: Receive = {
    case Startup() => {
      context.actorOf(Props(new LocationActor()), "location")
      context.actorOf(Props(new FetchActor(new WroclawPositionFetcher())), "fetch")
      context.actorOf(Props(new LocationFetchControlActor()), "locationFetchControl")
      sender ! true
    }
    case Shutdown() => {
      context.children.foreach(ar => context.stop(ar))
    }
  }
}
