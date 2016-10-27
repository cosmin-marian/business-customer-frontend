package models

import play.api.libs.json.Json

case class ReviewDetails(businessName: String,
                         businessType: Option[String],
                         businessAddress: Address,
                         sapNumber: String,
                         safeId: String,
                         isAGroup: Boolean = false,
                         directMatch: Boolean = false,
                         agentReferenceNumber: Option[String],
                         firstName: Option[String] = None,
                         lastName: Option[String] = None,
                         utr:Option[String] = None)

object ReviewDetails {
  implicit val formats = Json.format[ReviewDetails]
}
