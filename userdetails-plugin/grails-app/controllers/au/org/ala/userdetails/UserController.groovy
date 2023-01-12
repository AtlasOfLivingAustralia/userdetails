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
import au.org.ala.users.UserRecord
import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException

@PreAuthorise
class UserController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def passwordService

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
            def result = userService.listUsers(params)
            [ userInstanceList: result.list, userInstanceTotal: result.count, nextToken: result.nextPageToken ]
    }

    def create() {
        [userInstance: new UserRecord()]
    }

    @Transactional
    def save() {
        UserRecord user = userService.registerUser(params)

        if (!user) {
            render(view: "create", model: [userInstance: new UserRecord()])
            return
        }
        userService.sendAccountActivation(user)

        flash.message = message(code: 'default.created.message', args: [message(code: 'user.label', default: 'User'), user.id])
        redirect(action: "show", id: user.id)
    }

    def show(String id) {

        def userInstance = userService.getUserById(id)

        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'UserRecord'), id])
            redirect(action: "list")
            return
        }

        String resetPasswordUrl = passwordService.getResetPasswordUrl(userInstance)

        [userInstance: userInstance, resetPasswordUrl: resetPasswordUrl]
    }

    def edit(String id) {
        def userInstance = userService.getUserById(id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'UserRecord'), id])
            redirect(action: "list")
            return
        }
        [userInstance: userInstance, props:userInstance.propsAsMap()]
    }

    def update(String id, Long version) {

        def userInstance= userService.getUserById(id)

        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'UserRecord'), id])
            redirect(action: "list")
            return
        }

        // TODO: deal with optimistic locking
//        if (version != null) {
//            if (userInstance.version > version) {
//                userInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
//                          [message(code: 'user.label', default: 'UserRecord')] as Object[],
//                          "Another user has updated this UserRecord while you were editing")
//                render(view: "edit", model: [userInstance: userInstance])
//                return
//            }
//        }
//
//        if (userInstance.email != params.email) {
//            params.userName = params.email
//        }

        def success = userService.updateUser(id, params)

        if (!success) {
            render(view: "edit", model: [ userInstance: userInstance ])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'user.label', default: 'UserRecord'), userInstance.id])
        redirect(action: "show", id: userInstance.id)
    }

    def delete(Long id) {

        def userInstance = userService.getUserById(params.id as String)

        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'UserRecord'), params.id])
            redirect(action: "list")
            return
        }

        try {
            log.info("${request.userPrincipal?.name} is attempting to delete user ${userInstance.userName}")
            userService.deleteUser(userInstance)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'user.label', default: 'UserRecord'), params.id])
            redirect(action: "list")
        } catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'user.label', default: 'UserRecord'), params.id])
            redirect(action: "show", id: params.id)
        }

    }

    def disableMfa() {
        userService.enableMfa(params.userId, false)
        redirect(action: "edit", id: params.userId)
    }
}
