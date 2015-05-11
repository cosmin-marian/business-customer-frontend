package controllers

import play.api.Play
import controllers.auth.BusinessCustomerRegime
import forms.BusinessRegistrationForms._
import uk.gov.hmrc.play.config.{RunMode, FrontendAuthConnector}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object BusinessRegController extends BusinessRegController  {
  override val authConnector = FrontendAuthConnector
}

trait BusinessRegController extends FrontendController with Actions with RunMode {
  import play.api.Play.current


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
}

