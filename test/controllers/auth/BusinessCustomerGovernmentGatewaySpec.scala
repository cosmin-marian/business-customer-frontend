package controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class BusinessCustomerGovernmentGatewaySpec extends PlaySpec with OneServerPerSuite {
  
  "BusinessCustomerGovernmentGateway" must {
    
    "have login value overridden" in {
      BusinessCustomerGovernmentGateway.login must be(ExternalUrls.signIn)
    }
    
  }
  
}
