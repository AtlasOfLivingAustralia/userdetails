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


import au.org.ala.userdetails.marshaller.UserMarshaller
import au.org.ala.web.UserDetails
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY

@Path("userDetails")
class UserDetailsController {

    static allowedMethods = [getUserDetails: "POST", getUserList: "POST", getUserListWithIds: "POST", getUserListFull: "POST", getUserDetailsFromIdList: "POST", byRole: "GET", search: "GET"]

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {}

    @Operation(
            method = "GET",
            tags = "users",
            summary = "Search users",
            operationId = "search",
            description = "Search for users by username, email or display name.  Required scopes: 'users/read'.",
            parameters = [
                @Parameter(
                        name = "q",
                        in = QUERY,
                        description = "Search query for the user's username, email or display name",
                        required = true
                ),
                @Parameter(
                        name = "max",
                        in = QUERY,
                        description = "Maximum number of results to return",
                        required = false
                )
            ],
            responses = [
                    @ApiResponse(
                        description = "Search results",
                        responseCode = "200",
                        content = [
                                @Content(
                                        mediaType = "application/json",
                                        array = @ArraySchema(schema = @Schema(implementation = UserDetails))
                                )
                        ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Path("search")
    @Produces("application/json")
    def search() {
        def q = params['q']
        if (!q) {
            render(status: 400, text: 'q parameter is required')
            return
        }
        def streamer = new ResultStreamer(response: response, jsonConfig: UserMarshaller.WITH_PROPERTIES_CONFIG)
        userService.findScrollableUsersByUserName(params, streamer)
    }

    @Operation(
            method = "GET",
            tags = "users",
            summary = "Get Users by Role",
            description = "Get Users by Role.  Required scopes: 'users/read'.",
            parameters = [
                    @Parameter(
                            name = "role",
                            in = QUERY,
                            description = "The role to get users for",
                            required = true
                    ),
                    @Parameter(
                            name = "id",
                            in = QUERY,
                            description = "A list of user ids or usernames to limit the results to",
                            required = false
                    ),
                    @Parameter(
                            name = "includeProps",
                            in = QUERY,
                            description = "Whether to include additional user properties or not",
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Search results",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(arraySchema = @Schema(implementation = UserDetails))
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Path("byRole")
    @Produces("application/json")
    def byRole() {
        def roleName = params.get('role', 'ROLE_USER')
        def includeProps = params.boolean('includeProps', false)

        def streamer = new ResultStreamer(response: response, jsonConfig: includeProps ? UserMarshaller.WITH_PROPERTIES_CONFIG : 'default')

        def role = userService.listRoles().find { it.role == roleName}

        if (!role) {
            response.sendError(404, "Role not found")
            return
        }

        userService.findScrollableUsersByIdsAndRole(params, streamer)
    }

    @Operation(
            method = "POST",
            tags = "users",
            summary = "Get User Details",
            description = "Get User Details.  Required scopes: 'users/read'.",
            parameters = [
                    @Parameter(
                            name = "userName",
                            in = QUERY,
                            description = "The username of the user",
                            required = true
                    ),
                    @Parameter(
                            name = "includeProps",
                            in = QUERY,
                            description = "Whether to include additional user properties or not",
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "User Details",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = UserDetails)
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Path('getUserDetails')
    @Produces('application/json')
    def getUserDetails() {

        def user
        String userName = params.userName as String
        def includeProps = params.getBoolean('includeProps', false)

        if (userName) {
            if (userName.isLong()) {
                user = userService.getUserById(userName)
            } else {
                user = userService.findByUserNameOrEmail(params)
            }
        } else {
            render status:400, text: "Missing parameter: userName"
            return
        }

        if (user == null) {
            render status:404, text: "No user found for: ${userName}"
        } else {
            String jsonConfig = includeProps ? UserMarshaller.WITH_PROPERTIES_CONFIG : null
            try {
                JSON.use(jsonConfig)
                render user as JSON
            }
            finally {
                JSON.use(null) // resets to default config
            }
        }

    }

    @Operation(
            method = "POST",
            tags = "users",
            summary = "Get User List",
            description = "Get a list of all users.  Required scopes: 'users/read'.",
            deprecated = true,
            responses = [
                    @ApiResponse(
                            description = "Returns a map of user's email addresses to their names",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = Map) // TODO Annotation processor does not like generic parameters on types, create a type that realizes Map<String,String>
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Deprecated
    @Path("getUserList")
    @Produces("application/json")
    def getUserList() {
        def users = userService.listNamesAndEmails() // UserRecord.findNameAndEmailWhereEmailIsNotNull()
        def map = users.collectEntries { [(it[0].toLowerCase()): "${it[1]?:""} ${it[2]?:""}"]}
        render(map as JSON, contentType: "application/json")
    }

    @Operation(
            method = "POST",
            tags = "users",
            summary = "Get User List With Ids",
            description = "Get a list of all users by their user id.  Required scopes: 'users/read'.",
            deprecated = true,
            responses = [
                    @ApiResponse(
                            description = "Returns a map of user's ids to their names",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = Map)
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Deprecated
    @Path("getUserListWithIds")
    @Produces("application/json")
    def getUserListWithIds() {
        def users = userService.listIdsAndNames() // UserRecord.findIdFirstAndLastName()
        def map = users.collectEntries { [(it[0]), "${it[1]?:""} ${it[2]?:""}"] }
        render(map as JSON, contentType: "application/json")
    }

    @Operation(
            method = "POST",
            tags = "users",
            summary = "Get User List With Ids",
            description = "Get a list of all users by their user id.  Required scopes: 'users/read'.",
            deprecated = true,
            responses = [
                    @ApiResponse(
                            description = "User Details",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(schema = @Schema(implementation = UserDetails))
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Deprecated
    @Path("getUserListFull")
    @Produces("application/json")
    def getUserListFull() {
//        UserRecord.findUserDetails()
        def details = userService.listUserDetails().collect {
            [id: it[0], firstName: it[1]?:"", lastName: it[2]?:"", userName: it[3]?:"", email: it[4]?:""]
        }
        render(details as JSON, contentType: "application/json")
    }

    @Operation(
            method = "POST",
            tags = "users",
            operationId = "getUserDetailsFromIdList",
            summary = "Get User Details by id list",
            description = "Get a list of user details for a list of user ids.  Required scopes: 'users/read'.",
            requestBody = @RequestBody(
                    description = "The list of user ids to request and whether to include extended properties",
                    required = true,
                    content = @Content(
                            mediaType = 'application/json',
                            schema = @Schema(implementation = GetUserDetailsFromIdListRequest)
                    )
            ),
            responses = [
                    @ApiResponse(
                            description = "User Details",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = GetUserDetailsFromIdListResponse)
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Path("getUserDetailsFromIdList")
    @Consumes("application/json")
    @Produces("application/json")
    def getUserDetailsFromIdList() {

        def req = request.JSON
        def includeProps = req?.includeProps ?: params.getBoolean('includeProps', false)

        if (req && req.userIds) {

            try {
                List idList = req.userIds

                def results = userService.getUserDetailsFromIdList(idList)
                String jsonConfig = includeProps ? UserMarshaller.WITH_PROPERTIES_CONFIG : null
                try {

                    JSON.use(jsonConfig)

                    def resultsMap = [users:[:], invalidIds:[], success: true]
                    results.each { user ->
                        resultsMap.users[user.userId] = user
                    }

                    idList.each {
                        if (!resultsMap.users[it.toString()]) {
                            resultsMap.invalidIds << it
                        }
                    }

                    render resultsMap as JSON
                }
                finally {
                    JSON.use(null) // Reset to default
                }
            } catch (Exception ex) {
                render([success: false, message: "Exception: ${ex.toString()}"] as JSON, contentType: "application/json")
            }
        } else {
            render([success: false, message: "Body must contain JSON map payload with 'userIds' key that contains a list of user ids"] as JSON, contentType: "application/json")
        }

    }

    // classes used for the OpenAPI definition generator
    @JsonIgnoreProperties('metaClass')
    static class GetUserDetailsFromIdListRequest {
        boolean includeProps = false
        List<Long> userIds = []
    }

    @JsonIgnoreProperties('metaClass')
    static class GetUserDetailsFromIdListResponse {
        boolean success = false
        Map<String,UserDetails> users
        List<Long> invalidIds
        String message
    }
}