package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.BusinessVerificationForms._
import models.{BusinessMatchDetails, ReviewDetails}
import play.api.mvc._
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  override val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector = FrontendAuthConnector
}

trait BusinessVerificationController extends FrontendController with Actions {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

  def businessVerification(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_verification(businessDetailsForm, service))
  }

  def submit(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
    businessDetailsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_verification(formWithErrors, service))),
      value => {
        if(value.businessType == """NUK"""){
          Future.successful(Redirect(controllers.routes.BusinessRegController.register(service)))
        }else {
          businessMatchingConnector.lookup(BusinessMatchDetails(true, "1234567890", None, None)) flatMap {
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

