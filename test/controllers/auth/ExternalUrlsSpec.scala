package controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class ExternalUrlsSpec extends PlaySpec with OneServerPerSuite {

  "ExternalUrls" must {

    "have companyAuthHost " in {
      ExternalUrls.companyAuthHost must be("http://localhost:9025")
    }

    "have loginCallback " in {
      ExternalUrls.loginCallback must be("http://localhost:9923/business-customer")
    }

    "have loginPath " in {
      ExternalUrls.loginPath must be("sign-in")
    }

    "have signIn " in {
      ExternalUrls.signIn must be(s"""http://localhost:9025/account/sign-in?continue=http://localhost:9923/business-customer""")
    }

  }
  
}
