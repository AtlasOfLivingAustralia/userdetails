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

package au.org.ala.userdetails.gorm.marshaller

import au.org.ala.userdetails.gorm.Password
import au.org.ala.userdetails.gorm.Role
import au.org.ala.userdetails.gorm.User
import au.org.ala.userdetails.gorm.UserDetailsSpec
import au.org.ala.userdetails.gorm.UserProperty
import au.org.ala.userdetails.gorm.UserRole
import au.org.ala.userdetails.marshaller.UserMarshaller
import au.org.ala.users.IUser
import grails.converters.JSON
import grails.testing.gorm.DataTest

/**
 * Tests the UserMarshaller
 */
class UserMarshallerSpec extends UserDetailsSpec implements DataTest {

    private IUser<Long> user

    void setupSpec() {
        mockDomains(Role, User, Password, UserRole, UserProperty)
    }

    void setup() {

        user = createUser()
    }

    void "JSON serialization of the User object should be output in a specific format"() {
        when:
        registerMarshallers()
        IUser<Long> user = createUser()
        def expectedSerializedProperies = ['userId', 'userName', 'firstName', 'lastName', 'email', 'roles', 'activated', 'locked', 'displayName']
        JSON json = user as JSON
        Map deserializedJson = JSON.parse(json.toString())

        then:
        deserializedJson.size() == expectedSerializedProperies.size()
        deserializedJson.userId == Long.toString(user.id)
        deserializedJson.userName == user.userName

        deserializedJson.firstName == user.firstName
        deserializedJson.lastName == user.lastName
        deserializedJson.email == user.email
        deserializedJson.activated == user.activated
        deserializedJson.locked == user.locked
        deserializedJson.roles == ['ROLE_USER']

    }

    void "There should be a named JSON configuration that allows the properties of a User to be included in the serialized user data"() {
        when:
        JSON json
        IUser<Long> user = createUser()
        registerMarshallers()

        def expectedSerializedProperties = ['userId', 'userName', 'firstName', 'lastName', 'email', 'roles', 'activated', 'locked', 'props', 'displayName']

        JSON.use(UserMarshaller.WITH_PROPERTIES_CONFIG) {
            json = user as JSON
        }
        Map deserializedJson = JSON.parse(json.toString())

        then:
        deserializedJson.size() == expectedSerializedProperties.size()
        deserializedJson.userId == Long.toString(user.id)
        deserializedJson.userName == user.userName

        deserializedJson.firstName == user.firstName
        deserializedJson.lastName == user.lastName
        deserializedJson.email == user.email
        deserializedJson.activated == user.activated
        deserializedJson.locked == user.locked
        deserializedJson.roles == ['ROLE_USER']
        deserializedJson.props == [prop1:'prop1', prop2:'prop2']
    }



}
