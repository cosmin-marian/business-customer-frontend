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

  val businessRegistrationForm = Form(
    mapping(
      "businessName" -> text.
        verifying(Messages("bc.business-registration-error.businessName"), x => x.length > length0)
        .verifying(Messages("bc.business-registration-error.businessName.length", length40), x => x.isEmpty || (x.nonEmpty && x.length <= length40)),
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
          .verifying(Messages("ated.error.length", Messages("ated.address.postcode"), postcodeLength),
            x => checkFieldLengthIfPopulated(x, postcodeLength)),
        "country" -> text.
          verifying(Messages("bc.business-registration-error.country"), x => x.length > length0)
          .verifying(Messages("bc.business-registration-error.country.length", length2), x => x.isEmpty || (x.nonEmpty && x.length <= length2))
      )(Address.apply)(Address.unapply),
      "businessUniqueId" -> optional(text)
        .verifying(Messages("bc.business-registration-error.businessUniqueId.length", length60), x => x.isEmpty || (x.nonEmpty && x.get.length <= length60)),
      "issuingInstitution" -> optional(text)
        .verifying(Messages("bc.business-registration-error.issuingInstitution.length", length40), x => x.isEmpty || (x.nonEmpty && x.get.length <= length40))
   )(BusinessRegistration.apply)(BusinessRegistration.unapply)
  )

  def checkFieldLengthIfPopulated(optionValue: Option[String], fieldLength: Int): Boolean = {
    optionValue match {
      case Some(value) => value.isEmpty || (value.nonEmpty && value.length <= fieldLength)
      case None => true
    }
  }


}

