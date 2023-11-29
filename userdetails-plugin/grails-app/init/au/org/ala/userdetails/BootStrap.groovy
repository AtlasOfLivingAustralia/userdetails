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


import au.org.ala.users.RoleRecord
import org.springframework.beans.factory.annotation.Autowired

class BootStrap {
    def messageSource

    def customObjectMarshallers

    @Autowired
    IUserService userService

    def init = { servletContext ->
        log.info("Running bootstrap queries")

        messageSource.setBasenames(
                "file:///var/opt/atlas/i18n/userdetails/messages",
                "file:///opt/atlas/i18n/userdetails/messages",
                "WEB-INF/grails-app/i18n/messages",
                "classpath:messages"
        )

        customObjectMarshallers.register()
        addRoles()
        log.info("Done bootstrap queries.")
    }

    def addRoles() {
        if (userService.listRoles().size() == 0) {
            userService.addRoles([
                new RoleRecord(role:'ROLE_ABRS_ADMIN', description:''),
                new RoleRecord(role:'ROLE_ADMIN', description:''),
                new RoleRecord(role:'ROLE_COLLECTION_ADMIN', description:''),
                new RoleRecord(role:'ROLE_COLLECTION_EDITOR', description:''),
                new RoleRecord(role:'ROLE_COLLECTORS_ADMIN', description:''),
                new RoleRecord(role:'ROLE_SYSTEM_ADMIN', description:''),
                new RoleRecord(role:'ROLE_USER', description:''),
                new RoleRecord(role:'ROLE_AVH_CLUB', description:''),
                new RoleRecord(role:'ROLE_VP_ADMIN', description:''),
                new RoleRecord(role:'ROLE_ABRS_INSTITUTION', description:''),
                new RoleRecord(role:'ROLE_SPATIAL_ADMIN', description:''),
                new RoleRecord(role:'ROLE_VP_VALIDATOR', description:''),
                new RoleRecord(role:'ROLE_IMAGE_ADMIN', description:''),
                new RoleRecord(role:'ROLE_AVH_ADMIN', description:'')
            ])
        }

    }
}
