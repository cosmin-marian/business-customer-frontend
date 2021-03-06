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

package acceptance

import models.{Identification, Address, ReviewDetails}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen, Matchers}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class ReviewDetailsNonUkAgentFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with Matchers{

  val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "GB")
  val reviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = true,
    agentReferenceNumber = Some("agent123"), identification = Some(Identification("id","inst", "FR")))

  feature("The user can view the review details page for a non uk agent") {

    info("as a user i want to be able to view my review details page")

    scenario("return Review Details view for an agent, when agent can't be directly found with login credentials and the reg is editable") {

      Given("An agent has an editable business registration details")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.review_details_non_uk_agent("ATED", reviewDetails.copy(directMatch = false), Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())
      val bizRegistrationDetails = document.select("#business-details tbody tr")

      Then("The title should match - Confirm your business details")
      assert(document.select("h1").text === ("Check your agency details"))

      assert(document.getElementById("bc.business-registration-agent.text").text() === ("This section is: ATED agency set up"))

      And("The confirmation notice should display")
      assert(document.getElementById("check-agency-details").text ===("You are setting up your agency. These should be your company details not your clients."))

      And("Business name is correct")

      assert(document.getElementById("business-name-title").text === ("Business name"))
      assert(document.getElementById("business-name").text === ("ACME"))
      assert(document.getElementById("business-name-edit").attr("href") === ("/business-customer/agent/register/non-uk-client/ATED/edit"))

      And("Business address is correct")
      assert(document.getElementById("business-address-title").text === ("Registered address"))
      assert(document.getElementById("business-address").text === ("line 1 line 2 line 3 line 4 AA1 1AA United Kingdom"))
      assert(document.getElementById("business-reg-edit").attr("href") === ("/business-customer/agent/register/non-uk-client/ATED/edit"))

      And("Overseas tax reference is correct")
      bizRegistrationDetails.get(2).text should include ("Overseas company registration number")
      bizRegistrationDetails.get(2).text should include("id")
      bizRegistrationDetails.get(3).text should include("Country that issued the number")
      bizRegistrationDetails.get(3).text should include("France")
      bizRegistrationDetails.get(4).text should include("Institution that issued the number")
      bizRegistrationDetails.get(4).text should include("inst")

      And("The submit button is - Confirm")
      assert(document.getElementById("submit").text() === "Confirm")

    }

  }
}
