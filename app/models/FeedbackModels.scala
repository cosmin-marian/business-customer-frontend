package models

import play.api.data.Form
import play.api.data.Forms._

case class FeedBack(easyToUse: Option[Int],
                    satisfactionLevel: Option[Int],
                    howCanWeImprove: Option[String],
                    referrer: Option[String]
                   )

object FeedbackForm {

  val maxStringLength = 1200
  val maxOptionIntSize = 4
  val feedbackForm = Form(mapping(
    "easyToUse" -> optional(number(min = 0, max = maxOptionIntSize)),
    "satisfactionLevel" -> optional(number(min = 0, max = maxOptionIntSize)),
    "howCanWeImprove" -> optional(text(maxLength = maxStringLength)),
    "referrer" -> optional(text)
  )
  (FeedBack.apply)(FeedBack.unapply))

}
