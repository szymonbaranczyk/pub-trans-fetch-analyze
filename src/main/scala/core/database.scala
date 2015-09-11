package core

import domain.{ DBLocationsFetches, DBLocations }
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Database configuration
 */
object DatabaseCfg {

  // For H2 in memory database we need to use DB_CLOSE_DELAY
  val db = Database.forURL("jdbc:h2:mem:locations;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");

  // create TableQueries for all tables
  val fetchTable: TableQuery[DBLocationsFetches] = TableQuery[DBLocationsFetches]
  val locationsTable: TableQuery[DBLocations] = TableQuery[DBLocations]

  // Initialize database if tables does not exists
  def init() = {
    //if(MTable.getTables("Location"))
    val setup = DBIO.seq(
      (fetchTable.schema ++ locationsTable.schema).create
    )
    Await.result(db.run(setup), Duration.Inf)
  }
}
