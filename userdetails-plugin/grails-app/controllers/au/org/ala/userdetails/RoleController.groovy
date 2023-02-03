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
import au.org.ala.users.RoleRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException

@PreAuthorise
class RoleController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {

        PagedResult<RoleRecord> result = userService.listRoles(params)

        [ roleInstanceList: result.list, roleInstanceTotal: result.count, nextToken: result.nextPageToken ]
    }

    def create() {
        [roleInstance: new RoleRecord() ]
    }

    def save() {

        def pattern = ~/[A-Z_]{1,}/

        if(pattern.matcher(params.role).matches()){
            def roleInstance = new RoleRecord(role: params.role, description: params.description)
            def saved = userService.addRole(roleInstance) // roleInstance.save(flush: true)
            if (!saved) {
                render(view: "create", model: [roleInstance: roleInstance])
                return
            }
            flash.message = message(code: 'default.created.message', args: [message(code: 'role.label', default: 'Role'), roleInstance.role])
            redirect(action: "list")
        } else {
            flash.message = 'RoleRecord must consist of uppercase characters and underscores only'
            redirect(action: "create")
        }
    }

}
