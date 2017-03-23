package utils

import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HttpResponse


object ErrorMessageUtils {

  val uniqueAgentErrorMsg = "The service HMRC-AGENT-AGENT requires unique identifiers"
  val uniqueAgentErrorNum = "9001"

  def parseErrorResp(resp: HttpResponse, errorNumber: String): Boolean = {
     val msgToXml = scala.xml.XML.loadString((resp.json \ "message").as[String])
       (msgToXml \\ "ErrorNumber").text == errorNumber
  }

}
