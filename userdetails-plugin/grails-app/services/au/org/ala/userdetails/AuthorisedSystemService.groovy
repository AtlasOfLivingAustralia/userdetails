/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.userdetails

import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.security.credentials.JwtCredentials
import org.pac4j.core.adapter.FrameworkAdapter
import org.pac4j.core.client.DirectClient
import org.pac4j.core.config.Config
import org.pac4j.core.context.CallContext
import org.pac4j.core.context.FrameworkParameters
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.profile.ProfileManager
import org.pac4j.core.profile.UserProfile
import au.org.ala.ws.security.client.AlaAuthClient
import org.pac4j.core.profile.factory.ProfileManagerFactory
import org.pac4j.jee.context.JEEFrameworkParameters
import org.pac4j.oidc.credentials.OidcCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.regex.Pattern
import java.util.stream.Collectors

class AuthorisedSystemService {

    @Autowired
    JwtProperties jwtProperties
    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    AlaAuthClient alaAuthClient
    @Autowired
    IAuthorisedSystemRepository authorisedSystemRepository
    @Autowired(required = false)
    @Qualifier('alaClient')
    List<DirectClient> clientList

    def isAuthorisedSystem(HttpServletRequest request){
        def host = request.getRemoteAddr()
        log.debug("RemoteHost: " + request.getRemoteHost())
        log.debug("RemoteAddr: " + request.getRemoteAddr())
        log.debug("host using: " + host)

        return authorisedSystemRepository.findByHost(host)
//        return host != null && authorisedSystemRepository.findByHost(host)
    }

    /**
     * Validate a JWT Bearer token instead of the API key.
     * @param fallbackToLegacy Whether to fall back to legacy authorised systems if the JWT is not present.
     * @param roles The user roles required to continue. The user must have at least one role to be authorized.
     * @param scope The JWT scope required for the request to be authorized
     * @return true
     */
    def isAuthorisedRequest(HttpServletRequest request, HttpServletResponse response, String[] roles, String scope) {
        def result = false

        if (jwtProperties.enabled) {
            def context = context(request, response)
            def sessionStore = sessionStore(request, response)
            ProfileManager profileManager = new ProfileManager(context, sessionStore)
            profileManager.setConfig(config)

            ProfileManagerFactory profileManagerFactory = config.getProfileManagerFactory();
            CallContext callContext = new CallContext(context, sessionStore, profileManagerFactory);

            Optional<Credentials> cred = Optional.empty()
            def directClient = null

            for (DirectClient client : clientList) {
                Credentials credentials = client.getCredentials(callContext).orElse(null)
                credentials = (Credentials)client.validateCredentials(callContext, credentials).orElse(null)
                if (credentials != null && credentials.isForAuthentication()) {
                    cred =  Optional.of(credentials)
                    directClient = client
                    break
                }
            }

            result = cred.map { credentials -> checkCredentials(scope, credentials, directClient, roles, callContext, context, profileManager) }
                    .orElseGet { jwtProperties.fallbackToLegacyBehaviour && isAuthorisedSystem(request) }
        } else {
            result = isAuthorisedSystem(request)
        }
        return result
    }

    /**
     * Validate the given credentials against any required scope or role
     *
     * @param requiredScope The required scope for the access token, if any
     * @param credentials The credentials, should be an OidcCredentials instance
     * @param directClient The directClient
     * @param roles The required roles for the user, if any
     * @param callContext The call context
     * @param context The web context (request, response)
     * @param profileManager The profile manager, the user profile if available, will be saved into this profile manager
     * @return true if the credentials match both the requiredScope and requiredRole
     */
    private boolean checkCredentials(String requiredScope, Credentials credentials, DirectClient directClient, String[] roles, CallContext callContext, WebContext context, ProfileManager profileManager) {
        boolean matchesScope
        if (requiredScope) {

            if (credentials instanceof OidcCredentials) {

                OidcCredentials oidcCredentials = credentials

                matchesScope = oidcCredentials.accessToken.scope.contains(requiredScope)

                if (!matchesScope) {
                    log.debug "access_token scopes '${oidcCredentials.accessToken.scope}' is missing required scopes ${requiredScope}"
                }
            } else if (credentials instanceof JwtCredentials) {

                def jwtToken = ((JwtCredentials) credentials).getJwtAccessToken()
                var scopeClaim = jwtToken.getJWTClaimsSet().getClaim("scope")
                def tokenScopes = []
                if (scopeClaim instanceof List) {
                    tokenScopes = (List<String>) scopeClaim;
                } else if (scopeClaim instanceof String) {
                    tokenScopes = Arrays.stream(((String) scopeClaim).split(Pattern.quote(" "))).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                } else {
                    log.debug("Couldn't parse scope claim value: ${scopeClaim}")
                }
                matchesScope = tokenScopes.contains(requiredScope)

                if (!matchesScope) {
                    log.debug "access_token scopes '${tokenScopes}' is missing required scopes ${requiredScope}"
                }

            } else {
                matchesScope = false
                log.debug("$credentials are not OidcCredentials, so can't get access_token")
            }
        } else {
            matchesScope = true
        }

        boolean matchesRole
        Optional<UserProfile> userProfile = directClient.getUserProfile(callContext, credentials)
                .map { userProfile -> // save profile into profile manager to match pac4j filter
                    profileManager.save(
                            directClient.getSaveProfileInSession(context, userProfile),
                            userProfile,
                            directClient.isMultiProfile(context, userProfile)
                    )
                    userProfile
                }
        if (roles) {
            matchesRole = userProfile
                    .map {profile -> checkProfileRole(profile, roles) }
                    .orElseGet {
                        log.debug "rejecting request because roles ${roles} are required but no user profile is available"
                        false
                    }
        } else {
            matchesRole = true
        }

        return matchesScope && matchesRole
    }

    /**
     * Checks that the given profile has the required role
     * @param userProfile
     * @param requiredRole
     * @return true if the profile has the role, false otherwise
     */
    private boolean checkProfileRole(UserProfile userProfile, String[] requiredRoles) {
        def userProfileContainsRole = requiredRoles.any { role -> userProfile.roles.contains(role) }

        if (!userProfileContainsRole) {
            log.debug "user profile roles '${userProfile.roles}' is missing required roles ${requiredRoles}"
        }
        return userProfileContainsRole
    }

    private boolean profileHasScope(UserProfile userProfile, String scope) {
        def scopes = userProfile.attributes['scope']
        def result = false
        if (scopes != null) {
            if (scopes instanceof String) {
                result = scopes.tokenize(',').contains(scope)
            } else if (scopes.class.isArray()) {
                result =scopes.any { it?.toString() == scope }
            } else if (scopes instanceof Collection) {
                result =scopes.any { it?.toString() == scope }
            }
        }
        return result
    }

    private WebContext context(request, response) {
        FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config)
        final FrameworkParameters frameworkParameters = new JEEFrameworkParameters(request, response)
        final WebContext context = config.getWebContextFactory().newContext(frameworkParameters)
        return context
    }

    private SessionStore sessionStore(request, response ) {
        FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config)
        final FrameworkParameters frameworkParameters = new JEEFrameworkParameters(request, response)
        final SessionStore sessionStore =  config.sessionStoreFactory.newSessionStore(frameworkParameters)
        return sessionStore
    }
}
