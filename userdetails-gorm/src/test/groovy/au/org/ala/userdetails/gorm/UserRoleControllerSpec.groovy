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
import au.org.ala.userdetails.UserRoleController
import au.org.ala.users.IUser
import au.org.ala.users.UserRecord
import au.org.ala.ws.security.JwtProperties
import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.pac4j.core.config.Config
import au.org.ala.ws.security.client.AlaAuthClient

/**
 * Specification for the UserRoleController
 */
class UserRoleControllerSpec extends UserDetailsSpec implements ControllerUnitTest<UserRoleController>, DataTest {

    private IUser<?> user

    void setupSpec() {
        mockDomains(Role, User, Password, UserRole, UserProperty)
    }

    void setup() {
        defineBeans {
            jwtProperties(JwtProperties) {
                enabled = true
                fallbackToLegacyBehaviour = true
            }
            config(InstanceFactoryBean, Stub(Config), Config)
            alaAuthClient(InstanceFactoryBean, Stub(AlaAuthClient), AlaAuthClient)
            userService(InstanceFactoryBean, Mock(IUserService), IUserService)
        }
        user = createUser()
        controller.userService = new GormUserService()
    }

    void "user role list should return a model when HTML requested"() {

        setup:
        request.addUserRole("ROLE_ADMIN")

        when:
        def model = controller.list()
        then:
        response.status == HttpStatus.SC_OK
        model.userRoleInstanceTotal == 1
    }

    void "user role web service should return valid JSON"() {

        setup:
        registerMarshallers()
        request.format = 'json'
        response.format = 'json'

        when:
        controller.list()

        then:

        response.status == HttpStatus.SC_OK
        Map deserializedJson = JSON.parse(response.text)
        deserializedJson.count == 1
        deserializedJson.users[0].userId == Long.toString(user.id)
    }
}