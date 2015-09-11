package service

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.LoggingReceive
import akka.routing.{Routee, BroadcastRoutingLogic, Router}
import api.Marshalling
import domain.{LocationsFetch, Location}
import org.slf4j.{Logger, LoggerFactory}
import shapeless.~>
import spray.http._
import spray.client.pipelining._
import spray.json._
import spray.util.LoggingContext
import scala.collection.immutable.StringOps
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


case class AddRoutee(actorRef: ActorRef)

case class RemoveRoutee(actorRef: ActorRef)

case class RefreshLines()

case class Fetch()

/**
 * An actor wrapper for fetcher - object responsible for retrieving public transport locations at the moment from specific city
 * and broadcasting it to all received routees
 * @param positionFetcher fetcher specific from specific city
 */
class FetchActor(positionFetcher: PositionFetcher)(implicit val log: Logger) extends Actor with FetchActions {
  log.info(self.path.toStringWithAddress(self.path.address))
  implicit val system = context.system

  override def receive: Receive = {

    case AddRoutee(actorRef) => Add(actorRef)
      log.info(s"added Routee ${actorRef.path.name}")
    case RemoveRoutee(actorRef) => Remove(actorRef)
      log.info(s"removed Routee ${actorRef.path.name}")
    case RefreshLines() => positionFetcher.refreshLines()
      log.info("Refreshing available lines")
    case Fetch() =>
      val fetch = positionFetcher.fetch()
      router.route(fetch, self)
      log.info("fetching locations")

  }
}

trait FetchActions {
  //see http://stackoverflow.com/questions/27754247/how-to-use-noroutee-as-catch-all-case-for-custom-routing-logic
  var router = Router(BroadcastRoutingLogic(), scala.collection.immutable.IndexedSeq[Routee]())

  def Add(actorRef: ActorRef): Unit = router = router.addRoutee(actorRef)

  def Remove(actorRef: ActorRef): Unit = router = router.removeRoutee(actorRef)
}

trait PositionFetcher {
  def fetch(): LocationsFetch

  def refreshLines(): Unit
}

class WroclawPositionFetcher(implicit val logger: Logger, implicit val actorSystem: ActorSystem) extends LocationsFetchProtocol with PositionFetcher {
  var bus = Seq[String]()
  var train = Seq[String]()
  var tram = Seq[String]()

  /**
   * fetches vehicles' location using form data provided by fetchLines
   * @return
   */
  def fetch(): LocationsFetch = {
    logger.info("fetching...")
    val pipeline: HttpRequest => Future[List[Location]] =
      (

        sendReceive
          ~> unmarshal[List[Location]]
        )
    val data = FormData(convertToFormData("bus", bus) ++ convertToFormData("tram", tram) ++ convertToFormData("train", train))
    val response: Future[List[Location]] = pipeline(Post("http://pasazer.mpk.wroc.pl/position.php", data))
    val awaited = Await.result(response, 8.seconds)
    logger.info("fetched " + awaited.toString)
    LocationsFetch(System.currentTimeMillis(), awaited)
  }

  /**
   * quite messy method fetching http page and parsing it for lines' symbols. It cuts string into parts containing
   * symbols and then getLines extracts symbols
   * @return
   */
  private def fetchLines(): (Seq[String], Seq[String], Seq[String]) = {
    var nbus = Seq[String]()
    var ntrain = Seq[String]()
    var ntram = Seq[String]()
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val response: Future[HttpResponse] = pipeline(Get("http://pasazer.mpk.wroc.pl/jak-jezdzimy/mapa-pozycji-pojazdow"))
    val futureResult = Await.result(response, 8.seconds)
    var modifiedString = futureResult.entity.asString
    var stringOps = new StringOps(modifiedString)
    while (stringOps.indexOfSlice("bus_gps_b") != -1) {
      val indexb = stringOps.indexOfSlice("bus_gps_b")
      val endingIndexb = stringOps.indexOfSlice("bus_gps_t")
      val b = stringOps.slice(indexb + 9, endingIndexb)
      modifiedString = stringOps.slice(endingIndexb, modifiedString.length)
      stringOps = new StringOps(modifiedString)
      val indext = 0
      val endingIndexT = stringOps.indexOfSlice("bus_gps_b")
      val t = stringOps.slice(indext + 9, endingIndexT)
      modifiedString = stringOps.slice(endingIndexT, modifiedString.length)
      stringOps = new StringOps(modifiedString)
      nbus = nbus ++ getLines(b)
      ntram = ntram ++ getLines(t)
    }
    def getLines(string: String, acc: Seq[String] = Seq[String]()): Seq[String] = {
      val stringOps = new StringOps(string)
      stringOps.indexOfSlice("bus_line") match {
        case -1 => acc
        case index => val newString = stringOps.slice(index + 10, string.length)
          val stringOps2 = new StringOps(newString)
          val endindex = stringOps2.indexOfSlice("<")
          getLines(newString, stringOps2.slice(0, endindex) +: acc)
      }
    }
    logger.debug("fetched lines: " + (nbus.map(string => string.toLowerCase), ntram.map(string => string.toLowerCase), ntrain.map(string => string.toLowerCase)).toString())
    (nbus.map(string => string.toLowerCase), ntram.map(string => string.toLowerCase), ntrain.map(string => string.toLowerCase))
  }

  /**
   * turns information about lines of given type into Seq of String pairs required by FormData needed in vehicles' positions fetch
   * Rules are following:
   * - if no lines is given - Seq containing only one pair ("busList[$transportType]","")
   * - if there are lines given - Seq containing pairs ("busList[$transportType][]","$line")
   * @param transportType
   * @param lines
   * @return - if no lines is given - Seq containing only one pair ("busList[$transportType]","")
   *         - if there are lines given - Seq containing pairs ("busList[$transportType][]","$line")
   */
  private def convertToFormData(transportType: String, lines: Seq[String]): Seq[(String, String)] = {
    val field = s"busList[$transportType]" + {
      if (lines.isEmpty) "" else "[]"
    }
    if (lines.nonEmpty) lines.map(line => field -> line)
    else Seq((field, ""))
  }

  def refreshLines(): Unit = {
    //(bus,train,tram) = fetchLines() //doesn't work for some reason
    val tuple = fetchLines()
    bus = tuple._1
    train = tuple._3
    tram = tuple._2
  }
}

trait LocationsFetchProtocol extends Marshalling {
  implicit val locationFormat: JsonFormat[Location] = jsonFormat5(Location)
}