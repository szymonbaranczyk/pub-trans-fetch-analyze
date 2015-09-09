class Foo(s:String)(implicit val impInt: Int)

val seq=Seq(("a","b"),("a","c"))
seq.toMap