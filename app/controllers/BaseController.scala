package controllers

import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel

trait BaseController extends FrontendController with Actions {

  val GGConfidence = new NonNegotiableIdentityConfidencePredicate(ConfidenceLevel.L50)

  def AuthorisedForGG(taxRegime: TaxRegime) = {
    AuthorisedFor(taxRegime = taxRegime, pageVisibility = GGConfidence)
  }

}
