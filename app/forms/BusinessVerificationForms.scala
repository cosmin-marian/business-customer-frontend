package forms

import models.BusinessDetails
import play.api.data.Forms._
import play.api.data._

object BusinessVerificationForms {

  val businessDetailsForm = Form(mapping(
      "businessType" -> nonEmptyText
    )(BusinessDetails.apply)(BusinessDetails.unapply)
  )
}