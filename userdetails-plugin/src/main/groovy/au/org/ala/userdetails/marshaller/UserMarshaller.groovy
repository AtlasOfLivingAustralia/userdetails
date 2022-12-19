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

package au.org.ala.userdetails.marshaller


import au.org.ala.users.UserRecord
import grails.converters.JSON

/**
 * Registers two custom marshallers for the UserRecord class - one that includes properties, one that doesn't.
 */
class UserMarshaller {

    public static final String WITH_PROPERTIES_CONFIG = 'withProperties'

    private Map toMap(UserRecord user) {
        [
                userId: user.uiId.toString(),
                userName: user.userName,
                firstName: user.firstName,
                lastName: user.lastName,
                email: user.email,
                activated: user.activated,
                locked: user.locked,
                roles: user.getUserRoles()*.toString() ?: []
        ]
    }

    void register(){

        JSON.createNamedConfig(WITH_PROPERTIES_CONFIG) {
            it.registerObjectMarshaller(UserRecord) { UserRecord user ->
                Map userMap = toMap(user)
                userMap.props = user.propsAsMap()
                userMap
            }
        }
        JSON.registerObjectMarshaller(UserRecord) { UserRecord user ->
            toMap(user)
        }
    }

}
