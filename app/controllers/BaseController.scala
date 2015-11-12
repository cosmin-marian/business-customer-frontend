package controllers

import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.auth._

trait BaseController extends FrontendController with Actions {

  def AuthorisedForGG(taxRegime: TaxRegime) = {
    AuthorisedFor(taxRegime = taxRegime, pageVisibility = GGConfidence)
  }

}
