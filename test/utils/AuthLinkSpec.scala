package utils

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.InternalServerException

class AuthLinkSpec extends PlaySpec {

  "AuthLink" must {
    "Return the orgs link if we have org authorisation" in {
      implicit val aut = builders.AuthBuilder.createUserAuthContext("userId", "Joe Bloggs")
      val link = AuthLink.getAuthLink()
      link must startWith("org")
    }

    "Return the agent link if we have agent authorisation" in {
      implicit val aut = builders.AuthBuilder.createAgentAuthContext("userId", "Joe Bloggs")
      val link = AuthLink.getAuthLink()
      link must startWith("agent")
    }

    "throws an exception if the user does not have the correct authorisation" in {
      implicit val aut = builders.AuthBuilder.createInvalidAuthContext("userId", "Joe Bloggs")

      val thrown = the[RuntimeException] thrownBy AuthLink.getAuthLink
      thrown.getMessage must include("User does not have the correct authorisation")
    }
  }

}
