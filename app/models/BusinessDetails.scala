package models

import play.api.libs.json.Json

case class ReviewDetails(businessName: String, businessType: String, businessAddress: String, businessTelephone: String, businessEmail: String)

object ReviewDetails {
  implicit val formats = Json.format[ReviewDetails]
}
