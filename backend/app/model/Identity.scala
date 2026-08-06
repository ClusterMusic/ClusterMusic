package model

import play.api.libs.json.{Format, JsNumber, Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._


case class Identity[T](value: Int) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Identity[_] => this.value == that.value
    case _ => false
  }

  override def hashCode(): Int = value.hashCode()
}

object Identity {

  implicit def identityMapping[T]: JdbcType[Identity[T]] with BaseTypedType[Identity[T]] = MappedColumnType.base[Identity[T], Int](
      identity => identity.value,
      str => Identity[T](str)
    )

  implicit def eitherIdentityMapping[L, R]: JdbcType[Either[Identity[L], Identity[R]]] with BaseTypedType[Either[Identity[L], Identity[R]]] = MappedColumnType.base[Either[Identity[L], Identity[R]], String](
      {
        case Left(identity)  => s"A:${identity.value}"
        case Right(identity) => s"B:${identity.value}"
      },
      str => str.split(":", 2) match {
        case Array("A", value) => Left(Identity[L](value.toInt))
        case Array("B", value) => Right(Identity[R](value.toInt))
        case _ => throw new IllegalArgumentException(s"Invalid Either Identity format: $str")
      }
    )

  implicit def identityFormat[T]: Format[Identity[T]] = Format(
    Reads.IntReads.map(Identity[T](_)),
    Writes(id => JsNumber(id.value))
  )

  implicit def identityPathBindable[T]: PathBindable[Identity[T]] = new PathBindable[Identity[T]] {
    def bind(key: String, value: String): Either[String, Identity[T]] = {
      try {
        val intValue = value.toInt
        Right(Identity[T](intValue))
      } catch {
        case _: NumberFormatException => Left(s"Cannot parse parameter $key as Identity: $value is not a valid integer")
      }
    }

    def unbind(key: String, identity: Identity[T]): String = {
      identity.value.toString
    }
  }

  implicit def identityQueryStringBindable[T]: QueryStringBindable[Identity[T]] = new QueryStringBindable[Identity[T]] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Identity[T]]] = {
      params.get(key).flatMap(_.headOption) match {
        case Some(value) =>
          try {
            val intValue = value.toInt
            Some(Right(Identity[T](intValue)))
          } catch {
            case _: NumberFormatException => Some(Left(s"Cannot parse parameter $key as Identity: $value is not a valid integer"))
          }
        case None => None
      }
    }

    def unbind(key: String, identity: Identity[T]): String = {
      s"$key=${identity.value}"
    }
  }
}


