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

import au.org.ala.auth.PreAuthorise
import au.org.ala.users.IUser
import au.org.ala.users.IUserProperty
import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import javax.ws.rs.Path
import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY
import static io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER

@Path("property")
class PropertyController extends BaseController {

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def profileService

    static allowedMethods = [getProperty: "GET", saveProperty: "POST"]

    def index() {}

    /**
     * get a property value for a user
     * @return
     */
    @Operation(
            method = "GET",
            tags = "properties",
            summary = "Get Property",
            operationId = "getProperty",
            description = "Get a property value for a user.  Required scopes: 'users/read'.",
            parameters = [
                    @Parameter(
                            name = "alaId",
                            in = QUERY,
                            description = "The user's ALA ID",
                            required = true
                    ),
                    @Parameter(
                            name = "name",
                            in = QUERY,
                            description = "The name of the property to get",
                            required = true
                    ),
                    @Parameter(
                            name = 'Accept',
                            in = HEADER,
                            description = "Must be application/json",
                            required = true,
                            schema = @Schema(allowableValues = ['application/json'])
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Successful get property request",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(schema = @Schema(implementation = IUserProperty))
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/read'])]
    )
    @Path("getProperty")
    @Produces("application/json")
    @PreAuthorise(requiredScope = 'users/read', requiredRole = '')
    def getProperty() {
        String name = params.name
        String alaId = params.alaId
        if (!name || !alaId) {
            badRequest "name and alaId must be provided";
        } else {
            IUser user = userService.getUserById(alaId);
            List<IUserProperty> props
            if (user) {
                props = profileService.getUserProperty(user, name)
                render text: props as JSON, contentType: 'application/json'
            } else {
                notFound "Could not find user for id: ${alaId}";
            }
        }
    }

    /**
     * save a property value to a user
     * @return
     */
    @Operation(
            method = "POST",
            tags = "properties",
            summary = "Save a Property",
            operationId = "saveProperty",
            description = "Saves a property value for a user.  Required scopes: 'users/write'.",
            parameters = [
                    @Parameter(
                            name = "alaId",
                            in = QUERY,
                            description = "The user's ALA ID",
                            required = true
                    ),
                    @Parameter(
                            name = "name",
                            in = QUERY,
                            description = "The name of the property to set",
                            required = true
                    ),
                    @Parameter(
                            name = "value",
                            in = QUERY,
                            description = "The value of the property to set.",
                            required = false
                    ),
                    @Parameter(
                            name = 'Accept',
                            in = HEADER,
                            description = "Must be application/json",
                            required = true,
                            schema = @Schema(allowableValues = ['application/json'])
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Successful save property request",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = IUserProperty)
                                    )
                            ]
                    ),
                    @ApiResponse(
                            description = "Could not find user",
                            responseCode = "404",
                            content = [@Content(mediaType = "text/plain")]
                    ),
            ],
            security = [@SecurityRequirement(name = 'openIdConnect', scopes = ['users/write'])]
    )
    @Path("saveProperty")
    @Produces("application/json")
    @PreAuthorise(requiredScope = 'users/write', requiredRole = '')
    def saveProperty(){
        String name = params.name;
        String value = params.value;
        String alaId = params.alaId
        if (!name || !alaId) {
            badRequest "name and alaId must be provided";
        } else {
            IUser user = userService.getUserById(alaId);
            IUserProperty property
            if (user) {
                property = profileService.saveUserProperty(user, name, value);
                if (!property) {
                    saveFailed()
                } else {
                    render text: property as JSON, contentType: 'application/json'
                }
            } else {
                notFound "Could not find user for id: ${alaId}";
            }
        }

    }
}
