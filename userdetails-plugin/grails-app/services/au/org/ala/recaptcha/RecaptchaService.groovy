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

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j

@Slf4j
class RecaptchaService {

    static transactional = false

    GrailsApplication grailsApplication
    RecaptchaClient recaptchaClient

    boolean verify(String token, String expectedAction, String remoteIp, String userAgent) {
        String siteKey = grailsApplication.config.getProperty('recaptcha.siteKey')
        if (!siteKey) {
            return true
        }

        String projectId = grailsApplication.config.getProperty('recaptcha.projectId')
        String apiKey = grailsApplication.config.getProperty('recaptcha.apiKey')
        if (!projectId || !apiKey) {
            log.error('Recaptcha Enterprise is enabled but projectId or apiKey is missing')
            return false
        }

        if (!token) {
            log.warn('Recaptcha Enterprise token is missing')
            return false
        }

        try {
            def event = new RecaptchaRequest.Event(token, siteKey, expectedAction, remoteIp, userAgent)
            def response = recaptchaClient.assess(projectId, apiKey, new RecaptchaRequest(event)).execute()
            if (!response.isSuccessful()) {
                String errorBody = response.errorBody()?.string()
                log.warn('Recaptcha Enterprise assessment failed with HTTP status {}: {}', response.code(), errorBody)
                return false
            }

            RecaptchaResponse verification = response.body()
            Double scoreThreshold = grailsApplication.config.getProperty('recaptcha.scoreThreshold', Double, 0.7d)
            boolean valid = verification?.tokenProperties?.valid &&
                    verification.tokenProperties.action == expectedAction &&
                    verification.riskAnalysis?.score != null &&
                    verification.riskAnalysis.score >= scoreThreshold
            if (!valid) {
                log.warn('Recaptcha Enterprise verification failed for action {} with threshold {}: {}', expectedAction, scoreThreshold, verification)
            }
            return valid
        } catch (Exception e) {
            log.warn('Recaptcha Enterprise assessment request failed', e)
            return false
        }
    }
}
