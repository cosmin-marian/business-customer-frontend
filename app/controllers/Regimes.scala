package controllers

import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Account, Accounts}
import uk.gov.hmrc.play.frontend.auth.TaxRegime

trait GovernmentGatewayTaxRegime extends TaxRegime {
  def account(accounts: Accounts): Option[Account]

  def isAuthorised(accounts: Accounts) = account(accounts).isDefined
  val authenticationType = ???
}

object SaRegime extends GovernmentGatewayTaxRegime {
  def account(accounts: Accounts) = accounts.sa
}