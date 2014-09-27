import java.util.Date

/**
 * Created by leo on 27.09.2014.
 */
package object models {

  case class Tag(_id: String, total: Long, accepted: Long, updated: Date)

  object JsonFormats {

    import play.api.libs.json.Json
    import play.api.data._
    import play.api.data.Forms._

    // Generates Writes and Reads for Feed and User thanks to Json Macros
    implicit val tagFormat = Json.format[Tag]

  }

}
