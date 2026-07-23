package au.org.ala.userdetails

import au.org.ala.users.AuthorisedSystemRecord
import au.org.ala.users.IAuthorisedSystem
import grails.core.GrailsApplication
import grails.util.Environment
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.NotImplementedException
import org.grails.config.PropertySourcesConfig
import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.env.PropertySources
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

@Slf4j
class ConfigAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    GrailsApplication grailsApplication

    @Override
    IAuthorisedSystem create(GrailsParameterMap params) {
        return new AuthorisedSystemRecord(host: params.host, description: params.description)
    }

    @Override
    Boolean findByHost(String host) {
        def list = grailsApplication.config.getProperty('authorised.systems', List, [])
                .collect{ it as AuthorisedSystemRecord }
        return list.count{it.host ==  host } > 0
    }

    @Override
    def list(GrailsParameterMap params) {
        def list = []
        def count = 0
        def query = params.q as String
        def reload = params.getBoolean("reload", false)
        if(reload) {
            try {
                grailsApplication.getConfig().merge(getConfig())
            } catch (Exception e) {
                log.error "Unable to reload configuration. Please correct problem and try again: ${e}", e
            }
        }

        def fullList = grailsApplication.config.getProperty('authorised.systems', List, [])
                .collect{ it as AuthorisedSystemRecord }
        def max = Math.min(params.max ?: 10, fullList.size())
        def offset = (params.offset?: 0) as int

        if (query) {
            def filteredList = fullList.findAll{ it.host?.toLowerCase()?.contains(query.toLowerCase())
                    || it.description?.toLowerCase()?.contains(query.toLowerCase())}
            list = filteredList.subList(offset, offset + max <= filteredList.size() ? offset + max : filteredList.size())
            count = filteredList.size()

        } else {
            list = fullList.subList(offset, offset + max <= fullList.size() ? offset + max : fullList.size())
            count = fullList.size()
        }

        return [list: list, count: count]
    }

    @Override
    IAuthorisedSystem save(GrailsParameterMap params) {
        throw new NotImplementedException()
    }

    @Override
    IAuthorisedSystem get(Long id) {
        def records = grailsApplication.config.getProperty('authorised.systems', List, [])
                .collect{ it as AuthorisedSystemRecord }
        return records.find{ it.id == id }
    }


    @Override
    IAuthorisedSystem update(GrailsParameterMap params, Locale locale) {
        throw new NotImplementedException()
    }

    @Override
    Boolean delete(Long id) {
        throw new NotImplementedException()
    }

    private ConfigObject getConfig() {
        def configLocations = grailsApplication.config.getProperty("grails.config.locations", List, [])
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        ConfigObject config = new ConfigObject()

        configLocations.each { location ->
            log.info "reloading config file: ${location}"
            Resource resource = resolver.getResource(location)
            InputStream stream = null

            try {
                stream = resource.getInputStream()
                ConfigSlurper configSlurper = new ConfigSlurper(Environment.current.name)
                def props = null

                if (resource.filename.endsWith('.groovy')) {
                    props = stream.text
                } else if (resource.filename.endsWith('.properties')) {
                    props = new Properties()
                    props.load(stream)
                } else if (resource.filename.endsWith('.yml')) {
                    def mapPropertySource = new YamlPropertySourceLoader().load( "yml config", resource, null )
                    def sources = mapPropertySource.getIndices().collect() {mapPropertySource.get(it).getSource() }
                    props = new Properties()
                    sources.each {
                        props << new PropertySourcesConfig(it)
                    }
                }

                if (props) {
                    config.merge(configSlurper.parse(props))
                }
            } catch (Exception ex) {
                log.warn ex.message
            } finally {
                stream?.close()
            }
        }
        config
    }
}
