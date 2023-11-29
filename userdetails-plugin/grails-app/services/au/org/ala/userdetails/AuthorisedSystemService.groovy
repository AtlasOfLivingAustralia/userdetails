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
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.profile.ProfileManager
import org.pac4j.core.profile.UserProfile
import org.pac4j.core.util.FindBest
import au.org.ala.ws.security.client.AlaAuthClient
import org.pac4j.jee.context.JEEContextFactory
import org.pac4j.jee.context.session.JEESessionStoreFactory
import org.pac4j.oidc.credentials.OidcCredentials
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorisedSystemService {

    @Autowired
    JwtProperties jwtProperties
    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    AlaAuthClient alaAuthClient
    @Autowired
    IAuthorisedSystemRepository authorisedSystemRepository

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
     * @param role The user role required to continue
     * @param scope The JWT scope required for the request to be authorized
     * @return true
     */
    def isAuthorisedRequest(HttpServletRequest request, HttpServletResponse response, String role, String scope) {
        def result = false

        if (jwtProperties.enabled) {
            def context = context(request, response)
            def sessionStore = sessionStore()
            ProfileManager profileManager = new ProfileManager(context, sessionStore)
            profileManager.setConfig(config)

            result = alaAuthClient.getCredentials(context, sessionStore)
                    .map { credentials -> checkCredentials(scope, credentials, role, context, profileManager) }
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
     * @param requiredRole The required role for the user, if any
     * @param context The web context (request, response)
     * @param profileManager The profile manager, the user profile if available, will be saved into this profile manager
     * @return true if the credentials match both the requiredScope and requiredRole
     */
    private boolean checkCredentials(String requiredScope, Credentials credentials, String requiredRole, WebContext context, ProfileManager profileManager) {
        boolean matchesScope
        if (requiredScope) {

            if (credentials instanceof OidcCredentials) {

                OidcCredentials oidcCredentials = credentials

                matchesScope = oidcCredentials.accessToken.scope.contains(requiredScope)

                if (!matchesScope) {
                    log.debug "access_token scopes '${oidcCredentials.accessToken.scope}' is missing required scopes ${requiredScope}"
                }
            } else {
                matchesScope = false
                log.debug("$credentials are not OidcCredentials, so can't get access_token")
            }
        } else {
            matchesScope = true
        }

        boolean matchesRole
        Optional<UserProfile> userProfile = alaAuthClient.getUserProfile(credentials, context, config.sessionStore)
                .map { userProfile -> // save profile into profile manager to match pac4j filter
                    profileManager.save(
                            alaAuthClient.getSaveProfileInSession(context, userProfile),
                            userProfile,
                            alaAuthClient.isMultiProfile(context, userProfile)
                    )
                    userProfile
                }
        if (requiredRole) {
            matchesRole = userProfile
                    .map {profile -> checkProfileRole(profile, requiredRole) }
                    .orElseGet {
                        log.debug "rejecting request because role $requiredRole is required but no user profile is available"
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
    private boolean checkProfileRole(UserProfile userProfile, String requiredRole) {
        def userProfileContainsRole = userProfile.roles.contains(requiredRole)

        if (!userProfileContainsRole) {
            log.debug "user profile roles '${userProfile.roles}' is missing required role ${requiredRole}"
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
        final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
        return context
    }

    private SessionStore sessionStore() {
        final SessionStore sessionStore = FindBest.sessionStoreFactory(null, config, JEESessionStoreFactory.INSTANCE).newSessionStore()
        return sessionStore
    }
}
