package models

import play.api.libs.json.Json
import uk.gov.hmrc.domain.{SaUtr, CtUtr}

case class BusinessMatchDetails(matchUtr: Boolean, utr: String, individual: Option[Individual], organisation: Option[Organisation])

case class Individual(firstName: String, lastName: String, dob: String, saUtr: SaUtr)

case class Organisation(businessName: String, ctUtr: CtUtr)

object Individual {
  implicit val formats = Json.format[Individual]
}

object Organisation {
  implicit val formats = Json.format[Organisation]
}

object BusinessMatchDetails {
  implicit val formats = Json.format[BusinessMatchDetails]
}

