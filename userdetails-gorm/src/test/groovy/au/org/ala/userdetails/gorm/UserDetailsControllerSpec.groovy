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

import au.org.ala.userdetails.IUserService
import au.org.ala.userdetails.UserDetailsController
import au.org.ala.users.IUser
import au.org.ala.users.UserRecord
import au.org.ala.ws.security.JwtProperties
import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.pac4j.core.config.Config

/**
 * Tests the UserDetailsController and the filtering behaviour associated with it.
 */
class UserDetailsControllerSpec extends UserDetailsSpec implements ControllerUnitTest<UserDetailsController>, DataTest {

    static doWithSpring = {
        jwtProperties(JwtProperties) {
            enabled = true
            fallbackToLegacyBehaviour = true
        }
        authorisedSystemService(UserDetailsSpec.Authorised)
    }

    private IUser<?> user

    void setupSpec() {
        mockDomains(Role, User, Password, UserRole, UserProperty)
    }

    void setup() {
        defineBeans {
            userService(InstanceFactoryBean, Mock(IUserService), IUserService)
        }
        registerMarshallers()
        user = createUser()
        controller.userService = new GormUserService()
    }

    void "A user can be found by user id"() {
        when:
        request.method = 'POST'
        params.userName = Long.toString(user.id)
        controller.getUserDetails()

        then:
        response.contentType == 'application/json;charset=UTF-8'

        Map deserializedJson = JSON.parse(response.text)
        deserializedJson.userId == Long.toString(user.id)
        deserializedJson.userName == user.userName

        deserializedJson.firstName == user.firstName
        deserializedJson.lastName == user.lastName
        deserializedJson.email == user.email
        deserializedJson.roles == ['ROLE_USER']
        deserializedJson.props == null

    }

    void "A user can be found by user id and the user properties can be returned"() {
        when:
        request.method = 'POST'
        params.userName = Long.toString(user.id)
        params.includeProps = true
        controller.getUserDetails()

        then:
        response.contentType == 'application/json;charset=UTF-8'

        Map deserializedJson = JSON.parse(response.text)
        deserializedJson.userId == Long.toString(user.id)
        deserializedJson.userName == user.userName

        deserializedJson.firstName == user.firstName
        deserializedJson.lastName == user.lastName
        deserializedJson.email == user.email
        deserializedJson.roles == ['ROLE_USER']
        deserializedJson.props == [prop1:'prop1', prop2:'prop2']
    }

    void "Details of a list of users can be returned"() {

        setup:
        IUser<?> user2 = createUser(2)

        when:
        request.method = 'POST'
        request.JSON = [userIds:[Long.toString(user.id), Long.toString(user2.id)]] as JSON
        controller.getUserDetailsFromIdList()

        then:
        response.contentType == 'application/json;charset=UTF-8'

        Map deserializedJson = JSON.parse(response.text)
        deserializedJson.users.size() == 2

        Map user1 = deserializedJson.users[Long.toString(user.id)]
        user1.userId == Long.toString(user.id)
        user1.userName == user.userName
        user1.firstName == user.firstName
        user1.lastName == user.lastName
        user1.email == user.email
        user1.roles == ['ROLE_USER']

        Map user2Map = deserializedJson.users[Long.toString(user2.id)]
        user2Map.userId == Long.toString(user2.id)
        user2Map.userName == user2.userName
        user2Map.firstName == user2.firstName
        user2Map.lastName == user2.lastName
        user2Map.email == user2.email
        user2Map.roles == ['ROLE_USER']

    }


}
