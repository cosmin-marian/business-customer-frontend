package models

import play.api.libs.json.Json

case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object Identification {
  implicit val formats = Json.format[Identification]
}


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
                         utr: Option[String] = None,
                         identification: Option[Identification] = None)

object ReviewDetails {
  implicit val formats = Json.format[ReviewDetails]
}
