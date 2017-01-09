/*
 * Copyright 2016 HM Revenue & Customs
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

package acceptance.nonUkReg

import forms.BusinessRegistrationForms._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class overseas_company_registrationSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val service = "ATED"

  feature("The user can view the overseas company registration question") {

    info("as a client i want to be able to view the overseas company registration question page")

    scenario("return overseas company registration view for a client") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service, false, false, List(("UK", "UK")), None)

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Do you have an overseas company registration number?")
      assert(document.select("h1").text === ("Do you have an overseas company registration number?"))

      Then("The subheader should be - ATED registration")
      assert(document.getElementById("overseas-subheader").text() === "ATED registration")

      Then("The options should be Yes and No")
      assert(document.select(".block-label").text() === "Yes No")

    }

    scenario("return overseas company registration view for an agent") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service, true, false, List(("UK", "UK")), None)

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Do you have an overseas company registration number?")
      assert(document.select("h1").text === ("Do you have an overseas company registration number?"))

      Then("The subheader should be - ATED agency set up")
      assert(document.getElementById("overseas-subheader").text() === "ATED agency set up")

      Then("The options should be Yes and No")
      assert(document.select(".block-label").text() === "Yes No")

    }

    scenario("return overseas company registration view for an agent adding a client") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service, true, true, List(("UK", "UK")), None)

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Do you have an overseas company registration number?")
      assert(document.select("h1").text === ("Do you have an overseas company registration number?"))

      Then("The subheader should be - Add a client")
      assert(document.getElementById("overseas-subheader").text() === "Add a client")

      Then("The options should be Yes and No")
      assert(document.select(".block-label").text() === "Yes No")

    }
  }
}
