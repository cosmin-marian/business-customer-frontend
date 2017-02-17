package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import forms.BusinessRegistrationForms._
import controllers.{BusinessVerificationController, BackLinkController, BaseController}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object PaySAQuestionController extends PaySAQuestionController {
  val authConnector = FrontendAuthConnector
  override val controllerId: String = this.getClass.getName
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait PaySAQuestionController extends BackLinkController {

  def view(service: String) = AuthAction(service).async { implicit bcContext =>
    if (bcContext.user.isAgent)
      ForwardWithBack(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"))
    else
      addBackLinkToPage(
          Ok(views.html.nonUkReg.paySAQuestion(paySAQuestionForm, service))
      )
  }

  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    paySAQuestionForm.bindFromRequest.fold(
      formWithErrors =>
        addBackLinkToPage(
            BadRequest(views.html.nonUkReg.paySAQuestion(formWithErrors, service))
        ),
      formData => {
        val paysSa = formData.paySA.getOrElse(false)
        if (paysSa)
          RedirectWithBack(BusinessVerificationController.controllerId,
            controllers.routes.BusinessVerificationController.businessForm(service, "SOP"),
            controllers.nonUKReg.routes.PaySAQuestionController.view(service)
          )
        else
          RedirectWithBack(BusinessRegController.controllerId,
            controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"),
            controllers.nonUKReg.routes.PaySAQuestionController.view(service)
          )
      }
    )
  }

}
