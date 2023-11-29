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

class UrlMappings {

    static mappings = {
        name adminUserList: "/admin/user/list"(controller:'user', action: 'list')
        name adminRoleList: "/admin/role/list"(controller:'role', action: 'list')

        "/my-profile/"(controller: 'profile')
        "/my-profile"(controller: 'profile')
        "/myprofile/"(controller: 'profile')
        "/myprofile"(controller: 'profile')
        "/profile/$action?"(controller: 'profile')
        "/apiKey"(controller:'apiKey', action: 'index')

        "/external/flickr"(controller: 'externalSite', action: 'flickr')
        "/ws/flickr"(controller: 'externalSite', action: 'flickr')
        "/ws/getUserStats"(controller:'externalSite', action: 'getUserStats')
        "/admin/userRole/list"(controller:'userRole', action:'list', format:'html') // prevent link generator using the /ws endpoint by default
        "/ws/admin/userRole/list"(controller:'userRole', action:'list', format:'json')
        "/ws/registration/states(.$format)?"(controller: 'registration', action: 'states', format: 'json')
        "/ws/registration/countries(.$format)?"(controller: 'registration', action: 'countries', format: 'json')

        "/registration/$action?/$id?"(controller: 'registration')

        "/registration/activateAccount/$userId/$authKey"(controller:'registration', action: 'activateAccount')
        "/registration/activateAccount/$userId"(controller:'registration', action: 'activateAccount')
        "/registration/resetPassword/$userId/$authKey"(controller:'registration', action: 'passwordReset')

        "/registration/forgottenPassword"(controller:'registration', action: 'forgottenPassword')

        "/userDetails/byRole"(controller: 'userDetails', action: 'byRole')
        "/userDetails/search"(controller: 'userDetails', action: 'search')
        "/userDetails/getUserDetails"(controller:'userDetails', action: 'getUserDetails')
        "/userDetails/getUserList"(controller:'userDetails', action: 'getUserList')
        "/userDetails/getUserListFull"(controller:'userDetails', action: 'getUserListFull')
        "/userDetails/getUserListWithIds"(controller:'userDetails', action: 'getUserListWithIds')
        "/userDetails/getUserDetailsFromIdList"(controller:'userDetails', action: 'getUserDetailsFromIdList')

        "/property/getProperty"(controller: 'property', action: 'getProperty')
        "/property/saveProperty"(controller: 'property', action: 'saveProperty')

        "/myprofile"(controller:'profile', action: 'index')
        "/simpleCaptcha/captcha"(controller:'simpleCaptcha', action:'captcha')
        "/simpleCaptcha/*"(controller:'simpleCaptcha')

        "/admin"(controller:'admin', action: 'index')
        "/admin/"(controller:'admin', action: 'index')

//        "/admin/$controller"()
//        "/admin/$controller/$action?"()
        "/admin/$controller/$action?/$id?"()

        "/admin/$controller/$action\\?"()



        "/ws/token"(controller: 'apiKey', action: 'jwt')
        "/ws/jwt"(controller: 'apiKey', action: 'jwt')
        "/ws/apikey/$apiKey"(controller: 'apiKey', action: 'validate')

        "/logout/logout"(controller: "logout", action: 'logout')
        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
