package utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class OverseasCompanyUtilsSpec extends PlaySpec  with OneServerPerSuite {

  "OverseasCompanyUtils" must {
    "return the correct data for a client" in {
      val details = OverseasCompanyUtils.displayDetails(false, false)
      details.addClient must be (false)
      details.title must be ("Do you have an overseas company registration number?")
      details.header must be ("Do you have an overseas company registration number?")
      details.subHeader must be ("ATED registration")
    }

    "return the correct data for an agent" in {
      val details = OverseasCompanyUtils.displayDetails(true, false)
      details.addClient must be (false)
      details.title must be ("Do you have an overseas company registration number?")
      details.header must be ("Do you have an overseas company registration number?")
      details.subHeader must be ("ATED agency set up")
    }

    "return the correct data for an agent adding a client" in {
      val details = OverseasCompanyUtils.displayDetails(true, true)
      details.addClient must be (true)
      details.title must be ("Does your client have an overseas company registration number?")
      details.header must be ("Does your client have an overseas company registration number?")
      details.subHeader must be ("Add a client")
    }
  }
}
