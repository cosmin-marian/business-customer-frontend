package utils

import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HttpResponse


object ErrorMessageUtils {

  val uniqueAgentErrorNum = "9001"
  val multipleAgentErrorNum = "10004"

  def matchErrorResponse(resp: HttpResponse): Boolean = {
     val msgToXml = scala.xml.XML.loadString((resp.json \ "message").as[String])
       (msgToXml \\ "ErrorNumber").text == uniqueAgentErrorNum || (msgToXml \\ "ErrorNumber").text == multipleAgentErrorNum
  }

}
