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

import au.org.ala.users.IUser
import groovy.transform.EqualsAndHashCode

import java.sql.Timestamp

@EqualsAndHashCode(includes = ['firstName', 'lastName', 'userName', 'email', 'lastLogin', 'tempAuthKey'])
class User implements IUser<Long>, Serializable {

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

    Set<UserRole> userRoles
    Set<UserProperty> userProperties

    static mappedBy = [
            userProperties: 'user',
            userRoles: 'user'
    ]

    static hasMany =  [
            userRoles: UserRole,
            userProperties: UserProperty
    ]

    static transients = ['roles', 'additionalAttributes']

    static mapping = {
        table 'users'
//        tablePerSubclass false

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

    String getUserId() {
        return this.id?.toString()
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

    @Override
    Set<UserRole> getRoles() {
        return userRoles
    }

    @Override
    Set<UserProperty> getAdditionalAttributes() {
        return userProperties
    }

    @Override
    def propsAsMap(){
        def map = [:]
        this.userProperties.each {
            map.put(it.name.startsWith('custom:') ? it.name.substring(7) : it.name, it.value)
        }
        map
    }

    String toString(){
        firstName + " " + lastName + " <" +email +">"
    }
}
