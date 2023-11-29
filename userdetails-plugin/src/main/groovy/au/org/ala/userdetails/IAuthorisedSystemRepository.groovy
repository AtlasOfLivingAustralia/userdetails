package au.org.ala.userdetails

import au.org.ala.users.IAuthorisedSystem
import grails.web.servlet.mvc.GrailsParameterMap

/**
 * Abstract checking whether a host is allowed
 */
interface IAuthorisedSystemRepository {

    IAuthorisedSystem create(GrailsParameterMap params)

    /**
     * Return true if the given host parameter is authorised
     * @param host
     * @return
     */
    Boolean findByHost(String host)

    def list(GrailsParameterMap params)

    IAuthorisedSystem save(GrailsParameterMap params)

    IAuthorisedSystem get(Long id)

    IAuthorisedSystem update(GrailsParameterMap params, Locale locale)

    Boolean delete(Long id)
}
