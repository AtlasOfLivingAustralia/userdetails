package au.org.ala.userdetails

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import org.springframework.boot.actuate.health.DataSourceHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.session.data.redis.config.ConfigureRedisAction

import javax.sql.DataSource

@Slf4j
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @ConditionalOnProperty('spring.session.disable-redis-config-action')
    @Bean
    ConfigureRedisAction configureRedisAction() {
        ConfigureRedisAction.NO_OP
    }

    @Bean
    DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        new DataSourceHealthIndicator(dataSource)
    }

}