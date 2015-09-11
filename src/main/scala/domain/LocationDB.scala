package domain

import scala.slick.driver.H2Driver.simple._
import core.DatabaseCfg._
/**
 * Item case class, stores information about item
 */
case class DBLocation(id: Option[Long], fetchId: Long, name: String, `type`: String, x: Double, y: Double, k: Long)

/**
 * Slick Location table definition
 */
class DBLocations(tag: Tag) extends Table[DBLocation](tag, "Location") {
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def fetchId = column[Long]("fetchId")

  def name = column[String]("name")

  def `type` = column[String]("type")

  def x = column[Double]("x")

  def y = column[Double]("y")

  def k = column[Long]("k")

  def * = (id.?, fetchId, name, `type`, x, y, k) <>((DBLocation.apply _).tupled, DBLocation.unapply)
  def locationsFetch = foreignKey("locationsFetch", fetchId, fetchTable)(_.id, onDelete = ForeignKeyAction.Cascade)
}
case class DBLocationsFetch(id: Option[Long], time: Long)

/**
 * Slick Fetch table definition
 */
class DBLocationsFetches(tag: Tag) extends Table[DBLocationsFetch](tag, "LocationsFetch") {

  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def time = column[Long]("name")

  def * = (id.?, time) <>(DBLocationsFetch.tupled, DBLocationsFetch.unapply)
}