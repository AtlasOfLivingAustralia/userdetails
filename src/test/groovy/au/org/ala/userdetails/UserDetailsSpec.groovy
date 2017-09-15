package au.org.ala.userdetails

import au.org.ala.userdetails.marshaller.UserMarshaller
import grails.converters.JSON
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import java.sql.Timestamp

/**
 * Base class for user details tests.  Sets up some domain objects to support testing.
 */

abstract class UserDetailsSpec extends Specification {


    // These classes are a workaround for the difficulty in injecting Spock mocks into the filter class in unit tests.
    public static class UnAuthorised extends AuthorisedSystemService {
        @Override
        def isAuthorisedSystem(HttpServletRequest request) {
            return false
        }
    }

    public static class Authorised extends AuthorisedSystemService {
        @Override
        def isAuthorisedSystem(HttpServletRequest request) {
            return true
        }
    }
    void registerMarshallers() {
        // There are issues where tests fail the first time due to the custom marshallers not being registered.
        // This is a workaround to ensure the defaults are set.
        ConvertersConfigurationHolder.setDefaultConfiguration(JSON, ConvertersConfigurationHolder.getConverterConfiguration(JSON))

        new UserMarshaller().register()
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    void cleanup() {
        User.deleteAll()
        Role.deleteAll()
        UserRole.deleteAll()
        UserProperty.deleteAll()
    }


    protected User createUser(String tempAuthKey = "") {
        Role role = Role.findOrCreateWhere(role: 'ROLE_USER', description:"Everyone has this role")
        role.save(failOnError: true, flush: true)


        User user = new User(firstName: 'test first', lastName: 'test last', email: 'test@test.com', userName:'test@test.com', activated: true, locked: false, created: new Timestamp(System.currentTimeMillis()), tempAuthKey: tempAuthKey)
        user.save(failOnError: true, flush: true)

        UserRole userRole = new UserRole(user:user, role:role)
        userRole.save(failOnError: true, flush: true)


        [prop1:'prop1', prop2:'prop2'].each { k,v ->
            UserProperty prop = new UserProperty(user:user, property:k, value:v)
            prop.save(failOnError: true, flush: true)
        }

        User.findById(user.id)
    }
}
