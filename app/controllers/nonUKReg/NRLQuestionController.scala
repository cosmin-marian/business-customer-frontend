package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import controllers.{BackLinkController, BaseController}
import forms.BusinessRegistrationForms._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object NRLQuestionController extends NRLQuestionController {
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "NRLQuestionController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait NRLQuestionController extends BackLinkController {

  def view(service: String) = AuthAction(service).async { implicit bcContext =>
    if (bcContext.user.isAgent)
      ForwardBackLinkToNextPage(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"))
    else
      currentBackLink.map(backLink => Ok(views.html.nonUkReg.nrl_question(nrlQuestionForm, service, backLink)))
  }

  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    nrlQuestionForm.bindFromRequest.fold(
      formWithErrors =>
        currentBackLink.map(backLink => BadRequest(views.html.nonUkReg.nrl_question(formWithErrors, service, backLink))),
      formData => {
        val paysSa = formData.paysSA.getOrElse(false)
        if (paysSa)
          RedirectWithBackLink(PaySAQuestionController.controllerId,
            controllers.nonUKReg.routes.PaySAQuestionController.view(service),
            controllers.nonUKReg.routes.NRLQuestionController.view(service)
          )
        else
          RedirectWithBackLink(BusinessRegController.controllerId,
            controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"),
            controllers.nonUKReg.routes.NRLQuestionController.view(service)
          )

      }
    )
  }

}
