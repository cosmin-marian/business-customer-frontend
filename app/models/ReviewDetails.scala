package models

import play.api.libs.json.Json

case class ReviewDetails(businessName: String,
                         businessType: String,
                         businessAddress: Address,
                         sapNumber: String,
                         safeId: String,
                         agentReferenceNumber: String,
                         firstName : Option[String] = None,
                         lastName : Option[String] = None)

object ReviewDetails {
  implicit val formats = Json.format[ReviewDetails]
}
