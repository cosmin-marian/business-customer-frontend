package controllers

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import forms.BusinessVerificationForms._
import models.ReviewDetails
import play.api.mvc._
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.frontend.auth.Actions
import controllers.auth.BusinessCustomerRegime

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  override val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
}

trait BusinessVerificationController extends FrontendController with Actions {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

   def businessVerification(service: String) = AuthorisedFor(BusinessCustomerRegime) {
     implicit user => implicit request =>
     Ok(views.html.business_verification(businessDetailsForm, service))
   }

  def submit(service: String) = UnauthorisedAction.async {  implicit request =>
    businessDetailsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_verification(formWithErrors, service))),
      value => {
        if(value.businessType == """NUK"""){
          Future.successful(Redirect(controllers.routes.BusinessVerificationController.helloWorld("NON-UK")))
        }else {
          businessMatchingConnector.lookup(value) flatMap {
            actualResponse => {
              if (actualResponse.toString() contains ("error")) {
                Future.successful(Redirect(controllers.routes.BusinessVerificationController.helloWorld(actualResponse.toString())))
              } else {
                dataCacheConnector.saveReviewDetails(actualResponse.as[ReviewDetails]) flatMap {
                  cachedData =>
                  Future.successful(Redirect(controllers.routes.ReviewDetailsController.businessDetails(service)))
                }
              }
            }
          }
        }
      }
    )
  }

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }
}
