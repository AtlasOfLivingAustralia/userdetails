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

package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.EmailService
import au.org.ala.userdetails.IApplicationService
import au.org.ala.userdetails.IAuthorisedSystemRepository
import au.org.ala.userdetails.IPasswordOperations
import au.org.ala.userdetails.IUserService
import au.org.ala.userdetails.LocationService
import au.org.ala.userdetails.PasswordService
import au.org.ala.userdetails.secrets.DefaultRandomStringGenerator
import au.org.ala.userdetails.secrets.RandomStringGenerator
import au.org.ala.web.AuthService
import au.org.ala.ws.service.WebService
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean

import javax.sql.DataSource

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry
import static org.bson.codecs.configuration.CodecRegistries.fromProviders
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries

@Slf4j
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        new DataSourceHealthIndicator(dataSource)
    }

    @Bean
    AWSCredentialsProvider awsCredentialsProvider() {

        String accessKey = grailsApplication.config.getProperty('apigateway.accessKey')
        String secretKey = grailsApplication.config.getProperty('apigateway.secretKey')
        String sessionToken = grailsApplication.config.getProperty('apigateway.sessionToken')

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

//    @Bean
//    AmazonApiGateway gatewayIdpClient(AWSCredentialsProvider awsCredentialsProvider) {
//        def region = grailsApplication.config.getProperty('apigateway.region')
//
//        AmazonApiGateway gatewayIdp = AmazonApiGatewayClientBuilder.standard()
//                .withRegion(region)
//                .withCredentials(awsCredentialsProvider)
//                .build()
//
//        return gatewayIdp
//    }

    @Bean('userService')
    IUserService userService(GrailsApplication grailsApplication,
                             EmailService emailService,
                             PasswordService passwordService,
                             AuthService authService,
                             LocationService locationService,
                             MessageSource messageSource,
                             WebService webService
                             ) {

//        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserRecord)
//        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserPropertyRecord)
//        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserRoleRecord)

        GormUserService userService = new GormUserService()
        userService.emailService = emailService
        userService.passwordService = passwordService
        userService.authService = authService
        userService.locationService = locationService
        userService.webService = webService

        userService.grailsApplication = grailsApplication
        userService.messageSource = messageSource

        userService.affiliationsEnabled = grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)

        return userService
    }

    @Bean('authorisedSystemRepository')
    IAuthorisedSystemRepository authorisedSystemRepository(MessageSource messageSource) {
        new GormAuthorisedSystemRepository(messageSource)
    }

    @Bean('passwordOperations')
    IPasswordOperations passwordOperations(EmailService emailService) {
        new GormPasswordOperations(emailService: emailService)
    }

    @Bean('applicationServiceMongoCodecRegistry')
    CodecRegistry codecRegistry() {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
                .register(Cas66Service.class)
                .build()
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider))
        return pojoCodecRegistry
    }

    @Bean('applicationServiceMongoClient')
    MongoClient mongoClient(CodecRegistry codecRegistry) {
        def appName = grailsApplication.config.getProperty('info.app.name')
        def connectionString = grailsApplication.config.getProperty('applications.mongo.uri')
        def username = grailsApplication.config.getProperty('applications.mongo.username')
        def password = grailsApplication.config.getProperty('applications.mongo.password')
        def authDb = grailsApplication.config.getProperty('applications.mongo.auth-db')

        def builder = MongoClientSettings.builder()
                .applicationName(appName)
                .applyConnectionString(new ConnectionString(connectionString))

        if (username && password) {
            builder = builder.credential(MongoCredential.createCredential(username, authDb ?: '', password.toCharArray()))
        }

        MongoClientSettings settings =  builder.codecRegistry(codecRegistry).build()
        MongoClient mongoClient = MongoClients.create(settings)
        return mongoClient
    }

    @Bean('applicationService')
    IApplicationService applicationService(MongoClient mongoClient, RandomStringGenerator randomStringGenerator) {
        def database = grailsApplication.config.getProperty('applications.mongo.database')
        def collection = grailsApplication.config.getProperty('applications.mongo.collection')

        def supportedIdentityProviders = grailsApplication.config.getProperty('oauth.support.dynamic.client.supportedIdentityProviders', List, [])
        def authFlows = grailsApplication.config.getProperty('oauth.support.dynamic.client.authFlows', List, [])
        def clientScopes = grailsApplication.config.getProperty('oauth.support.dynamic.client.scopes', List, [])
        def galahCallbackURLs = grailsApplication.config.getProperty('oauth.support.dynamic.client.galah.callbackURLs', List, [])
        def tokensCallbackURLs = grailsApplication.config.getProperty('oauth.support.dynamic.client.tokens.callbackURLs', List, [])

        GormApplicationService applicationService = new GormApplicationService(mongoClient: mongoClient,
                mongoDbName: database, mongoCollectionName: collection, randomStringGenerator: randomStringGenerator,
                supportedIdentityProviders: supportedIdentityProviders, authFlows: authFlows,
                clientScopes: clientScopes, galahCallbackURLs: galahCallbackURLs, tokensCallbackURLs: tokensCallbackURLs)

        return applicationService
    }
}