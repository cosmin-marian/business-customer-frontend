package controllers

import java.net.URLEncoder

import config.{ApplicationConfig, BusinessCustomerHeaderCarrierForPartialsConverter, FrontendAuthConnector, WSHttp}
import controllers.auth.BusinessCustomerGovernmentGateway
import play.api.Logger
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.{HttpPost, HttpReads, HttpResponse}

import scala.concurrent.Future

trait ContactHmrcController extends FrontendController with Actions {

  def httpPost: HttpPost
  def contactFrontendPartialBaseUrl: String
  def contactFormServiceIdentifier: String

  private val TICKET_ID = "ticketId"
  private lazy val submitUrl = routes.ContactHmrcController.submitContactHmrc().url
  private lazy val contactHmrcFormPartialUrl =
    s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form?service=${contactFormServiceIdentifier}&submitUrl=${urlEncode(submitUrl)}"
  private lazy val contactHmrcSubmitPartialUrl = s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form?resubmitUrl=${urlEncode(submitUrl)}"
  private def contactHmrcThankYouPartialUrl(ticketId: String) =
    s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form/confirmation?ticketId=${urlEncode(ticketId)}"

  def contactHmrc = AuthenticatedBy(BusinessCustomerGovernmentGateway("")).async {
    implicit user => implicit request =>
      Future.successful(Ok(views.html.contact_hmrc(contactHmrcFormPartialUrl, None)))
  }

  def submitContactHmrc = AuthenticatedBy(BusinessCustomerGovernmentGateway("")).async {
    implicit user => implicit request =>
      request.body.asFormUrlEncoded.map { formData =>
        httpPost.POSTForm[HttpResponse](contactHmrcSubmitPartialUrl, formData)(rds = PartialsFormReads.readPartialsForm, hc = partialsReadyHeaderCarrier).map {
          resp => resp.status match {
            case OK => Redirect(routes.ContactHmrcController.contactHmrcThankYou).withSession(request.session + (TICKET_ID -> resp.body))
            case BAD_REQUEST => BadRequest(views.html.contact_hmrc(contactHmrcFormPartialUrl, Some(Html(resp.body)))(request))
            case INTERNAL_SERVER_ERROR => InternalServerError(Html(resp.body))
            case status => throw new Exception(s"Unexpected status code from contact HMRC form: $status")
          }
        }
      }.getOrElse {
        Logger.warn("Trying to submit an empty contact form")
        Future.successful(InternalServerError)
      }
  }

  def contactHmrcThankYou = AuthenticatedBy(BusinessCustomerGovernmentGateway("")).async {
    implicit user => implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      Future.successful(Ok(views.html.contact_hmrc_thankyou(contactHmrcThankYouPartialUrl(ticketId))))
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]) : HeaderCarrier = {
    val hc1 = BusinessCustomerHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    BusinessCustomerHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

}

object ContactHmrcController extends ContactHmrcController {
  implicit override val authConnector = FrontendAuthConnector
  override val httpPost = WSHttp
  override val contactFrontendPartialBaseUrl = ApplicationConfig.contactFrontendPartialBaseUrl
  override val contactFormServiceIdentifier = ApplicationConfig.contactFormServiceIdentifier
}

object PartialsFormReads {
  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = response
  }
}
