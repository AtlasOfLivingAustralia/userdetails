package au.org.ala.userdetails

import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.userdetails.secrets.DefaultRandomStringGenerator
import au.org.ala.userdetails.secrets.RandomStringGenerator
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import grails.core.GrailsApplication
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.mongo.MongoHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Configuration
class UserDetailsGrailsPluginConfiguration {

    @Autowired
    GrailsApplication grailsApplication

    @Bean
    RecaptchaClient recaptchaClient() {
        def baseUrl = grailsApplication.config.getProperty('recaptcha.baseUrl', 'https://www.google.com/recaptcha/api/')
        return new Retrofit.Builder().baseUrl(baseUrl).client(new OkHttpClient()).addConverterFactory(MoshiConverterFactory.create()).build().create(RecaptchaClient)
    }

    @Configuration
    @ConditionalOnProperty(value = "spring.session.enabled", havingValue = "true", matchIfMissing = false)
    @Import(MongoAutoConfiguration) // unsure if this is disabled by grails?
    @EnableMongoHttpSession
    @EnableConfigurationProperties(MongoProperties)
    static class MongoSessionConfig {

//        @Bean
//        JdkMongoSessionConverter jdkMongoSessionConverter() {
//            return new JdkMongoSessionConverter(Duration.ofMinutes(15L));
//        }
        @Bean // TODO is this necessary?
        MongoHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
            new MongoHealthIndicator(mongoTemplate)
        }
    }

    @Bean('authorisedSystemRepository')
    @ConditionalOnMissingBean(name = 'authorisedSystemRepository')
    IAuthorisedSystemRepository authorisedSystemRepository() {
        new ConfigAuthorisedSystemRepository(grailsApplication: grailsApplication)
    }

    @Bean('awsRegion')
    Region awsRegion() {
        def region = grailsApplication.config.getProperty('aws.region') ?: grailsApplication.config.getProperty('cognito.region')
        return region ? Region.of(region) : Region.AP_SOUTHEAST_2
    }

    @Bean
    @Qualifier('gatewayAwsCredentialsProvider')
    @ConditionalOnProperty(name = 'apikey.type', havingValue = 'aws')
    AwsCredentialsProvider gatewayAwsCredentialsProvider() {
        def accessKey = grailsApplication.config.getProperty('apikey.aws.gateway.access-key')
        def secretKey = grailsApplication.config.getProperty('apikey.aws.gateway.secret-key')
        def sessionToken = grailsApplication.config.getProperty('apikey.aws.gateway.session-token')


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

    @Bean('apikeyService')
    @ConditionalOnMissingBean(name = 'apikeyService')
    @ConditionalOnProperty(name = 'apikey.type', havingValue = 'aws')
    IApikeyService apikeyService(
            @Qualifier('gatewayAwsCredentialsProvider') AwsCredentialsProvider gatewayAwsCredentialsProvider,
            IUserService userService, Region awsRegion) {

        // TODO @Bean this
        ApiGatewayClient gatewayIdp = ApiGatewayClient.builder()
                .region(awsRegion)
                .credentialsProvider(gatewayAwsCredentialsProvider)
                .build()

        return new AWSApikeyService(gatewayIdp, userService)
    }

    @Bean
    RandomStringGenerator randomStringGenerator() {
        new DefaultRandomStringGenerator(RandomStringGenerator.DEFAULT_LENGTH)
    }
}
