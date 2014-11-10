
import org.joda.time.DateTime

package object models {

  case class Tag(_id: String, total: Long, accepted: Long, rate: Double, top: Long, updated: DateTime)

  case class RateTops(_id: String, top10: Double, top20: Double, top30: Double, top40: Double, top50: Double, updated: DateTime)

  object JsonFormats {

    import play.api.libs.json.Json
    import play.api.data._
    import play.api.data.Forms._

    // Generates Writes and Reads for Feed and User thanks to Json Macros
    implicit val tagFormat = Json.format[Tag]
    implicit val rateTopsFormat = Json.format[RateTops]

  }

}
