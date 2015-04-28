package forms

import forms.formValidators.BusinessVerificationValidator._
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import play.api.libs.json.Json


case class SoleTraderMatch(sAFirstName: Option[String], sASurname: Option[String], sAUTR: Option[Int])

case class LimitedCompanyMatch(ltdBusinessName: Option[String], ltdCotaxAUTR: Option[Int])

case class UnincorporatedMatch(uibBusinessName: Option[String], uibCotaxAUTR: Option[Int])

case class OrdinaryBusinessPartnershipMatch(obpBusinessName: Option[String], obpPSAUTR: Option[Int])

case class LimitedLiabilityPartnershipMatch(llpBusinessName: Option[String], llpPSAUTR: Option[Int])

case class BusinessDetails (businessType: String, soleTrader: SoleTraderMatch, ltdCompany: LimitedCompanyMatch, uibCompany: UnincorporatedMatch, obpCompany :OrdinaryBusinessPartnershipMatch, llpCompany :LimitedLiabilityPartnershipMatch)


object SoleTraderMatch {
  implicit val formats = Json.format[SoleTraderMatch]
}

object LimitedCompanyMatch {
  implicit val formats = Json.format[LimitedCompanyMatch]
}

object UnincorporatedMatch {
  implicit val formats = Json.format[UnincorporatedMatch]
}

object OrdinaryBusinessPartnershipMatch {
  implicit val formats = Json.format[OrdinaryBusinessPartnershipMatch]
}

object LimitedLiabilityPartnershipMatch {
  implicit val formats = Json.format[LimitedLiabilityPartnershipMatch]
}

object BusinessDetails {
  implicit val formats = Json.format[BusinessDetails]
}

object BusinessVerificationForms {

  val businessDetailsForm = Form(mapping(
      "businessType" -> nonEmptyText,
      "soleTrader"   -> mapping(
        "sAFirstName" -> optional(text.verifying(maxLength(40))),
        "sASurname"   -> optional(text.verifying(maxLength(40))),
        "sAUTR"   -> optional(number.verifying(p => String.valueOf(p).length==10))
    )(SoleTraderMatch.apply)(SoleTraderMatch.unapply),
    "ltdCompany"   -> mapping(
      "ltdBusinessName"   -> optional(text.verifying(maxLength(40))),
      "ltdCotaxAUTR"   -> optional(number.verifying(p => String.valueOf(p).length==10))
    )(LimitedCompanyMatch.apply)(LimitedCompanyMatch.unapply),
      "uibCompany"   -> mapping(
        "uibBusinessName"   -> optional(text.verifying(maxLength(40))),
        "uibCotaxAUTR"   -> optional(number.verifying(p => String.valueOf(p).length==10))
    )(UnincorporatedMatch.apply)(UnincorporatedMatch.unapply),
    "obpCompany"   -> mapping(
        "obpBusinessName"   -> optional(text.verifying(maxLength(40))),
        "obpPSAUTR"   -> optional(number.verifying(p => String.valueOf(p).length==10))
    )(OrdinaryBusinessPartnershipMatch.apply)(OrdinaryBusinessPartnershipMatch.unapply),
    "llpCompany"   -> mapping(
        "llpBusinessName"   -> optional(text.verifying(maxLength(40))),
        "llpPSAUTR"   -> optional(number.verifying(p => String.valueOf(p).length==10))
    )(LimitedLiabilityPartnershipMatch.apply)(LimitedLiabilityPartnershipMatch.unapply)
    )(BusinessDetails.apply)(BusinessDetails.unapply)
    verifying(Messages("bc.business-verification-error.firstname"), p => saFirstNameCheck(p))
    verifying(Messages("bc.business-verification-error.surname"), p => saSurnameCheck(p))
    verifying(Messages("bc.business-verification-error.sautr"), p => saUTREmptyCheck(p))
    verifying(Messages("bc.business-verification-error.businessName"), p => businessNameCheck(p))
    verifying(Messages("bc.business-verification-error.cotaxutr"), p => cotaxUTREmptyCheck(p))
    verifying(Messages("bc.business-verification-error.psautr"), p => psaUTREmptyCheck(p))
    verifying(Messages("bc.business-verification-error.invalidSAUTR"), p => validateSAUTR(p))
    verifying(Messages("bc.business-verification-error.invalidCOUTR"), p => validateCOUTR(p))
    verifying(Messages("bc.business-verification-error.invalidPSAUTR"), p => validatePSAUTR(p))
  )

}
