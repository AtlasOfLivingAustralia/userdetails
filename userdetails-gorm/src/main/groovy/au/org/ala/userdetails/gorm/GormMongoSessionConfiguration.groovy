package au.org.ala.userdetails.gorm

import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession

@Configuration
@ConditionalOnProperty(value = 'spring.session.enabled', havingValue = 'true', matchIfMissing = false)
@Import(MongoAutoConfiguration)
@EnableMongoHttpSession
@EnableConfigurationProperties(MongoProperties)
class GormMongoSessionConfiguration {

    @Bean
    MongoHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
        new MongoHealthIndicator(mongoTemplate)
    }
}
