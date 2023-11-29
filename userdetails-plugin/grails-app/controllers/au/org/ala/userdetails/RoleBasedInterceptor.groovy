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

import au.org.ala.auth.PreAuthorise
import grails.converters.JSON
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder

class RoleBasedInterceptor {

    def authorisedSystemService

    RoleBasedInterceptor() {
        matchAll().except(uri:'/error')
    }

    boolean before() {
        // If the method or controller has the PreAuthorise annotation:
        // 1) For html we validate the user is the in role specified by the annotation
        // 2) For json requests we check the calling system is in the authorized systems list.
        def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
        Class controllerClass = controller?.clazz
        def method = controllerClass?.getMethod(actionName ?: "index", [] as Class[])

        if (method && (controllerClass.isAnnotationPresent(PreAuthorise) || method.isAnnotationPresent(PreAuthorise))) {
            boolean result = true
            PreAuthorise pa = method.getAnnotation(PreAuthorise) ?: controllerClass.getAnnotation(PreAuthorise)
            response.withFormat {
                json {
                    try{
                        if (!authorisedSystemService.isAuthorisedRequest(request, response, pa.requiredRole(), pa.requiredScope())) {
                            log.warn("Denying access to $actionName from remote addr: ${request.remoteAddr}, remote host: ${request.remoteHost}")
                            response.status = HttpStatus.SC_UNAUTHORIZED
                            render(['error': "Unauthorized"] as JSON)

                            result = false
                        }
                    }
                    catch (Exception e){
                        log.error(e.getMessage(), e)
                        response.sendError(HttpStatus.SC_UNAUTHORIZED)
                        return false
                    }
                }
                '*' {
                    def requiredRole = pa.requiredRole()
                    def inRole = request?.isUserInRole(requiredRole)

                    if (!inRole) {
                        log.warn("Denying access to $controllerName, $actionName to ${request?.userPrincipal?.name}")
                        flash.message = "Access denied: User does not have required permission."
                        redirect(uri: '/')
                        result = false
                    }
                }
            }
            return result
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
