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

import grails.converters.JSON

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_OK

class BaseController {
    public static final String CONTEXT_TYPE_JSON = "application/json"

    protected def notFound(String message = null) {
        sendError(SC_NOT_FOUND, message ?: "")
    }

    protected def badRequest(String message = null) {
        sendError(SC_BAD_REQUEST, message ?: "")
    }

    protected def success(resp) {
        response.status = SC_OK
        response.setContentType(CONTEXT_TYPE_JSON)
        render resp as JSON
    }

    protected def saveFailed() {
        sendError(SC_INTERNAL_SERVER_ERROR)
    }

    protected def sendError(int status, String msg = null) {
        response.status = status
        response.sendError(status, msg)
    }
}
