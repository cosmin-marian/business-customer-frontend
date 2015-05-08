package controllers

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.auth.BusinessCustomerRegime
import forms.{UnincorporatedMatch, BusinessDetails}
import forms.BusinessVerificationForms._
import models.ReviewDetails
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import scala.concurrent.ExecutionContext.Implicits.global

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
      Ok(views.html.business_verification(businessTypeForm, service))
  }

  def continue(service: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
    businessTypeForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_verification(formWithErrors, service))),
      value => {
        value.businessType match {
          case "NUK" => Future.successful(Redirect(controllers.routes.BusinessRegController.register(service)))
          case "SOP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "SOP")))
          case "UIB" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "UIB")))
          case "LTD" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "LTD")))
          case "OBP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "OBP")))
          case "LLP" => Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "LLP")))
        }
        //
        //          businessMatchingConnector.lookup(value) flatMap {
        //            actualResponse => {
        //              if (actualResponse.toString() contains ("error")) {
        //                Future.successful(Redirect(controllers.routes.BusinessVerificationController.helloWorld(actualResponse.toString())))
        //              } else {
        //                dataCacheConnector.saveReviewDetails(actualResponse.as[ReviewDetails]) flatMap {
        //                  cachedData =>
        //                  Future.successful(Redirect(controllers.routes.ReviewDetailsController.businessDetails(service)))
        //                }
        //              }
        //            }
        //          }
      }
    )
  }

  def businessLookup(service: String, businessType: String) = Action { implicit request =>
    businessType match {
      case "SOP"  => Ok(views.html.business_lookup_SOP(soleTraderForm, service))
      case "LTD"  => Ok(views.html.business_lookup_LTD(limitedCompanyForm, service))
      case "UIB"  => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, service))
      case "OBP"  => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, service))
      case "LLP"  => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, service))
    }
  }

  def submit(service: String, businessType: String) = AuthorisedFor(BusinessCustomerRegime(service)).async {
    implicit user => implicit request =>
    businessType match {
      case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service)
      case "SOP" => soleTraderForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, service))),
        value => Future.successful(Ok)
      )
      case "LLP" => limitedLiabilityPartnershipForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LLP(formWithErrors, service))),
        value => Future.successful(Ok)
      )
      case "OBP" => ordinaryBusinessPartnershipForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, service))),
        value => Future.successful(Ok)
      )
      case "LTD" => limitedCompanyForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, service))),
        value => Future.successful(Ok)
      )
    }

  }

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String, service: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, service))),
      unincorporatedMatch => {
        val businessDetails = BusinessDetails(businessType, None, None, Some(unincorporatedMatch), None, None)
        businessMatchingConnector.lookup(businessDetails) flatMap {
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
    )
  }


}
