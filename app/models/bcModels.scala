package models

import play.api.libs.json.Json

case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String])

case class Organisation(organisationName: String, organisationType: String)

case class OrganisationResponse(organisationName: String, isAGroup: Option[Boolean], organisationType: Option[String])

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

object OrganisationResponse {
  implicit val formats = Json.format[OrganisationResponse]
}


object MatchBusinessData {
  implicit val formats = Json.format[MatchBusinessData]
}

case class MatchFailureResponse(Reason: String)

object MatchFailureResponse {
  implicit val formats = Json.format[MatchFailureResponse]
}

case class MatchSuccessResponse(isAnIndividual: Boolean,
                                agentReferenceNumber: Option[String],
                                sapNumber: Option[String],
                                safeId: String,
                                address: EtmpAddress,
                                organisation: Option[OrganisationResponse] = None,
                                individual: Option[Individual] = None)

object MatchSuccessResponse {
  implicit val format = Json.format[MatchSuccessResponse]
}


case class NRLQuestion(paysSA: Option[Boolean] = None)

object NRLQuestion {
  implicit val formats = Json.format[NRLQuestion]
}

case class ClientPermission(permission: Option[Boolean] = None)

object ClientPermission {
  implicit val formats = Json.format[ClientPermission]
}





