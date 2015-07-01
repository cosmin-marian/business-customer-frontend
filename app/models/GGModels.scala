package models

import play.api.libs.json.Json

case class EnrolRequest(portalIdentifier: String, serviceName: String, friendlyName: String,  knownFact: String)

object EnrolRequest {
  implicit val formats = Json.format[EnrolRequest]
}

case class EnrolResponse(serviceName: String, state:String, friendlyName: String, identifiersForDisplay: String)

object EnrolResponse {
  implicit val formats = Json.format[EnrolResponse]
}

