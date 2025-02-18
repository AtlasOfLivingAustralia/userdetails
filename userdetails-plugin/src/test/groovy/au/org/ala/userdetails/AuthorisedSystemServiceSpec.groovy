package au.org.ala.userdetails

import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.security.credentials.JwtCredentials
import au.org.ala.ws.security.profile.AlaOidcUserProfile
import com.google.common.io.Resources
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.time.TimeCategory
import io.github.joke.spockmockable.Mockable
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.pac4j.core.config.Config
import au.org.ala.ws.security.client.AlaAuthClient
import org.pac4j.http.client.direct.DirectBearerAuthClient
import org.pac4j.oidc.credentials.OidcCredentials
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

@Mockable(className = "org.pac4j.core.profile.BasicUserProfile")
class AuthorisedSystemServiceSpec extends Specification implements ServiceUnitTest<AuthorisedSystemService>, DataTest {

    def client = Stub(AlaAuthClient)
    def directClient = Mock(DirectBearerAuthClient)

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
        client.getCredentials(_) >> Optional.empty()
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

    def "test token isAuthorisedRequest with profile: #hasProfile, requiredScope: #requiredScope, tokenScopes: #tokenScopes, requiredRoles: #requiredRoles, profileRoles: #profileRoles, result: #result, oidcCredential: #oidcCredential"(boolean hasProfile, String requiredScope, List<String> tokenScopes, String[] requiredRoles, List<String> profileRoles, boolean result, boolean  oidcCredential) {
        given:
        def token = new BearerAccessToken(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                86400,
                new Scope(*tokenScopes)
        )
        def credentials
        if(oidcCredential) {
            credentials = new OidcCredentials().tap { accessToken = token.toJSONObject() }
            credentials.accessToken.scope = tokenScopes
        }
        else {
            def jwt = generateJwt(JWKSet.load(Resources.getResource('test.jwks').newInputStream()), tokenScopes.toSet())
            credentials = new JwtCredentials(jwt, JWTParser.parse(jwt))
        }
        directClient.getCredentials(_) >> Optional.of(credentials)
        directClient.validateCredentials(_, credentials) >> Optional.of(credentials)
        service.clientList = [directClient]
        if (hasProfile) {
            def profile = new AlaOidcUserProfile("text@example.org").tap { roles = profileRoles }
            directClient.getUserProfile(_, _) >> Optional.of(profile)
        } else {
            directClient.getUserProfile(_, _) >> Optional.empty()
        }
        directClient.getSaveProfileInSession(_, _) >> false
        directClient.isMultiProfile(_, _) >> false

        def request = new MockHttpServletRequest("GET", "/userdetails/getUserDetails")
        request.remoteAddr = "1.1.1.1"
        request.remoteHost = 'example.org'
        def response = new MockHttpServletResponse()
        when:
        def authorised = service.isAuthorisedRequest(request, response, requiredRoles, requiredScope)
        then:
        0 * service.authorisedSystemRepository.findByHost(_)
        authorised == result

        where:
        hasProfile  | requiredScope | tokenScopes   | requiredRoles  | profileRoles  || result || oidcCredential
        false       | "scope"       | ["scope"]     | null          | []            || true   || true
        false       | "scope"       | ["no_scope"]  | null          | []            || false  || true
        false       | "scope"       | []            | null          | []            || false  || true
        true        | null          | []            | ["role"]        | ["role"]      || true   || true
        true        | null          | []            | ["role"]       | ["no_role"]   || false  || true
        true        | null          | []            | ["role" ]       | []            || false  || true
        true        | "scope"       | ["scope"]     | ["role" ]       | ["role"]      || true   || true
        true        | "scope"       | ["no_scope"]  | ["role" ]       | ["role"]      || false  || true
        true        | "scope"       | []            | ["role" ]       | ["role"]      || false  || true
        true        | "scope"       | ["scope"]     | ["role" ]       | ["no_role"]   || false  || true
        true        | "scope"       | ["scope"]     | ["role" ]       | []            || false  || true
        true        | "scope"       | ["no_scope"]  | ["role" ]       | ["no_role"]   || false  || true
        true        | "scope"       | []            | ["role" ]       | []            || false  || true
        false       | "scope"       | ["scope"]     | ["role" ]       | []            || false  || true
        false       | "scope"       | ["scope"]     | null          | []            || true   || false
        false       | "scope"       | ["no_scope"]  | null          | []            || false  || false
        false       | "scope"       | []            | null          | []            || false  || false
        true        | null          | []            | ["role" ]       | ["role"]      || true   || false
        true        | null          | []            | ["role" ]       | ["no_role"]   || false  || false
        true        | null          | []            | ["role" ]       | []            || false  || false
        true        | "scope"       | ["scope"]     | ["role" ]       | ["role"]      || true   || false
        true        | "scope"       | ["no_scope"]  | ["role" ]       | ["role"]      || false  || false
        true        | "scope"       | []            | ["role" ]       | ["role"]      || false  || false
        true        | "scope"       | ["scope"]     | ["role" ]       | ["no_role"]   || false  || false
        true        | "scope"       | ["scope"]     | ["role" ]       | []            || false  || false
        true        | "scope"       | ["no_scope"]  | ["role" ]       | ["no_role"]   || false  || false
        true        | "scope"       | []            | ["role" ]       | []            || false  || false
        false       | "scope"       | ["scope"]     | ["role" ]       | []            || false  || false

    }

    static String generateJwt(JWKSet jwkSet, Set<String> scopes) {
        def header = new JWSHeader(JWSAlgorithm.RS256, new JOSEObjectType("jwt"), null, null, null, null, null, null, null, null, "test", true, null, null)
        def claimsSet = generateClaims(scopes).build()
        def signedJWT = new SignedJWT(header, claimsSet)
        signedJWT.sign(new RSASSASigner(jwkSet.getKeyByKeyId('test').toRSAKey()))
        signedJWT.serialize(false)
    }

    static JWTClaimsSet.Builder generateClaims(
            Set<String> scopes,
            String subject = 'sub',
            String issuer = 'http://localhost',
            String audience = 'some-aud',
            Date notBefore = new Date(),
            Date issueTime = new Date(),
            Date expiration = use(TimeCategory) { new Date() + 1.minute }
    ) {
        new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .claim('scope',scopes)
                .notBeforeTime(notBefore)
                .expirationTime(expiration)
                .audience(audience)
                .issueTime(issueTime)
                .claim('client_id', 'some-client-id')
                .claim('cit', 'client_id')
                .claim('jti', 'asdfasdfgafgadfg')
                .claim('scp', scopes)
    }
}
