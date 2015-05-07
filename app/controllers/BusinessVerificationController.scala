package controllers

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import forms.BusinessVerificationForms._
import models.ReviewDetails
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  val businessMatchingConnector: BusinessMatchingConnector = BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

trait BusinessVerificationController extends FrontendController {

  val businessMatchingConnector: BusinessMatchingConnector
  val dataCacheConnector: DataCacheConnector

   def businessVerification(service: String) = UnauthorisedAction { implicit request =>
     Ok(views.html.business_verification(businessTypeForm, service)).withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
   }

  def continue(service: String) = UnauthorisedAction { implicit request =>
    businessTypeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.business_verification(formWithErrors, service)),
      value => {
        value.businessType match {
          case "NUK" => Redirect(controllers.routes.BusinessRegController.register())
          case "SOP" => Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "SOP"))
          case "UIB" => Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "UIB"))
          case "LTD" => Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "LTD"))
          case "OBP" => Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "OBP"))
          case "LLP" => Redirect(controllers.routes.BusinessVerificationController.businessLookup(service, "LLP"))
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

  def businessLookup(service: String, businessType: String) = UnauthorisedAction { implicit request =>
    businessType match {
      case "SOP"  => Ok(views.html.business_lookup_SOP(soleTraderForm, service))
      case "LTD"  => Ok(views.html.business_lookup_LTD(limitedCompanyForm, service))
      case "UIB"  => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, service))
      case "OBP"  => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, service))
      case "LLP"  => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, service))
    }
  }

  def submit(businessType: String) = UnauthorisedAction { implicit request =>
    val service = "ATED"
    businessType match {
      case "UIB" => unincorporatedBodyForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.business_lookup_UIB(formWithErrors, service)),
        value => ???
      )
      case "SOP" => soleTraderForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.business_lookup_SOP(formWithErrors, service)),
        value => ???
      )
      case "LLP" => limitedLiabilityPartnershipForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.business_lookup_LLP(formWithErrors, service)),
        value => ???
      )
      case "OBP" => ordinaryBusinessPartnershipForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.business_lookup_OBP(formWithErrors, service)),
        value => ???
      )
      case "LTD" => limitedCompanyForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.business_lookup_LTD(formWithErrors, service)),
        value => ???
      )
    }

  }

  def helloWorld(response: String) = Action {
    Ok(views.html.hello_world(response))
  }
}
