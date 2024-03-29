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

import java.sql.Timestamp

class User implements Serializable {

    static hasMany =  [userRoles:UserRole, userProperties:UserProperty]

    String firstName
    String lastName

    String userName
    String email

    Date dateCreated
    Date lastUpdated

    Timestamp lastLogin

    Boolean activated
    Boolean locked

    String tempAuthKey

    String displayName

    Collection<UserRole> userRoles

    static mapping = {
        table 'users'
        id (generator:'identity', column:'userid', type:'long')
        userName column:  'username'
        firstName column:  'firstname'
        lastName column:  'lastname'
        activated sqlType: 'char'
        locked sqlType: 'char'
        lastLogin type: Timestamp, sqlType: "timestamp"
        displayName formula: 'CONCAT_WS(" ", NULLIF(firstname,""), NULLIF(lastname,""))'
        version false
    }

    static constraints = {
        email nullable: true
        firstName  nullable: true
        lastName  nullable: true
        activated nullable: false
        locked nullable: false
        lastLogin nullable: true
        tempAuthKey nullable: true
        displayName nullable: true
    }

    static List<String[]> findNameAndEmailWhereEmailIsNotNull() {
        return User.withCriteria {
            isNotNull('email')
            projections {
                property('email')
                property('firstName')
                property('lastName')
            }
        }
    }

    static List<String[]> findIdFirstAndLastName() {
        return User.withCriteria {
            projections {
                property('id')
                property('firstName')
                property('lastName')
            }
        }
    }

    static List<String[]> findUserDetails() {
        return User.withCriteria {
            projections {
                property('id')
                property('firstName')
                property('lastName')
                property('userName')
                property('email')
            }
        }
    }

    def propsAsMap(){
        def map = [:]
        this.getUserProperties().each {
            map.put(it.name, it.value)
        }
        map
    }

    String toString(){
        firstName + " " + lastName + " <" +email +">"
    }
}
