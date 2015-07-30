package models

import play.api.libs.json.Json

case class EnrolRequest(portalId: String, serviceName: String, friendlyName: String,  knownFacts: Seq[String])

object EnrolRequest {
  implicit val formats = Json.format[EnrolRequest]
}

case class Identifier(`type`: String, value: String)

object Identifier {
  implicit val formats = Json.format[Identifier]
}

case class EnrolResponse(serviceName: String, state:String, identifiers: Seq[Identifier])

object EnrolResponse {
  implicit val formats = Json.format[EnrolResponse]
}

