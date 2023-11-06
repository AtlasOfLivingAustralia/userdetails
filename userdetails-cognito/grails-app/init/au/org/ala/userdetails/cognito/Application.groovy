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
import au.org.ala.web.OidcClientProperties
import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.auth.*
import com.amazonaws.regions.Region
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Bean

@Slf4j
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    AWSCredentialsProvider awsCredentialsProvider() {

        String accessKey = grailsApplication.config.getProperty('cognito.accessKey')
        String secretKey = grailsApplication.config.getProperty('cognito.secretKey')
        String sessionToken = grailsApplication.config.getProperty('cognito.sessionToken')

        AWSCredentialsProvider credentialsProvider
        if (accessKey && secretKey) {
            AWSCredentials credentials
            if (sessionToken) {
                credentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken)
            } else {
                credentials = new BasicAWSCredentials(accessKey, secretKey)
            }
            credentialsProvider = new AWSStaticCredentialsProvider(credentials)
        } else {
            credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance()
        }
        return credentialsProvider
    }

    @Bean
    AWSCognitoIdentityProvider cognitoIdpClient(AWSCredentialsProvider awsCredentialsProvider) {
        def region = grailsApplication.config.getProperty('cognito.region')

        AWSCognitoIdentityProvider cognitoIdp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withRegion(region)
                .withCredentials(awsCredentialsProvider)
                .build()

        return cognitoIdp
    }

    @Bean
    AmazonDynamoDB amazonDynamoDB(AWSCredentialsProvider awsCredentialsProvider, Region awsRegion) {
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(awsRegion.toString())
                .withCredentials(awsCredentialsProvider)
                .build()
    }

    @Bean('userService')
    IUserService userService(TokenService tokenService, EmailService emailService, AWSCognitoIdentityProvider cognitoIdp, JwtProperties jwtProperties) {

        CognitoUserService userService = new CognitoUserService()
        userService.cognitoIdp = cognitoIdp
        userService.poolId = grailsApplication.config.getProperty('cognito.poolId')

        userService.emailService = emailService
        userService.tokenService = tokenService
        userService.jwtProperties = jwtProperties

        userService.affiliationsEnabled = grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)
        userService.socialLoginGroups = grailsApplication.config.getProperty('users.delegated-group-names', List, [])

        return userService
    }

    @Bean('passwordOperations')
    IPasswordOperations passwordOperations(AWSCognitoIdentityProvider cognitoIdp, OidcClientProperties oidcClientProperties) {
        return new CognitoPasswordOperations(cognitoIdp: cognitoIdp, poolId: grailsApplication.config.getProperty('cognito.poolId'),
                oidcClientProperties: oidcClientProperties)
    }

    @Bean('applicationService')
    IApplicationService applicationService(AWSCognitoIdentityProvider cognitoIdp, IUserService userService, AmazonDynamoDB amazonDynamoDB) {

        def poolId = grailsApplication.config.getProperty('cognito.poolId')
        def supportedIdentityProviders = grailsApplication.config.getProperty('oauth.support.dynamic.client.supportedIdentityProviders', List, [])
        def authFlows = grailsApplication.config.getProperty('oauth.support.dynamic.client.authFlows', List, [])
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
                dynamoDB: amazonDynamoDB,
                dynamoDBTable: dynamoDBTable,
                dynamoDBPK: dynamoDBPK,
                dynamoDBSK: dynamoDBSK,
                tokensCallbackURLs: tokensCallbackURLs
        )


        return applicationService
    }
//
//    @Bean('apikeyService')
//    IApikeyService apikeyService(IUserService userService, AmazonApiGateway apiGatewayIdp) {
//        return new AWSApikeyService(userService: userService, apiGatewayIdp: apiGatewayIdp)
//    }
}