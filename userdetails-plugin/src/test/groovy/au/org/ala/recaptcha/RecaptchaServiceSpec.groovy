/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 */

package au.org.ala.recaptcha

import grails.testing.services.ServiceUnitTest
import retrofit2.mock.Calls
import spock.lang.Specification

class RecaptchaServiceSpec extends Specification implements ServiceUnitTest<RecaptchaService> {

    RecaptchaClient recaptchaClient = Mock()

    void setup() {
        service.recaptchaClient = recaptchaClient
        grailsApplication.config.recaptcha.siteKey = 'site-key'
        grailsApplication.config.recaptcha.projectId = 'project-id'
        grailsApplication.config.recaptcha.apiKey = 'api-key'
        grailsApplication.config.recaptcha.scoreThreshold = 0.5d
    }

    def "verification succeeds for a valid Enterprise assessment"() {
        when:
        boolean valid = service.verify('token', 'register', '127.0.0.1', 'test-agent')

        then:
        1 * recaptchaClient.assess('project-id', 'api-key', {
            RecaptchaRequest request ->
                request.event.token == 'token' &&
                        request.event.siteKey == 'site-key' &&
                        request.event.expectedAction == 'register' &&
                        request.event.userIpAddress == '127.0.0.1' &&
                        request.event.userAgent == 'test-agent'
        }) >> { Calls.response(enterpriseResponse(true, 0.9d, 'register')) }
        valid
    }

    def "verification is bypassed when recaptcha is disabled"() {
        given:
        grailsApplication.config.recaptcha.siteKey = ''

        when:
        boolean valid = service.verify(null, 'register', '127.0.0.1', null)

        then:
        0 * recaptchaClient._
        valid
    }

    def "verification fails when Enterprise configuration is incomplete"() {
        given:
        grailsApplication.config.recaptcha.apiKey = ''

        when:
        boolean valid = service.verify('token', 'register', '127.0.0.1', null)

        then:
        0 * recaptchaClient._
        !valid
    }

    def "verification fails without calling Google when the token is missing"() {
        when:
        boolean valid = service.verify(null, 'register', '127.0.0.1', null)

        then:
        0 * recaptchaClient._
        !valid
    }

    def "verification rejects Enterprise assessment: #scenario"() {
        when:
        boolean valid = service.verify('token', 'register', '127.0.0.1', null)

        then:
        1 * recaptchaClient.assess('project-id', 'api-key', _ as RecaptchaRequest) >> { Calls.response(assessment) }
        !valid

        where:
        scenario        | assessment
        'invalid token' | enterpriseResponse(false, null, null, 'MISSING')
        'low score'     | enterpriseResponse(true, 0.4d, 'register')
        'wrong action'  | enterpriseResponse(true, 0.9d, 'password_reset')
    }

    private static RecaptchaResponse enterpriseResponse(boolean valid, Double score, String action, String invalidReason = null) {
        new RecaptchaResponse(
                new RecaptchaResponse.TokenProperties(valid, invalidReason, 'test-host', action),
                new RecaptchaResponse.RiskAnalysis(score, [])
        )
    }
}
