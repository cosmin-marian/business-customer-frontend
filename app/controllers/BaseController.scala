package controllers

import controllers.auth.BusinessCustomerHelpers
import models.BusinessCustomerContext
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.auth._

trait BaseController extends FrontendController with BusinessCustomerHelpers {
  implicit def bcContext2Request(implicit bcc: BusinessCustomerContext): Request[AnyContent] = bcc.request
  implicit def bcContext2AuthContext(implicit bcc: BusinessCustomerContext): AuthContext = bcc.user.authContext
}
