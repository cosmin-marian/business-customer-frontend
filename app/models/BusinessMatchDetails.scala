package models

import play.api.libs.json.Json

case class BusinessMatchDetails(matchUtr: Boolean, utr: String, individual: Option[Individual], organisation: Option[Organisation])

case class Individual(firstName: String, lastName: String, dob: String, saUtr: String)

case class Organisation(businessName: String, ctUtr: String)

object Individual {
  implicit val formats = Json.format[Individual]
}

object Organisation {
  implicit val formats = Json.format[Organisation]
}

object BusinessMatchDetails {
  implicit val formats = Json.format[BusinessMatchDetails]
}

