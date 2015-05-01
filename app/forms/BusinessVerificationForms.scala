package forms


import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import play.api.libs.json.Json


case class SoleTraderMatch(firstName: String, surname: String, saUTR: Long)

case class LimitedCompanyMatch(businessName: String, cotaxUTR: Long)

case class UnincorporatedMatch(businessName: String, cotaxUTR: Long)

case class OrdinaryBusinessPartnershipMatch(businessName: String, psaUTR: Long)

case class LimitedLiabilityPartnershipMatch(businessName: String, psaUTR: Long)

case class BusinessType (businessType: String)


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

object BusinessType {
  implicit val formats = Json.format[BusinessType]
}

object BusinessVerificationForms {

  val businessTypeForm = Form(mapping(
      "businessType" -> nonEmptyText
    )(BusinessType.apply)(BusinessType.unapply)
//    verifying(Messages("bc.business-verification-error.firstname"), p => saFirstNameCheck(p))
//    verifying(Messages("bc.business-verification-error.surname"), p => saSurnameCheck(p))
//    verifying(Messages("bc.business-verification-error.sautr"), p => saUTREmptyCheck(p))
//    verifying(Messages("bc.business-verification-error.businessName"), p => businessNameCheck(p))
//    verifying(Messages("bc.business-verification-error.cotaxutr"), p => cotaxUTREmptyCheck(p))
//    verifying(Messages("bc.business-verification-error.psautr"), p => psaUTREmptyCheck(p))
//    verifying(Messages("bc.business-verification-error.invalidSAUTR"), p => validateSAUTR(p))
//    verifying(Messages("bc.business-verification-error.invalidCOUTR"), p => validateCOUTR(p))
//    verifying(Messages("bc.business-verification-error.invalidPSAUTR"), p => validatePSAUTR(p))
  )

  val soleTraderForm = Form(mapping(
      "firstName" -> nonEmptyText.verifying(maxLength(40)),
      "surname"   -> nonEmptyText.verifying(maxLength(40)),
      "saUTR"   -> longNumber.verifying(p => String.valueOf(p).length==10)
    )(SoleTraderMatch.apply)(SoleTraderMatch.unapply)
  )

  val limitedCompanyForm = Form(mapping(
    "businessName"   -> nonEmptyText.verifying(maxLength(40)),
    "cotaxUTR"   -> longNumber.verifying(p => String.valueOf(p).length==10)
  )(LimitedCompanyMatch.apply)(LimitedCompanyMatch.unapply))

  val unincorporatedBodyForm = Form(mapping(
    "businessName"   -> nonEmptyText.verifying(maxLength(40)),
    "cotaxUTR"   -> longNumber.verifying(p => String.valueOf(p).length==10)
  )(UnincorporatedMatch.apply)(UnincorporatedMatch.unapply))

  val ordinaryBusinessPartnershipForm = Form(mapping(
    "businessName"   -> nonEmptyText.verifying(maxLength(40)),
    "psaUTR"   -> longNumber.verifying(p => String.valueOf(p).length==10)
  )(OrdinaryBusinessPartnershipMatch.apply)(OrdinaryBusinessPartnershipMatch.unapply))

  val limitedLiabilityPartnershipForm = Form(mapping(
    "businessName"   -> nonEmptyText.verifying(maxLength(40)),
    "psaUTR"   -> longNumber.verifying(p => String.valueOf(p).length==10)
  )(LimitedLiabilityPartnershipMatch.apply)(LimitedLiabilityPartnershipMatch.unapply))

}
