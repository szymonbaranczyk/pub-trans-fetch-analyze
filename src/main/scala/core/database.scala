package core

import domain.{ DBLocationsFetches, DBLocations }
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

/**
 * Database configuration
 */
object DatabaseCfg {

  // For H2 in memory database we need to use DB_CLOSE_DELAY
  val db = Database.forURL("jdbc:h2:mem:todo-list;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");

  // create TableQueries for all tables
  val fetchTable: TableQuery[DBLocationsFetches] = TableQuery[DBLocationsFetches]
  val locationsTable: TableQuery[DBLocations] = TableQuery[DBLocations]

  // Initialize database if tables does not exists
  def init() = {
    db.withTransaction { implicit session =>
      if (MTable.getTables("Location").list.isEmpty) {
        fetchTable.ddl.create
      }
      if (MTable.getTables("LocationsFetch").list.isEmpty) {
        locationsTable.ddl.create
      }
    }
  }
}
