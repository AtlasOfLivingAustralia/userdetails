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

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class RecaptchaRequest {
    Event event

    @Canonical
    @CompileStatic
    static class Event {
        String token
        String siteKey
        String expectedAction
        String userIpAddress
        String userAgent
    }
}
