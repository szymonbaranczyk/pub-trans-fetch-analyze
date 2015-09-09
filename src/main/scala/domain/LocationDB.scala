package domain

import scala.slick.driver.H2Driver.simple._
import core.DatabaseCfg._
/**
 * Item case class, stores information about item
 */
case class DBLocation(id: Option[Long], fetchId: Option[Long], name: String, `type`: String,x:Double,y:Double,k:Long)

/**
 * Slick Item table definition
 */
class DBLocations(tag: Tag) extends Table[DBLocation](tag, "Location") {
  def id: Column[Long] = column[Long]("id", O.AutoInc, O.NotNull, O.PrimaryKey)
  def fetchId: Column[Long] = column[Long]("fetchId", O.NotNull)
  def name: Column[String] = column[String]("name", O.NotNull)
  def `type`: Column[String] = column[String]("type", O.NotNull)
  def x: Column[Double] = column[Double]("x", O.NotNull)
  def y: Column[Double] = column[Double]("y", O.NotNull)
  def k: Column[Long] = column[Long]("k", O.NotNull)
  def * = (id.?, fetchId.?, name, `type`, x, y, k) <> (DBLocation.tupled, DBLocation.unapply _)
  def locationsFetch = foreignKey("locationsFetch", fetchId, fetchTable)(_.id, onDelete = ForeignKeyAction.Cascade)
}
case class DBLocationsFetch(id: Option[Long], time: Long)

/**
 * Slick Todo table definition
 */
class DBLocationsFetches(tag: Tag) extends Table[DBLocationsFetch](tag, "LocationsFetch") {

  def id: Column[Long] = column[Long]("id", O.NotNull, O.AutoInc, O.PrimaryKey)
  def time: Column[Long] = column[Long]("name", O.NotNull)
  def * = (id.?, time) <> (DBLocationsFetch.tupled, DBLocationsFetch.unapply _)
}