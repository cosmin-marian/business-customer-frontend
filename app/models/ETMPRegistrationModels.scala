package models

import play.api.libs.json.Json

case class EtmpAddress(addressLine1: String, addressLine2: String, addressLine3: Option[String], addressLine4: Option[String],
                   postalCode: Option[String], countryCode: String)

object EtmpAddress {
  implicit val formats = Json.format[EtmpAddress]
}

case class EtmpOrganisation(organisationName : String)

object EtmpOrganisation {
  implicit val formats = Json.format[EtmpOrganisation]
}

case class EtmpContactDetails(phoneNumber: Option[String] = None,
                              mobileNumber: Option[String] = None,
                              faxNumber: Option[String] = None,
                              eMailAddress: Option[String] = None)

object EtmpContactDetails {
  implicit val formats = Json.format[EtmpContactDetails]
}

case class NonUKIdentification(idNumber: Option[String], issuingInstitution : Option[String], issuingCountryCode : Option[String])

object NonUKIdentification {
  implicit val formats = Json.format[NonUKIdentification]
}

case class NonUKRegistrationRequest(acknowledgmentReference: String,
                        organisation: EtmpOrganisation,
                        address: EtmpAddress,
                        isAnAgent: Boolean,
                        isAGroup: Boolean,
                        nonUKIdentification: Option[NonUKIdentification],
                        contactDetails: EtmpContactDetails)

object NonUKRegistrationRequest {
  implicit val formats = Json.format[NonUKRegistrationRequest]
}


case class NonUKRegistrationResponse(processingDate : String,
                                     sapNumber: String,
                                     safeId: String,
                                     agentReferenceNumber : String)

object NonUKRegistrationResponse {
  implicit val formats = Json.format[NonUKRegistrationResponse]
}
