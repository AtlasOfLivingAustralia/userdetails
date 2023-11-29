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
import au.org.ala.users.AuthorisedSystemRecord
import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

@PreAuthorise
class AuthorisedSystemController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    @Autowired
    IAuthorisedSystemRepository authorisedSystemRepository

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def response =  authorisedSystemRepository.list(params)
        [authorisedSystemInstanceList: response.list, authorisedSystemInstanceTotal: response.count]
    }

    def create() {
        [authorisedSystemInstance: authorisedSystemRepository.create(params)]
    }

    def save() {
        def authorisedSystemInstance = authorisedSystemRepository.save(params)
        if (!authorisedSystemInstance) {
            render(view: "create", model: [authorisedSystemInstance: authorisedSystemRepository.create(params)])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), authorisedSystemInstance.id])
        redirect(action: "show", id: authorisedSystemInstance.id)
    }

    def show(Long id) {
        def authorisedSystemInstance = authorisedSystemRepository.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        [authorisedSystemInstance: authorisedSystemInstance]
    }

    def edit(Long id) {
        def authorisedSystemInstance = authorisedSystemRepository.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        [authorisedSystemInstance: authorisedSystemInstance]
    }

    @Transactional
    def update(Long id) {

        try {
            def authorisedSystemInstance = authorisedSystemRepository.update(params, request.locale)
            if (!authorisedSystemInstance) {
                render(view: "edit", model: [authorisedSystemInstance: authorisedSystemInstance])
                return
            }
            flash.message = message(code: 'default.updated.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), authorisedSystemInstance.id])
            redirect(action: "show", id: authorisedSystemInstance.id)
        } catch (NotFoundException e) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }
    }

    def delete(Long id) {
        def authorisedSystemInstance = authorisedSystemRepository.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        try {
            authorisedSystemRepository.delete(id)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "show", id: id)
        }
    }

    def ajaxResolveHostName() {

        def host = params.host as String

        def hostname = "?"
        def reachable = false
        if (host) {
            try {
                InetAddress addr = InetAddress.getByName(host);
                hostname = addr.getHostName();
                reachable = addr.isReachable(2000);
            } catch (Exception ex) {
                ex.printStackTrace()
            }
        }

        render([host:host, hostname: hostname, reachable: reachable] as JSON)
    }
}
