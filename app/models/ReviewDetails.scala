package models

import play.api.libs.json.Json

case class ReviewDetails(businessName: String, businessType: String, businessAddress: Address)

object ReviewDetails {
  implicit val formats = Json.format[ReviewDetails]
}
