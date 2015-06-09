package models

import play.api.libs.json.Json

case class EtmpAddress(addressLine1: String, addressLine2: String, addressLine3: Option[String], addressLine4: Option[String],
                   postalCode: Option[String], countryCode: String)

object EtmpAddress {
  implicit val formats = Json.format[EtmpAddress]
}

case class AddressChoice(foreignAddress : EtmpAddress)

object AddressChoice {
  implicit val formats = Json.format[AddressChoice]
}

case class EtmpOrganisation(organisationName : String)

object EtmpOrganisation {
  implicit val formats = Json.format[EtmpOrganisation]
}

case class NonUKIdentification(idNumber : String, issuingInstitution : String, issuingCountryCode : String)

object NonUKIdentification {
  implicit val formats = Json.format[NonUKIdentification]
}

case class NonUKRegistrationRequest(acknowledgmentReference: String,
                        organisation: EtmpOrganisation,
                        address : AddressChoice,
                        isAnAgent : Boolean,
                        isAGroup : Boolean,
                        nonUKIdentification : NonUKIdentification)

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