package utils

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.InternalServerException

class AuthUtilsSpec extends PlaySpec {

  "AuthLink" must {
    "Return the orgs link if we have org authorisation" in {
      implicit val aut = builders.AuthBuilder.createUserAuthContext("userId", "Joe Bloggs")
      val link = AuthUtils.getAuthLink()
      link must startWith("org")
    }

    "Return the agent link if we have agent admin authorisation" in {
      implicit val aut = builders.AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      val link = AuthUtils.getAuthLink()
      link must startWith("agent")
      link must endWith("/admin")
    }

    "Return the agent link if we have agent assistant authorisation" in {
      implicit val aut = builders.AuthBuilder.createAgentAssistantAuthContext("agentId", "Agent Bloggs")
      val link = AuthUtils.getAuthLink()
      link must startWith("agent")
      link must endWith("/assistant")
    }

    "throws an exception if the user does not have the correct authorisation" in {
      implicit val aut = builders.AuthBuilder.createInvalidAuthContext("userId", "Joe Bloggs")

      val thrown = the[RuntimeException] thrownBy AuthUtils.getAuthLink
      thrown.getMessage must include("User does not have the correct authorisation")
    }
  }

  "isAgent" must {
    "Return true if this user is an agent" in {
      implicit val auth = builders.AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAgent must be(true)
    }

    "Return false if this user is not an agent" in {
      implicit val auth = builders.AuthBuilder.createUserAuthContext("userId", "Joe Bloggs")
      AuthUtils.isAgent must be(false)
    }
  }
}
