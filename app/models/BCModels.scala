package models

import play.api.libs.json.Json

case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String])

case class Organisation(organisationName: String, organisationType: String)

case class MatchBusinessData(acknowledgmentReference: String,
                             utr: String,
                             requiresNameMatch: Boolean = false,
                             isAnAgent: Boolean = false,
                             individual: Option[Individual],
                             organisation: Option[Organisation])

object Individual {
  implicit val formats = Json.format[Individual]
}

object Organisation {
  implicit val formats = Json.format[Organisation]
}

object MatchBusinessData {
  implicit val formats = Json.format[MatchBusinessData]
}

case class MatchFailureResponse(Reason: String)

object MatchFailureResponse {
  implicit val formats = Json.format[MatchFailureResponse]
}
