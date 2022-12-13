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
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.users.UserRoleRecord
import au.org.ala.userdetails.marshaller.UserMarshaller
import grails.converters.JSON
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Base class for user details tests.  Sets up some domain objects to support testing.
 */

abstract class UserDetailsSpec extends Specification {


    // These classes are a workaround for the difficulty in injecting Spock mocks into the filter class in unit tests.
    public static class UnAuthorised extends AuthorisedSystemService {
        @Override
        def isAuthorisedSystem(HttpServletRequest request) {
            return false
        }
    }

    public static class Authorised extends AuthorisedSystemService {
        @Override
        def isAuthorisedSystem(HttpServletRequest request) {
            return true
        }
    }
    void registerMarshallers() {
        // There are issues where tests fail the first time due to the custom marshallers not being registered.
        // This is a workaround to ensure the defaults are set.
        ConvertersConfigurationHolder.setDefaultConfiguration(JSON, ConvertersConfigurationHolder.getConverterConfiguration(JSON))

        new UserMarshaller().register()
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())
    }

    void cleanup() {
        UserRecord.deleteAll()
        RoleRecord.deleteAll()
        UserRoleRecord.deleteAll()
        UserPropertyRecord.deleteAll()
    }


    protected UserRecord createUser(String tempAuthKey = "") {
        RoleRecord role = RoleRecord.findOrCreateWhere(role: 'ROLE_USER', description:"Everyone has this role")
        role.save(failOnError: true, flush: true)


        UserRecord user = new UserRecord(firstName: 'test first', lastName: 'test last', email: 'test@test.com', userName:'test@test.com', activated: true, locked: false, tempAuthKey: tempAuthKey)
        user.save(failOnError: true, flush: true)

        UserRoleRecord userRole = new UserRoleRecord(user:user, role:role)
        userRole.save(failOnError: true, flush: true)


        [prop1:'prop1', prop2:'prop2'].each { k,v ->
            UserPropertyRecord prop = new UserPropertyRecord(user:user, name:k, value:v)
            prop.save(failOnError: true, flush: true)
        }

        UserRecord.findById(user.id)
    }
}
