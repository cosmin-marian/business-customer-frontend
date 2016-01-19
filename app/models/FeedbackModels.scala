package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

case class FeedBack(easyToUse: Option[Int] = None,
                    satisfactionLevel: Option[Int] = None,
                    howCanWeImprove: Option[String] = None,
                    referer: Option[String] = None
                   )

object FeedBack {
  implicit val formats = Json.format[FeedBack]
}

object FeedbackForm {

  val maxStringLength = 1200
  val maxOptionIntSize = 4
  val feedbackForm = Form(mapping(
    "easyToUse" -> optional(number(min = 0, max = maxOptionIntSize)),
    "satisfactionLevel" -> optional(number(min = 0, max = maxOptionIntSize)),
    "howCanWeImprove" -> optional(text(maxLength = maxStringLength)),
    "referer" -> optional(text)
  )
  (FeedBack.apply)(FeedBack.unapply))

}
