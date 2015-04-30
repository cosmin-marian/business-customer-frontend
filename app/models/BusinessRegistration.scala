package models

import play.api.libs.json.Json

case class BusinessRegistration(businessName: String, businessType: String, businessAddress: String, businessPostCode: Int, businessCountry: String, businessTelephone: String, businessEmail: String)

object BusinessRegistration {
  implicit val formats = Json.format[BusinessRegistration]
}
