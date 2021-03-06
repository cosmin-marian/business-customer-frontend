/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.auth

import config.ApplicationConfig
import models.{BusinessCustomerContext, BusinessCustomerUser}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.frontend.auth.Actions
import utils.ValidateUri

import scala.concurrent.Future

trait BusinessCustomerHelpers extends Actions {

  def AuthAction(service: String) = new AuthAction(service)

  private def isValidUrl(s: String): Boolean = {
    ValidateUri.isValid(ApplicationConfig.serviceList, s)
  }

  class AuthAction(service: String) {

    def apply(f: BusinessCustomerContext => Result): Action[AnyContent] = {
        AuthorisedFor(taxRegime = BusinessCustomerRegime(service), pageVisibility = GGConfidence) {
          implicit authContext => implicit request =>
            f(BusinessCustomerContext(request, BusinessCustomerUser(authContext)))
        }
    }

    def async(f: BusinessCustomerContext => Future[Result]): Action[AnyContent] = {
      if (!isValidUrl(service)) {
        Action.apply(NotFound)
      }
      else {
        AuthorisedFor(taxRegime = BusinessCustomerRegime(service), pageVisibility = GGConfidence).async {
          implicit authContext => implicit request =>
            f(BusinessCustomerContext(request, BusinessCustomerUser(authContext)))
        }
      }
    }

  }

}