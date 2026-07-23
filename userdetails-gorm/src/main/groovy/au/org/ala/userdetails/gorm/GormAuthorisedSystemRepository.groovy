package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.IAuthorisedSystemRepository
import au.org.ala.userdetails.NotFoundException
import au.org.ala.users.IAuthorisedSystem
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource
import org.springframework.dao.DataIntegrityViolationException

@Slf4j
@Transactional
class GormAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    MessageSource messageSource

    GormAuthorisedSystemRepository(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    @Override
    @NotTransactional
    IAuthorisedSystem create(GrailsParameterMap params) {
        return new AuthorisedSystem(params)
    }

    @Override
    @Transactional(readOnly = true)
    Boolean findByHost(String host) {
        return AuthorisedSystem.findByHost(host) != null
    }

    @Override
    @Transactional(readOnly = true)
    def list(GrailsParameterMap params) {
        def list = []
        def count = 0
        def query = params.q as String
        if (query) {
            def c = AuthorisedSystem.createCriteria()
            list = c.list(params) {
                or {
                    ilike('host', "%${query}%")
                    ilike('description', "%${query}%")
                }
            }
            count = list.totalCount
        } else {
            list = AuthorisedSystem.list(params)
            count = AuthorisedSystem.count()
        }

        return [list: list, count: count]
    }

    @Override
    IAuthorisedSystem save(GrailsParameterMap params) {
        def authorisedSystemInstance = new AuthorisedSystem(host: params.host, description: params.description)
        if (!authorisedSystemInstance.save(flush: true)) {
            return null
        }
        return authorisedSystemInstance
    }

    @Override
    @Transactional(readOnly = true)
    IAuthorisedSystem get(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        return authorisedSystemInstance
    }

    @Override
    IAuthorisedSystem update(GrailsParameterMap params, Locale locale) {
        def authorisedSystemInstance = AuthorisedSystem.get(params.id as Long)
        if (!authorisedSystemInstance) {
            throw new NotFoundException('')
        }

        if (params.version != null) {
            if (authorisedSystemInstance.version > params.version as Long) {
                authorisedSystemInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                        [messageSource.getMessage('authorisedSystem.label', [] as Object[], 'AuthorisedSystem', locale)] as Object[],
                        "Another user has updated this AuthorisedSystem while you were editing")
                return null
            }
        }

        authorisedSystemInstance.properties = params
        authorisedSystemInstance = authorisedSystemInstance.save(validate: true, failOnError: true)

        return authorisedSystemInstance
    }

    @Override
    Boolean delete(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            return false
        }
        try {
            authorisedSystemInstance.delete(flush: true)
        }
        catch (DataIntegrityViolationException e) {
            return false
        }
        return true
    }
}
