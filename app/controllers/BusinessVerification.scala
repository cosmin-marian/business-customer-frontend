package controllers

import controllers.common.BaseController
import play.api.mvc._

object BusinessVerification extends BusinessVerification{

}

trait BusinessVerification extends BaseController{
   def show = Action {
     Ok(views.html.business_verification())
   }
}
