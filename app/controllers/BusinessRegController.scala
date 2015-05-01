package controllers


import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController


object BusinessRegController extends BusinessRegController{

}

trait BusinessRegController extends FrontendController {

  def register = Action {
    Ok("Yess")
  }
}