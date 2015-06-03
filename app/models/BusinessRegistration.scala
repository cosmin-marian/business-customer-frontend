package models

import play.api.libs.json.Json


case class BusinessRegistration(businessName: String, businessAddress: Address )

case class Address(line_1: String, line_2: String, line_3: Option[String], line_4: Option[String],
                   postcode: Option[String] = None, country: String) {
  override def toString = {
    val line3display = line_3.map(line3 => s"$line3, " ).getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, " ).getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
}


object Address {
  implicit val formats = Json.format[Address]
}

object BusinessRegistration {
  implicit val formats = Json.format[BusinessRegistration]
}

