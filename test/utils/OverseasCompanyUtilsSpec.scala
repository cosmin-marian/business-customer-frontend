package utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class OverseasCompanyUtilsSpec extends PlaySpec  with OneServerPerSuite {

  "OverseasCompanyUtils" must {
    "return the correct data for a client" in {
      val details = OverseasCompanyUtils.displayDetails(false, false, "awrs")
      details.addClient must be (false)
      details.title must be ("Do you have an overseas company registration number?")
      details.header must be ("Do you have an overseas company registration number?")
      details.subHeader must be ("AWRS registration")
    }

    "return the correct data for an agent" in {
      val details = OverseasCompanyUtils.displayDetails(true, false, "ated")
      details.addClient must be (false)
      details.title must be ("Do you have an overseas company registration number?")
      details.header must be ("Do you have an overseas company registration number?")
      details.subHeader must be ("ATED agency set up")
    }

    "return the correct data for an agent adding a client" in {
      val details = OverseasCompanyUtils.displayDetails(true, true, "ated")
      details.addClient must be (true)
      details.title must be ("Does your client have an overseas company registration number?")
      details.header must be ("Does your client have an overseas company registration number?")
      details.subHeader must be ("Add a client")
    }
  }
}
