package models

import play.api.libs.json.Json


case class BusinessRegistration(businessName: String, businessAddress: Address, contactDetails: ContactDetails )

case class Address(line_1: String, line_2: String, line_3: String, line_4: String,  country: String) {

}

case class ContactDetails(telePhoneNumber: String, email: String)


object Address {
  implicit val formats = Json.format[Address]
}

object ContactDetails {
  implicit val formats = Json.format[ContactDetails]
}

object BusinessRegistration {
  implicit val formats = Json.format[BusinessRegistration]
}