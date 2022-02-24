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

package au.org.ala.userdetails

import grails.testing.web.interceptor.InterceptorUnitTest
import org.apache.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.InterceptorUnitTestMixin} for usage instructions
 */
//@TestFor(UserDetailsWebServicesInterceptor)
//@TestMixin([InterceptorUnitTestMixin, GrailsUnitTestMixin])
class UserDetailsWebServicesInterceptorSpec extends Specification implements InterceptorUnitTest<UserDetailsWebServicesInterceptor> {

    Closure doWithSpring(){{ ->
        authorisedSystemService(UserDetailsSpec.UnAuthorised)
    }}

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
