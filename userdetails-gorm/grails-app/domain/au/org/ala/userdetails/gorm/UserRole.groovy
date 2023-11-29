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

package au.org.ala.userdetails.gorm

import au.org.ala.users.IUserRole
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = ['user', 'role'])
class UserRole implements IUserRole<User, Role>, Serializable {

    User user
    Role role

    static transients = ['owner', 'roleObject']

    static mapping = {
        id composite: ['user', 'role']
        version false
    }

    static constraints = {
    }
    String toString(){
        role
    }

    @Override
    User getOwner() {
        return user
    }

    @Override
    Role getRoleObject() {
        return role
    }
}