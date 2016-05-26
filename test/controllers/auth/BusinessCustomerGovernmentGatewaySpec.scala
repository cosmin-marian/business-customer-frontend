package controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class BusinessCustomerGovernmentGatewaySpec extends PlaySpec with OneServerPerSuite {

  val serviceName: String = "ATED"
  
  "BusinessCustomerGovernmentGateway" must {
    
    "have loginURL value overridden" in {
      BusinessCustomerGovernmentGateway(serviceName).loginURL must be(ExternalUrls.loginURL)
    }

    "have continueURL value overridden" in {
      BusinessCustomerGovernmentGateway(serviceName).continueURL must be(ExternalUrls.continueURL(serviceName))
    }
    
  }
  
}
