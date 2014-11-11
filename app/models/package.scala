
import org.joda.time.DateTime

package object models {

  case class Tag(_id: String, total: Long, accepted: Long, rate: Double, top: Long, updated: DateTime)

  object JsonFormats {

    import play.api.libs.json.Json
    import play.api.data._
    import play.api.data.Forms._

    implicit val tagFormat = Json.format[Tag]

  }

}
