package domain


case class Location(name:String,`type`:String,x:Double,y:Double,k:Long)
case class LocationsFetch(time: Long, list: List[Location])
