package forms


import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import play.api.libs.json.Json
import utils.BCUtils._


case class SoleTraderMatch(firstName: String, lastName: String, saUTR: String)

case class LimitedCompanyMatch(businessName: String, cotaxUTR: String)

case class UnincorporatedMatch(businessName: String, cotaxUTR: String)

case class OrdinaryBusinessPartnershipMatch(businessName: String, psaUTR: String)

case class LimitedLiabilityPartnershipMatch(businessName: String, psaUTR: String)

case class LimitedPartnershipMatch(businessName: String, psaUTR: String)

case class BusinessType(businessType: Option[String] = None, isSaAccount: Boolean, isOrgAccount: Boolean)

case class BusinessDetails(businessType: String,
                           soleTrader: Option[SoleTraderMatch],
                           ltdCompany: Option[LimitedCompanyMatch],
                           uibCompany: Option[UnincorporatedMatch],
                           obpCompany: Option[OrdinaryBusinessPartnershipMatch],
                           llpCompany: Option[LimitedLiabilityPartnershipMatch],
                           lpCompany: Option[LimitedPartnershipMatch])


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

object LimitedPartnershipMatch {
  implicit val formats = Json.format[LimitedPartnershipMatch]
}

object BusinessType {
  implicit val formats = Json.format[BusinessType]
}

object BusinessDetails {
  implicit val formats = Json.format[BusinessDetails]
}

object BusinessVerificationForms {

  val length40 = 40
  val length0 = 0
  val length105 = 105

  def validateBusinessType(businessTypeData: Form[BusinessType]) = {
    val isSaAccount = businessTypeData.data.get("isSaAccount").fold(false)(x => x.toBoolean)
    val isOrgAccount = businessTypeData.data.get("isOrgAccount").fold(false)(x => x.toBoolean)
    val businessType = businessTypeData.data.get("businessType") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    (businessType.nonEmpty, businessType.getOrElse(""), isSaAccount, isOrgAccount) match {
      case (true, "SOP", false, true) => businessTypeData.withError(key = "businessType",
        message = "bc.business-verification-error.type_of_business_organisation_invalid")
      case (true, "SOP", true, false) => businessTypeData
      case (true, _, true, false) => businessTypeData.withError(key = "businessType",
        message = "bc.business-verification-error.type_of_business_individual_invalid")
      case _ => businessTypeData
    }
  }

  val businessTypeForm = Form(mapping(
    "businessType" -> optional(text).verifying(Messages("bc.business-verification-error.not-selected"), x => x.isDefined),
    "isSaAccount" -> boolean,
    "isOrgAccount" -> boolean
  )(BusinessType.apply)(BusinessType.unapply)
  )

  val soleTraderForm = Form(mapping(
    "firstName" -> text
      .verifying(Messages("bc.business-verification-error.firstname"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.firstname.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length40)),
    "lastName" -> text
      .verifying(Messages("bc.business-verification-error.surname"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.surname.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length40)),
    "saUTR" -> text
      .verifying(Messages("bc.business-verification-error.sautr"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.sautr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("bc.business-verification-error.invalidSAUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))
  )(SoleTraderMatch.apply)(SoleTraderMatch.unapply))

  val limitedCompanyForm = Form(mapping(
    "businessName" -> text
      .verifying(Messages("bc.business-verification-error.businessName"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.registeredName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "cotaxUTR" -> text
      .verifying(Messages("bc.business-verification-error.cotaxutr"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.cotaxutr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("bc.business-verification-error.invalidCOUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))

  )(LimitedCompanyMatch.apply)(LimitedCompanyMatch.unapply))

  val unincorporatedBodyForm = Form(mapping(
    "businessName" -> text
      .verifying(Messages("bc.business-verification-error.businessName"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.registeredName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "cotaxUTR" -> text
      .verifying(Messages("bc.business-verification-error.cotaxutr"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.cotaxutr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("bc.business-verification-error.invalidCOUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))
  )(UnincorporatedMatch.apply)(UnincorporatedMatch.unapply))

  val ordinaryBusinessPartnershipForm = Form(mapping(
    "businessName" -> text
      .verifying(Messages("bc.business-verification-error.businessName"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.registeredName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "psaUTR" -> text
      .verifying(Messages("bc.business-verification-error.psautr"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.psautr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("bc.business-verification-error.invalidPSAUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))
  )(OrdinaryBusinessPartnershipMatch.apply)(OrdinaryBusinessPartnershipMatch.unapply))

  val limitedLiabilityPartnershipForm = Form(mapping(
    "businessName" -> text
      .verifying(Messages("bc.business-verification-error.businessName"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.registeredName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "psaUTR" -> text
      .verifying(Messages("bc.business-verification-error.psautr"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.psautr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("bc.business-verification-error.invalidPSAUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))
  )(LimitedLiabilityPartnershipMatch.apply)(LimitedLiabilityPartnershipMatch.unapply))
  val limitedPartnershipForm = Form(mapping(
    "businessName" -> text
      .verifying(Messages("bc.business-verification-error.businessName"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.registeredName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "psaUTR" -> text
      .verifying(Messages("bc.business-verification-error.psautr"), x => x.length > length0)
      .verifying(Messages("bc.business-verification-error.psautr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("bc.business-verification-error.invalidPSAUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))
  )(LimitedPartnershipMatch.apply)(LimitedPartnershipMatch.unapply))

}
