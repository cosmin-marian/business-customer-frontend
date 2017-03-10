package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import forms.BusinessRegistrationForms._
import controllers.{BusinessVerificationController, BackLinkController, BaseController}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object PaySAQuestionController extends PaySAQuestionController {
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "PaySAQuestionController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait PaySAQuestionController extends BackLinkController {

  def view(service: String) = AuthAction(service).async { implicit bcContext =>
    if (bcContext.user.isAgent)
      ForwardBackLinkToNextPage(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"))
    else
      currentBackLink.map(backLink =>
        Ok(views.html.nonUkReg.paySAQuestion(paySAQuestionForm, service, backLink))
      )
  }

  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    paySAQuestionForm.bindFromRequest.fold(
      formWithErrors =>
        currentBackLink.map(backLink =>
          BadRequest(views.html.nonUkReg.paySAQuestion(formWithErrors, service, backLink))
        ),
      formData => {
        val paysSa = formData.paySA.getOrElse(false)
        if (paysSa)
          RedirectWithBackLink(BusinessVerificationController.controllerId,
            controllers.routes.BusinessVerificationController.businessForm(service, "NRL"),
            Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)
          )
        else
          RedirectWithBackLink(BusinessRegController.controllerId,
            controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"),
            Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)
          )
      }
    )
  }

}
