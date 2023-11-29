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

import au.org.ala.users.IRole

class Role implements IRole, Serializable {

    String role
    String description

    static mapping = {
        id (generator:'assigned', column:'role', type:'string' , name: 'role')
        version false
    }

    String toString(){
        role
    }

    static constraints = {
        role nullable: false, blank: false
        description nullable:true
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        Role role1 = (Role) o

        if (role != role1.role) return false

        return true
    }

    int hashCode() {
        return (role != null ? role.hashCode() : 0)
    }
}
