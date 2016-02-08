package forms

import models.{Address, BusinessRegistration}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

object BusinessRegistrationForms {

  val postcodeLength = 10
  val length40 = 40
  val length35 = 35
  val length0 = 0
  val length2 = 2
  val length60 = 60
  val length105 = 105
  // scalastyle:off line.size.limit
  val postcodeRegex = """(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"""

  val countryUK = "GB"

  val businessRegistrationForm = Form(
    mapping(
      "businessName" -> text.
        verifying(Messages("bc.business-registration-error.businessName"), x => x.length > length0)
        .verifying(Messages("bc.business-registration-error.businessName.length", length105), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
      "businessAddress" -> mapping(
        "line_1" -> text.
          verifying(Messages("bc.business-registration-error.line_1"), x => x.length > length0)
          .verifying(Messages("bc.business-registration-error.line_1.length", length35), x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "line_2" -> text.
          verifying(Messages("bc.business-registration-error.line_2"), x => x.length > length0)
          .verifying(Messages("bc.business-registration-error.line_2.length", length35), x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "line_3" -> optional(text)
          .verifying(Messages("bc.business-registration-error.line_3.length", length35), x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "line_4" -> optional(text)
          .verifying(Messages("bc.business-registration-error.line_4.length", length35), x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "postcode" -> optional(text)
          .verifying(Messages("bc.business-registration-error.postcode.length", postcodeLength),
            x => x.isEmpty || (x.nonEmpty && x.get.length <= postcodeLength)),
        "country" -> text.
          verifying(Messages("bc.business-registration-error.country"), x => x.length > length0)

      )(Address.apply)(Address.unapply),
      "businessUniqueId" -> optional(text)
        .verifying(Messages("bc.business-registration-error.businessUniqueId.length", length60), x => x.isEmpty || (x.nonEmpty && x.get.length <= length60)),
      "issuingInstitution" -> optional(text)
        .verifying(Messages("bc.business-registration-error.issuingInstitution.length", length40), x => x.isEmpty || (x.nonEmpty && x.get.length <= length40)),
      "issuingCountry" -> optional(text)
    )(BusinessRegistration.apply)(BusinessRegistration.unapply)
  )

  def checkFieldLengthIfPopulated(optionValue: Option[String], fieldLength: Int): Boolean = {
    optionValue match {
      case Some(value) => value.isEmpty || (value.nonEmpty && value.length <= fieldLength)
      case None => true
    }
  }

  def validateNonUK(registrationData: Form[BusinessRegistration]) :Form[BusinessRegistration] = {
    validateNonUkInstitution(validateCountryNonUK(registrationData))
  }

  def validateUK(registrationData: Form[BusinessRegistration]) :Form[BusinessRegistration] = {
    validateUkInstitution(validatePostCode(registrationData))
  }

  def validateUkInstitution(registrationData: Form[BusinessRegistration]) = {
      registrationData
  }

  def validateNonUkInstitution(registrationData: Form[BusinessRegistration]) = {
    val businessUniqueId = registrationData.data.get("businessUniqueId") map {_.trim} filterNot {_.isEmpty}
    val issuingInstitution = registrationData.data.get("issuingInstitution") map {_.trim} filterNot {_.isEmpty}
    (businessUniqueId, issuingInstitution) match {
      case (Some(id), None) => registrationData.withError(key = "issuingInstitution",
        message = Messages("bc.business-registration-error.issuingInstitution.select"))
      case(None, Some(inst)) => registrationData.withError(key = "businessUniqueId",
        message = Messages("bc.business-registration-error.businessUniqueId.select"))
      case _ => registrationData
    }
  }

  private def validatePostCode(registrationData: Form[BusinessRegistration]) = {
    val postCode = registrationData.data.get("businessAddress.postcode") map {_.trim} filterNot {_.isEmpty}
    if (!postCode.isDefined) {
      registrationData.withError(key = "businessAddress.postcode",
        message = Messages("bc.business-registration-error.postcode"))
    } else {
      if(!postCode.fold("")(x => x).matches(postcodeRegex)) {
        registrationData.withError(key = "businessAddress.postcode",
          message = Messages("bc.business-registration-error.postcode.invalid"))
      } else {
        registrationData
      }
    }
  }


  private def validateCountryNonUK(registrationData: Form[BusinessRegistration]) = {
    val country = registrationData.data.get("businessAddress.country") map {_.trim} filterNot {_.isEmpty}
      if(country.fold("")(x => x).matches(countryUK)) {
        registrationData.withError(key = "businessAddress.country",
          message = Messages("bc.business-verification-error.non-uk"))
      } else {
        registrationData
      }

  }


}
