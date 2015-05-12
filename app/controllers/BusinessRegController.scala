package controllers


import play.api.Play

import connectors.{BusinessCustomerConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import models.ReviewDetails
import uk.gov.hmrc.play.config.{RunMode, FrontendAuthConnector}

import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object BusinessRegController extends BusinessRegController  {

  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val businessCustomerConnector = BusinessCustomerConnector
}

trait BusinessRegController extends FrontendController with Actions with RunMode {
  import play.api.Play.current


  val dataCacheConnector: DataCacheConnector
  val businessCustomerConnector: BusinessCustomerConnector

  def register(service: String) = AuthorisedFor(BusinessCustomerRegime(service)) {
    implicit user => implicit request =>
      Ok(views.html.business_registration(businessRegistrationForm, service))
  }


  def redirectToService(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl")
      serviceRedirectUrl match{
        case Some(serviceUrl) => Future.successful(Redirect(serviceUrl))
        case _ => throw new RuntimeException(s"Service does not exist for : $service. This should be in the conf file against 'govuk-tax.$env.services.${service.toLowerCase}.serviceRedirectUrl'")
      }
  }

  def send(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
      businessRegistrationForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.business_registration(formWithErrors, service)))
        },
        registrationData => {
          businessCustomerConnector.register(registrationData).flatMap {
            registrationSuccessResponse => {
              dataCacheConnector.saveReviewDetails(registrationSuccessResponse.as[ReviewDetails]) flatMap {
                cachedData =>
                  Future.successful(Redirect(controllers.routes.ReviewDetailsController.businessDetails(service)))
              }
            }
          }
        }
      )
  }

}

