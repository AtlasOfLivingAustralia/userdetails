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

import au.org.ala.ws.security.JwtProperties
import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest

class PropertyControllerSpec extends UserDetailsSpec implements ControllerUnitTest<PropertyController>, DataTest{

    def profileService = Mock(ProfileService)

    static doWithSpring = {
        jwtProperties(JwtProperties) {
            enabled = true
            fallbackToLegacyBehaviour = true
        }
        authorisedSystemService(UserDetailsSpec.Authorised)
    }

    private User user

    void setupSpec() {
        mockDomains(User, Role, UserRole, UserProperty)
    }

    void setup() {
        registerMarshallers()
        user = createUser()
        controller.profileService = profileService
    }

    void "Get user property"() {
        when:
        request.method = 'GET'
        params.alaId = Long.toString(user.id)
        params.name = "prop1"
        controller.getProperty()

        then:
        1 * profileService.getUserProperty(user, 'prop1') >> { [ new UserProperty(user: user, name: 'prop1', value:
                user.userProperties.find {it.name == "prop1"}.value)] }

        def deserializedJson = JSON.parse(response.text)
        deserializedJson[0].name == 'prop1'
        deserializedJson[0].value == user.userProperties.find {it.name == "prop1"}.value
    }

    void "Save user property"() {
        when:
        request.method = 'POST'
        params.alaId = Long.toString(user.id)
        params.name = "city"
        params.value = "city"
        controller.saveProperty()

        then:
        1 * profileService.saveUserProperty(user, 'city', 'city') >> { new UserProperty(user: user, name: 'city', value:'city') }

        def deserializedJson = JSON.parse(response.text)
        deserializedJson.name == 'city'
        deserializedJson.value == 'city'
    }
}
