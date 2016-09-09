package forms

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json.Json


object BusinessRegistrationForms {

  val postcodeLength = 10
  val length40 = 40
  val length35 = 35
  val length0 = 0
  val length2 = 2
  val length60 = 60
  val length105 = 105
  // scalastyle:off line.size.limit
  val postcodeRegex =
    """(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"""

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
      "hasBusinessUniqueId" -> optional(boolean).verifying(Messages("bc.business-registration-error.hasBusinessUniqueId.not-selected"), x => x.isDefined),
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

  def validateNonUK(registrationData: Form[BusinessRegistration]): Form[BusinessRegistration] = {
    validateNonUkIdentifiers(validateCountryNonUK(registrationData))
  }

  def validateUK(registrationData: Form[BusinessRegistration]): Form[BusinessRegistration] = {
    validateUkIdentifiers(validatePostCode(registrationData))
  }

  def validateUkIdentifiers(registrationData: Form[BusinessRegistration]) = {
    registrationData
  }

  def validateNonUkIdentifiers(registrationData: Form[BusinessRegistration]): Form[BusinessRegistration] = {
    validateNonUkIdentifiersInstitution(validateNonUkIdentifiersCountry(validateNonUkIdentifiersId(registrationData)))
  }

  def validateNonUkIdentifiersInstitution(registrationData: Form[BusinessRegistration]) = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val issuingInstitution = registrationData.data.get("issuingInstitution") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if issuingInstitution.isEmpty =>
        registrationData.withError(key = "issuingInstitution", message = Messages("bc.business-registration-error.issuingInstitution.select"))
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersCountry(registrationData: Form[BusinessRegistration]) = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val issuingCountry = registrationData.data.get("issuingCountry") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if issuingCountry.isEmpty =>
        registrationData.withError(key = "issuingCountry", message = Messages("bc.business-registration-error.issuingCountry.select"))
      case Some(true) if issuingCountry.isDefined && issuingCountry.fold("")(x => x).matches(countryUK) =>
        registrationData.withError(key = "issuingCountry", message = Messages("bc.business-registration-error.non-uk"))
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersId(registrationData: Form[BusinessRegistration]) = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val businessUniqueId = registrationData.data.get("businessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if businessUniqueId.isEmpty =>
        registrationData.withError(key = "businessUniqueId", message = Messages("bc.business-registration-error.businessUniqueId.select"))
      case _ => registrationData
    }
  }

  private def validatePostCode(registrationData: Form[BusinessRegistration]) = {
    val postCode = registrationData.data.get("businessAddress.postcode") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    if (postCode.isEmpty) {
      registrationData.withError(key = "businessAddress.postcode",
        message = Messages("bc.business-registration-error.postcode"))
    } else {
      if (!postCode.fold("")(x => x).matches(postcodeRegex)) {
        registrationData.withError(key = "businessAddress.postcode",
          message = Messages("bc.business-registration-error.postcode.invalid"))
      } else {
        registrationData
      }
    }
  }

  private def validateCountryNonUK(registrationData: Form[BusinessRegistration]) = {
    val country = registrationData.data.get("businessAddress.country") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    if (country.fold("")(x => x).matches(countryUK)) {
      registrationData.withError(key = "businessAddress.country", message = Messages("bc.business-registration-error.non-uk"))
    } else {
      registrationData
    }

  }

  val nrlQuestionForm = Form(
    mapping(
      "paysSA" -> optional(boolean).verifying(Messages("bc.nrl.paysSA.not-selected.error"), a => a.isDefined)
    )(NRLQuestion.apply)(NRLQuestion.unapply)
  )

  val clientPermissionForm = Form(
    mapping(
      "permission" -> optional(boolean).verifying(Messages("bc.permission.not-selected.error"), a => a.isDefined)
    )(ClientPermission.apply)(ClientPermission.unapply)
  )

}
