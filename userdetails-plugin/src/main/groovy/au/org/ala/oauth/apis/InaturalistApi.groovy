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

package au.org.ala.oauth.apis

import com.github.scribejava.core.builder.api.DefaultApi20
import grails.util.Holders

class InaturalistApi extends DefaultApi20 {

    private static final String DEFAULT_BASE_URL = 'https://www.inaturalist.org/'

    private static class InstanceHolder {
        private static final InaturalistApi INSTANCE = new InaturalistApi()
    }

    static InaturalistApi instance() {
        return InstanceHolder.INSTANCE
    }

    static String getBaseUrl() {
        Holders.config.getProperty("inaturalist.baseUrl", String, DEFAULT_BASE_URL)
    }

    @Override
    String getAccessTokenEndpoint() {
        return "${baseUrl}oauth/token"
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "${baseUrl}oauth/authorize"
    }
}
