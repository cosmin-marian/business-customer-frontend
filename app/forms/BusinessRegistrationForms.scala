package forms

import models.{BusinessRegistration, Address}
import play.api.data.Form
import play.api.data.Forms._


object BusinessRegistrationForms {

  val businessRegistrationForm = Form(
    mapping(
      "businessName" -> text,
      "businessAddress" -> mapping(
      "line_1" -> text,
      "line_2" -> text,
      "line_3" -> text,
      "line_4" -> text,
      "country" -> text
    )(Address.apply)(Address.unapply)
   )(BusinessRegistration.apply)(BusinessRegistration.unapply)
  )

}
