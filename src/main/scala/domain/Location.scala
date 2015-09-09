package domain

/**
 * Created by SBARANCZ on 01.09.2015.
 */
case class Location(name:String,`type`:String,x:Double,y:Double,k:Long)
case class LocationsFetch(time: Long, list: List[Location])
