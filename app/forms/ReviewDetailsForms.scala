package forms

import models.ReviewDetails
import play.api.data.Form
import play.api.data.Forms._


object ReviewDetailsForms {

  val reviewDetailsForm = Form(mapping(
    "businessName" -> nonEmptyText,
    "businessType" -> nonEmptyText,
    "businessAddress" -> nonEmptyText,
    "telephone" -> number ,
    "email" -> nonEmptyText

  )(ReviewDetails.apply)(ReviewDetails.unapply)
  )
}
