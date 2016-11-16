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

package acceptance

import models.{Identification, Address, ReviewDetails}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class ReviewDetailsNonUkAgentFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val address = Address("23 High Street", "Park View", Some("Gloucester"), Some("Gloucestershire, NE98 1ZZ"), Some("NE98 1ZZ"), "GB")
  val reviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = true,
    agentReferenceNumber = Some("agent123"), identification = Some(Identification("id","inst", "FR")))

  feature("The user can view the review details page for a non uk agent") {

    info("as a user i want to be able to view my review details page")

    scenario("return Review Details view for an agent, when agent can't be directly found with login credentials and the reg is editable") {

      Given("An agent has an editable business registration details")
      When("The user views the page")
      implicit val request = FakeRequest()

      val html = views.html.review_details_non_uk_agent("ATED", reviewDetails.copy(directMatch = false))

      val document = Jsoup.parse(html.toString())

      And("The submit button is - Confirm")
      assert(document.getElementById("submit").text() === "Confirm")

      Then("The title should match - Confirm your business details")
      assert(document.select("h1").text === ("Check your agency details"))

      assert(document.getElementById("bc.business-registration-agent.text").text() === ("ATED agency set up"))

      And("Business name is correct")
      assert(document.getElementById("business-name-title").text === ("Business name"))
      assert(document.getElementById("business-name").text === ("ACME"))
      assert(document.getElementById("bus-name-edit").attr("href") === ("/business-customer/agent/register/non-uk-client/ATED/edit"))

      And("Business address is correct")
      assert(document.getElementById("business-address-title").text === ("Registered address"))
      assert(document.getElementById("business-address").text === ("23 High Street Park View Gloucester Gloucestershire, NE98 1ZZ NE98 1ZZ United Kingdom"))
      assert(document.getElementById("bus-reg-edit").attr("href") === ("/business-customer/agent/register/non-uk-client/ATED/edit"))

      And("Overseas tax referebce is correct")
      assert(document.getElementById("overseas-tax-reference-title").text === ("Overseas company registration number"))
      assert(document.getElementById("overseas-details").text === ("id France inst"))
      assert(document.getElementById("overseas-edit").attr("href") === ("/business-customer/agent/register/non-uk-client/ATED/edit"))

      assert(document.select(".button").text === ("Confirm"))

    }

  }
}
