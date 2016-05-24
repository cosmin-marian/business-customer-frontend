package controllers.auth

import models.{BusinessCustomerUser, BusinessCustomerContext}
import play.api.mvc.{Action, Result, AnyContent}
import uk.gov.hmrc.play.frontend.auth.Actions

import scala.concurrent.Future

//scalastyle:off
trait BusinessCustomerHelpers extends Actions {

  def AuthAction(service: String) = new AuthAction(service)

  class AuthAction(service: String) {

    def apply(f: BusinessCustomerContext => Result): Action[AnyContent] = {
      AuthorisedFor(taxRegime = BusinessCustomerRegime(service), pageVisibility = GGConfidence) {
        implicit authContext => implicit request =>
          f(BusinessCustomerContext(request, BusinessCustomerUser(authContext)))
      }
    }

    def async(f: BusinessCustomerContext => Future[Result]): Action[AnyContent] = {
      AuthorisedFor(taxRegime = BusinessCustomerRegime(service), pageVisibility = GGConfidence).async {
        implicit authContext => implicit request =>
          f(BusinessCustomerContext(request, BusinessCustomerUser(authContext)))
      }
    }

  }

}
