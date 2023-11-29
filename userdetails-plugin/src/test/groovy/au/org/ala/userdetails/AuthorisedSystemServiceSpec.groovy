package au.org.ala.userdetails

import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.security.profile.AlaOidcUserProfile
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import io.github.joke.spockmockable.Mockable
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.pac4j.core.config.Config
import au.org.ala.ws.security.client.AlaAuthClient
import org.pac4j.core.profile.UserProfile
import org.pac4j.oidc.credentials.OidcCredentials
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

@Mockable(className = "org.pac4j.core.profile.BasicUserProfile")
class AuthorisedSystemServiceSpec extends Specification implements ServiceUnitTest<AuthorisedSystemService>, DataTest {

    def client = Stub(AlaAuthClient)

    void setupSpec() {
    }

    def setup() {
        defineBeans {
            authorisedSystemRepository(InstanceFactoryBean, Mock(IAuthorisedSystemRepository), IAuthorisedSystemRepository)
            pac4jConfig(InstanceFactoryBean, Stub(Config), Config)
            alaAuthClient(InstanceFactoryBean, client, AlaAuthClient)
            jwtProperties(JwtProperties) {
                enabled = true
                fallbackToLegacyBehaviour = true
            }
        }
    }

    def "test isAuthorisedRequest legacy"(String remoteAddr, boolean result) {
        given:
        service.jwtProperties.enabled = false
        service.config = null
        service.alaAuthClient = null
        def request = new MockHttpServletRequest("GET", "/userdetails/getUserDetails")
        request.remoteAddr = remoteAddr
        request.remoteHost = 'example.org'
        def response = new MockHttpServletResponse()
        when:
        def authorised = service.isAuthorisedRequest(request, response, null, null)
        then:
        1 * service.authorisedSystemRepository.findByHost(remoteAddr) >> result
        authorised == result

        where:
        remoteAddr | result
        '123.123.123.124' | false
        '123.123.123.123' | true
    }

    def "test isAuthorisedRequest legacy fallback"(String remoteAddr, boolean result) {
        given:
        def request = new MockHttpServletRequest("GET", "/userdetails/getUserDetails")
        request.remoteAddr = remoteAddr
        request.remoteHost = 'example.org'
        client.getCredentials(_, _) >> Optional.empty()
        def response = new MockHttpServletResponse()
        when:
        def authorised = service.isAuthorisedRequest(request, response, null, null)
        then:
        1 * service.authorisedSystemRepository.findByHost(remoteAddr) >> result
        authorised == result

        where:
        remoteAddr | result
        '123.123.123.124' | false
        '123.123.123.123' | true
    }

    def "test token isAuthorisedRequest with profile: #hasProfile, requiredScope: #requiredScope, tokenScopes: #tokenScopes, requiredRole: #requiredRole, profileRoles: #profileRoles, result: #result"(boolean hasProfile, String requiredScope, List<String> tokenScopes, String requiredRole, List<String> profileRoles, boolean result) {
        given:
        def token = new BearerAccessToken(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                86400,
                new Scope(*tokenScopes)
        )
        def credentials = new OidcCredentials().tap { accessToken = token}
        client.getCredentials(_, _) >> Optional.of(credentials)
        if (hasProfile) {
            def profile = new AlaOidcUserProfile("text@example.org").tap { roles = profileRoles }
            client.getUserProfile(_, _, _) >> Optional.of(profile)
        } else {
            client.getUserProfile(_, _, _) >> Optional.empty()
        }

        def request = new MockHttpServletRequest("GET", "/userdetails/getUserDetails")
        request.remoteAddr = "1.1.1.1"
        request.remoteHost = 'example.org'
        def response = new MockHttpServletResponse()
        when:
        def authorised = service.isAuthorisedRequest(request, response, requiredRole, requiredScope)
        then:
        0 * service.authorisedSystemRepository.findByHost(_)
        authorised == result

        where:
        hasProfile  | requiredScope | tokenScopes   | requiredRole  | profileRoles  || result
        false       | "scope"       | ["scope"]     | null          | []            || true
        false       | "scope"       | ["no_scope"]  | null          | []            || false
        false       | "scope"       | []            | null          | []            || false
        true        | null          | []            | "role"        | ["role"]      || true
        true        | null          | []            | "role"        | ["no_role"]   || false
        true        | null          | []            | "role"        | []            || false
        true        | "scope"       | ["scope"]     | "role"        | ["role"]      || true
        true        | "scope"       | ["no_scope"]  | "role"        | ["role"]      || false
        true        | "scope"       | []            | "role"        | ["role"]      || false
        true        | "scope"       | ["scope"]     | "role"        | ["no_role"]   || false
        true        | "scope"       | ["scope"]     | "role"        | []            || false
        true        | "scope"       | ["no_scope"]  | "role"        | ["no_role"]   || false
        true        | "scope"       | []            | "role"        | []            || false
        false       | "scope"       | ["scope"]     | "role"        | []            || false

    }
}
