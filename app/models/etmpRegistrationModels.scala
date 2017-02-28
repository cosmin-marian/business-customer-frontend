package models

import org.joda.time.LocalDate
import play.api.libs.json.Json

case class EtmpAddress(addressLine1: String,
                       addressLine2: String,
                       addressLine3: Option[String],
                       addressLine4: Option[String],
                       postalCode: Option[String],
                       countryCode: String)

object EtmpAddress {
  implicit val formats = Json.format[EtmpAddress]
}

case class EtmpOrganisation(organisationName: String)

object EtmpOrganisation {
  implicit val formats = Json.format[EtmpOrganisation]
}

case class EtmpIndividual(firstName: String,
                      middleName: Option[String] = None,
                      lastName: String,
                      dateOfBirth: LocalDate)

object EtmpIndividual {
  implicit val formats = Json.format[EtmpIndividual]
}

case class EtmpContactDetails(phoneNumber: Option[String] = None,
                              mobileNumber: Option[String] = None,
                              faxNumber: Option[String] = None,
                              emailAddress: Option[String] = None)

object EtmpContactDetails {
  implicit val formats = Json.format[EtmpContactDetails]
}

case class EtmpIdentification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object EtmpIdentification {
  implicit val formats = Json.format[EtmpIdentification]
}

case class BusinessRegistrationRequest(acknowledgementReference: String,
                                       isAnAgent: Boolean,
                                       isAGroup: Boolean,
                                       identification: Option[EtmpIdentification],
                                       organisation: EtmpOrganisation,
                                       address: EtmpAddress,
                                       contactDetails: EtmpContactDetails)

object BusinessRegistrationRequest {
  implicit val formats = Json.format[BusinessRegistrationRequest]
}


case class BusinessRegistrationResponse(processingDate: String,
                                        sapNumber: String,
                                        safeId: String,
                                        agentReferenceNumber: Option[String])

object BusinessRegistrationResponse {
  implicit val formats = Json.format[BusinessRegistrationResponse]
}


case class UpdateRegistrationDetailsRequest(acknowledgementReference: String,
                                            isAnIndividual: Boolean,
                                            individual: Option[EtmpIndividual],
                                            organisation: Option[EtmpOrganisation],
                                            address: EtmpAddress,
                                            contactDetails: EtmpContactDetails,
                                            isAnAgent: Boolean,
                                            isAGroup: Boolean,
                                            identification: Option[EtmpIdentification] = None) {
}

object UpdateRegistrationDetailsRequest {
  implicit val formats = Json.format[UpdateRegistrationDetailsRequest]
}
