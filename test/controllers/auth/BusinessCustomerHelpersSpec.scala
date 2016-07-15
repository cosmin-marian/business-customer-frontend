package controllers.auth

import builders.AuthBuilder
import org.scalatestplus.play.PlaySpec

class BusinessCustomerHelpersSpec extends PlaySpec {

  "AuthLink" must {
    "Return the orgs link if we have org authorisation" in {
      implicit val aut = AuthBuilder.createUserAuthContext("userId", "Joe Bloggs")
      aut.user.authLink must startWith("org")
    }

    "Return the sa link if we have an sa individual" in {
      implicit val aut = AuthBuilder.createSaAuthContext("agentId", "Agent Bloggs")
      aut.user.authLink must startWith("sa")
      aut.user.authLink mustNot startWith("sa/individual")
    }


    "Return the agent link if we have agent admin authorisation" in {
      implicit val aut = AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      aut.user.authLink must startWith("agent")
    }

    "Return the agent link if we have agent assistant authorisation" in {
      implicit val aut = AuthBuilder.createAgentAssistantAuthContext("agentId", "Agent Bloggs")
      aut.user.authLink must startWith("agent")
    }

    "throws an exception if the user does not have the correct authorisation" in {
      implicit val aut = AuthBuilder.createInvalidAuthContext("userId", "Joe Bloggs")

      val thrown = the[RuntimeException] thrownBy aut.user.authLink
      thrown.getMessage must include("User does not have the correct authorisation")
    }
  }

  "isAgent" must {
    "Return true if this user is an agent" in {
      implicit val auth = AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      auth.user.isAgent must be(true)
    }

    "Return false if this user is not an agent" in {
      implicit val auth = AuthBuilder.createUserAuthContext("userId", "Joe Bloggs")
      auth.user.isAgent must be(false)
    }
  }

}
