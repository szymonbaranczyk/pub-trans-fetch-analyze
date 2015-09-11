package service


import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef}
import core.DatabaseCfg._
import domain._
import slick.dbio.DBIO
import spray.util.LoggingContext
import scala.concurrent.{Await, Future}
import slick.driver.H2Driver.api._
import scala.concurrent.duration._

class LocationActor(implicit log: LoggingContext) extends Actor with LocationDAO {
  override def receive: Receive = {
    case loc: LocationsFetch => log.info("inserting fetch")
      insert(loc)
    case rec => log.debug("ups, received: " + rec.toString)
  }
}

trait LocationDAO {
  def insert(locationsFetch: LocationsFetch) = {
    val insertFetch =
      (fetchTable returning fetchTable.map(_.id)) += DBLocationsFetch(None, locationsFetch.time)
    val id = Await.result(db.run(insertFetch), 10 seconds)
    val insertList = locationsTable ++= locationsFetch.list.map(loc => DBLocation(None, id, loc.name, loc.`type`, loc.x, loc.y, loc.k))
    db.run(insertList)

  }
}