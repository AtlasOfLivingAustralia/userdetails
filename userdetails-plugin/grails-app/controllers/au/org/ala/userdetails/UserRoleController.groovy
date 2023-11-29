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
import au.org.ala.users.IUser
import au.org.ala.users.IUserRole
import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@PreAuthorise
class UserRoleController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {
        redirect(action: "list", params: params)
    }

    def create() {

        IUser user = userService.getUserById(params['user.id'])

        def roles = userService.listRoles()

        //remove existing roles this user has
        def usersRoles = user?.roles

        def acquiredRoles = []
        usersRoles.each { acquiredRoles << it.roleObject }

        roles.removeAll(acquiredRoles)

        [user: user, roles:roles]
    }

    def list() {

        PagedResult<IUserRole> model = userService.findUserRoles(params.role, params)
        withFormat {

            html { [userRoleInstanceList: model.list, userRoleInstanceTotal: model.count, nextToken: model.nextPageToken] }
            json {
                Map toRender = [users:model.list.collect{it.user}, count:model.count, nextToken: model.nextPageToken]
                render toRender as JSON
            }
        }
    }

    def addRole() {

        log.debug(params.userId + " - " + params.role.id)

        def user = userService.getUserById(params.userId)

        userService.addUserRole(user.userId, params.role.id)

        redirect(action: "show", controller: 'user', id: user.userId)
    }

    def deleteRole() {

        try {
            def result = userService.removeUserRole(params.userId, params.role)
            if (result) {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'userRole.label', default: 'UserRole'), params.role])
                redirect(controller:"user", action: "edit", id:params.userId)
            } else {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'userRole.label', default: 'UserRole'), params.role])
                redirect(controller:"user", action: "edit", id:params.userId)
            }
        } catch (Exception e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'userRole.label', default: 'UserRole'), params.role])
            redirect(controller:"user", action: "edit", id: params.userId)
        }


    }
}
