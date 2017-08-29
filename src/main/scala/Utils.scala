object Utils {

}


object ShapelessUtils {

  import shapeless._
  import labelled.{FieldType, field}

  /*
  Usage:
  import shapeless._
  import shapeless.record._
  case class Book(author: String, title: String)
  implicit val bookGen = LabelledGeneric[Book]
  val aBookMap = Map("author" -> "John Updike", "title" -> "hello")
  val result: Option[Book] = Helper.to[Book].from(aBookMap)
   */

  trait FromMap[L <: HList] {
    def apply(m: Map[String, Any]): Option[L]
  }

  object FromMap {
    implicit val hnilFromMap: FromMap[HNil] = new FromMap[HNil] {
      def apply(m: Map[String, Any]): Option[HNil] = Some(HNil)
    }

    implicit def hconsFromMap[K <: Symbol, V, T <: HList](implicit
                                                          witness: Witness.Aux[K],
                                                          typeable: Typeable[V],
                                                          fromMapT: FromMap[T]): FromMap[FieldType[K, V] :: T] = new FromMap[FieldType[K, V] :: T] {
      def apply(m: Map[String, Any]): Option[FieldType[K, V] :: T] = for {
        v <- m.get(witness.value.name.toString)
        r <- typeable.cast(v)
        t <- fromMapT(m)
      } yield field[K][V](r) :: t
    }
  }

  class Helper[A] {
    def from[R <: HList](m: Map[String, AnyRef])(implicit
                                              gen: LabelledGeneric.Aux[A, R],
                                              fromMap: FromMap[R]): Option[A] = {
      fromMap(m).map(gen.from(_))
    }
  }

  object Helper {
    def to[A]: Helper[A] = new Helper[A]
  }

}
