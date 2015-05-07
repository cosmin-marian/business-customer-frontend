package forms

import models.{BusinessRegistration, Address}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages


object BusinessRegistrationForms {

  val businessRegistrationForm = Form(
    mapping(
      "businessName" -> text.
        verifying(Messages("bc.business-registration-error.businessName"), x => x.length > 0),
      "businessAddress" -> mapping(
      "line_1" -> text.
        verifying(Messages("bc.business-registration-error.line_1"), x => x.length > 0),
      "line_2" -> text.
        verifying(Messages("bc.business-registration-error.line_2"), x => x.length > 0),
      "line_3" -> optional(text),
      "line_4" -> optional(text),
      "country" -> text.
        verifying(Messages("bc.business-registration-error.country"), x => x.length > 0)
    )(Address.apply)(Address.unapply)
   )(BusinessRegistration.apply)(BusinessRegistration.unapply)
  )

}

