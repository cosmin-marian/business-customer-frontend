package models

import play.api.libs.json.Json


case class BusinessRegistration(businessName: String, businessAddress: Address )

case class Address(line_1: String, line_2: String, line_3: Option[String], line_4: Option[String],  country: String) {

}


object Address {
  implicit val formats = Json.format[Address]
}

object BusinessRegistration {
  implicit val formats = Json.format[BusinessRegistration]
}

