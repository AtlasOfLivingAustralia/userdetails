package au.org.ala.userdetails

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.web.InterceptorUnitTestMixin
import org.apache.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.InterceptorUnitTestMixin} for usage instructions
 */
@TestFor(UserDetailsWebServicesInterceptor)
@TestMixin([InterceptorUnitTestMixin, GrailsUnitTestMixin])
class UserDetailsWebServicesInterceptorSpec extends Specification {

    static doWithSpring = {
        authorisedSystemService(UserDetailsSpec.UnAuthorised)
    }

    def setup() {

    }

    def cleanup() {

    }

    void "Test userDetailsWebServices interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"userDetails")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

    void "Unauthorised systems should not be able to use the UserDetailsController web services"() {

        setup:
        def controller = new UserDetailsController()
        grailsApplication.addArtefact("Controller", UserDetailsController)

        when:
        request.method = 'POST'
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userDetails')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'getUserDetails')
        withInterceptors(controller: 'userDetails', action:'getUserDetails') {
            controller.getUserDetails()
        }
        then:
        response.status == HttpStatus.SC_UNAUTHORIZED

        when:
        response.reset()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userDetails')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'getUserList')
        withInterceptors(controller: 'userDetails', action:'getUserList') {
            request.method = 'POST'
            controller.getUserList()
        }
        then:
        response.status == HttpStatus.SC_UNAUTHORIZED

        when:
        response.reset()
        request.method = 'POST'
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userDetails')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'getUserListWithIds')
        withInterceptors(controller: 'userDetails', action:'getUserListWithIds') {
            controller.getUserListWithIds()
        }
        then:
        response.status == HttpStatus.SC_UNAUTHORIZED

        when:
        response.reset()
        request.method = 'POST'
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userDetails')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'getUserListFull')
        withInterceptors(controller: 'userDetails', action:'getUserListFull') {
            controller.getUserListFull()
        }
        then:
        response.status == HttpStatus.SC_UNAUTHORIZED

        when:
        response.reset()
        request.method = 'POST'
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userDetails')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'getUserDetailsFromIdList')
        withInterceptors(controller: 'userDetails', action:'getUserDetailsFromIdList') {
            controller.getUserDetailsFromIdList()
        }
        then:
        response.status == HttpStatus.SC_UNAUTHORIZED
    }
}
