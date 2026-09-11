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

package au.org.ala.userdetails.cognito

import au.org.ala.userdetails.*
import au.org.ala.web.AuthService
import au.org.ala.web.OidcClientProperties
import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.tokens.TokenService
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.core.env.EnumerablePropertySource

@Slf4j
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp app = new GrailsApp(Application)
        app.setDefaultProperties(loadUserdetailsPluginDefaults())
        app.run(args)
    }

    private static Map<String, Object> loadUserdetailsPluginDefaults() {
        ClassPathResource resource = new ClassPathResource('plugin.yml')
        if (!resource.exists()) {
            throw new IllegalStateException('Required classpath resource plugin.yml was not found')
        }

        Map<String, Object> defaults = [:]
        new YamlPropertySourceLoader()
                .load('userdetails-plugin', resource)
                .findAll { it instanceof EnumerablePropertySource }
                .each { EnumerablePropertySource propertySource ->
                    propertySource.propertyNames.each { String propertyName ->
                        Object value = propertySource.getProperty(propertyName)
                        if (value != null) {
                            defaults[propertyName] = value
                        }
                    }
                }
        defaults
    }

    @Bean
    AwsCredentialsProvider awsCredentialsProvider() {

        String accessKey = grailsApplication.config.getProperty('cognito.accessKey')
        String secretKey = grailsApplication.config.getProperty('cognito.secretKey')
        String sessionToken = grailsApplication.config.getProperty('cognito.sessionToken')

        if (accessKey && secretKey) {
            if (sessionToken) {
                return StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
                )
            }
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            )
        }
        return DefaultCredentialsProvider.builder().build()
    }

    @Bean
    CognitoIdentityProviderClient cognitoIdpClient(@Qualifier('awsCredentialsProvider') AwsCredentialsProvider awsCredentialsProvider) {
        def region = grailsApplication.config.getProperty('cognito.region')

        CognitoIdentityProviderClient cognitoIdp = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider)
                .build()

        return cognitoIdp
    }

    @Bean
    DynamoDbClient dynamoDbClient(@Qualifier('awsCredentialsProvider') AwsCredentialsProvider awsCredentialsProvider) {
        def region = grailsApplication.config.getProperty('cognito.region')

        return DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider)
                .build()
    }

    @Bean('userService')
    IUserService userService(TokenService tokenService, EmailService emailService, CognitoIdentityProviderClient cognitoIdp, JwtProperties jwtProperties,
                             LocationService locationService, AuthService authService) {

        CognitoUserService userService = new CognitoUserService()
        userService.cognitoIdp = cognitoIdp
        userService.poolId = grailsApplication.config.getProperty('cognito.poolId')

        userService.emailService = emailService
        userService.tokenService = tokenService
        userService.jwtProperties = jwtProperties
        userService.locationService = locationService
        userService.authService = authService

        userService.affiliationsEnabled = grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)
        userService.socialLoginGroups = grailsApplication.config.getProperty('users.delegated-group-names', List, [])
        userService.useGatewayAPI = grailsApplication.config.getProperty('userdetails.api.useGatewayAPI', Boolean, false)

        return userService
    }

    @Bean('passwordOperations')
    IPasswordOperations passwordOperations(CognitoIdentityProviderClient cognitoIdp, OidcClientProperties oidcClientProperties) {
        return new CognitoPasswordOperations(cognitoIdp: cognitoIdp, poolId: grailsApplication.config.getProperty('cognito.poolId'),
                oidcClientProperties: oidcClientProperties)
    }

    @Bean('applicationService')
    IApplicationService applicationService(CognitoIdentityProviderClient cognitoIdp, IUserService userService, DynamoDbClient dynamoDbClient) {

        def poolId = grailsApplication.config.getProperty('cognito.poolId')
        def supportedIdentityProviders = grailsApplication.config.getProperty('oauth.support.dynamic.client.supportedIdentityProviders', List, [])
        def authFlows = grailsApplication.config.getProperty('oauth.support.dynamic.client.authFlows', List, []).collect { ExplicitAuthFlowsType.fromValue(it.toString()) }
        def clientScopes = grailsApplication.config.getProperty('oauth.support.dynamic.client.scopes', List, [])
        def galahCallbackURLs = grailsApplication.config.getProperty('oauth.support.dynamic.client.galah.callbackURLs', List, [])
        def tokensCallbackURLs = grailsApplication.config.getProperty('oauth.support.dynamic.client.tokens.callbackURLs', List, [])
        def dynamoDBTable = grailsApplication.config.getProperty('oauth.support.dynamic.client.dynamoDBTableName', String, null)
        def dynamoDBPK = grailsApplication.config.getProperty('oauth.support.dynamic.client.dynamoDBTable.dynamoDBPK', String, null)
        def dynamoDBSK = grailsApplication.config.getProperty('oauth.support.dynamic.client.dynamoDBTable.dynamoDBSK', String, null)

        CognitoApplicationService applicationService = new CognitoApplicationService(
                userService: userService,
                cognitoIdp: cognitoIdp,
                poolId: poolId,
                supportedIdentityProviders: supportedIdentityProviders,
                authFlows: authFlows,
                clientScopes: clientScopes,
                galahCallbackURLs: galahCallbackURLs,
                dynamoDbClient: dynamoDbClient,
                dynamoDBTable: dynamoDBTable,
                dynamoDBPK: dynamoDBPK,
                dynamoDBSK: dynamoDBSK,
                tokensCallbackURLs: tokensCallbackURLs
        )


        return applicationService
    }
}