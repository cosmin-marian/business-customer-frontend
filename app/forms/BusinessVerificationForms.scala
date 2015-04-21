package forms

import forms.formValidators.BusinessVerificationValidator._
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._

case class BusinessDetails (businessType: String, soleTrader: SoleTraderMatch, limitedCompany: LimitedCompanyMatch, uib: UnincorporatedMatch, obp :OrdinaryBusinessPartnershipMatch, llp :LimitedLiabilityPartnershipMatch)

case class SoleTraderMatch(sAFirstName: Option[String], sASurname: Option[String], sAUTR: Option[String])

case class LimitedCompanyMatch(ltdBusinessName: Option[String], ltdCotaxUTR: Option[String])

case class UnincorporatedMatch(uibBusinessName: Option[String], uibCotaxUTR: Option[String])

case class OrdinaryBusinessPartnershipMatch(obpBusinessName: Option[String], obpPSAUTR: Option[String])

case class LimitedLiabilityPartnershipMatch(llpBusinessName: Option[String], llpPSAUTR: Option[String])

object BusinessVerificationForms {

  val businessDetailsForm = Form(mapping(
      "businessType" -> nonEmptyText,
      "soleTrader"   -> mapping(
        "sAFirstName" -> optional(text),
        "sASurname"   -> optional(text),
        "sAUTR"   -> optional(text)
    )(SoleTraderMatch.apply)(SoleTraderMatch.unapply),
    "ltdCompany"   -> mapping(
      "ltdBusinessName"   -> optional(text),
      "ltdCotaxAUTR"   -> optional(text)
    )(LimitedCompanyMatch.apply)(LimitedCompanyMatch.unapply),
      "uibCompany"   -> mapping(
        "uibBusinessName"   -> optional(text),
        "uibCotaxAUTR"   -> optional(text)
    )(UnincorporatedMatch.apply)(UnincorporatedMatch.unapply),
    "obpCompany"   -> mapping(
        "obpBusinessName"   -> optional(text),
        "obpPSAUTR"   -> optional(text)
    )(OrdinaryBusinessPartnershipMatch.apply)(OrdinaryBusinessPartnershipMatch.unapply),
    "llpCompany"   -> mapping(
        "llpBusinessName"   -> optional(text),
        "llpPSAUTR"   -> optional(text)
    )(LimitedLiabilityPartnershipMatch.apply)(LimitedLiabilityPartnershipMatch.unapply)
    )(BusinessDetails.apply)(BusinessDetails.unapply)
    verifying("Please enter First name", p => saFirstNameCheck(p))
    verifying("Please enter Surname", p => saSurnameCheck(p))
    verifying("Please enter SA UTR", p => saUTRCheck(p))
    verifying("Please enter Business Name", p => businessNameCheck(p))
    verifying("Please enter COTAX UTR", p => cotaxUTRCheck(p))
    verifying("Please enter Partnership Self Assessment Unique Tax Reference", p => psaUTRCheck(p))

  )

}
