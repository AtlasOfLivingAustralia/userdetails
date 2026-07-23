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

import au.org.ala.users.IUserProperty
import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.beans.factory.annotation.Autowired

import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path('/ws')
class ExternalSiteController {

    static allowedMethods = [flickr: "GET", getUserStats: "GET"]

    @Autowired
    IUserService userService

    def index() {}

    @Operation(
            method = "GET",
            tags = "users",
            summary = "Get list of flickr users",
            operationId = "flickr",
            description = "Lists all flickr profiles known to the application, including their ala id, flickr id, username and their flickr URL",
            parameters = [],
            responses = [
                    @ApiResponse(
                            description = "Successful get flickr users",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(schema = @Schema(implementation = Map))
                                    )
                            ]
                    )
            ]
    )
    @Path("flickr")
    @Produces("application/json")
    def flickr() {

        def flickrIds = userService.searchProperty(null, "flickrId")
        render(contentType: "application/json") {
            flickrUsers(flickrIds) { IUserProperty flickrId ->
                id flickrId.owner.id.toString()
                externalId flickrId.value
                externalUsername flickrId.owner.additionalAttributes.find { it.name == 'flickrUsername' }?.value
                externalUrl 'http://www.flickr.com/photos/' + flickrId.value
            }
        }
    }

    @Operation(
            method = "GET",
            tags = "users",
            summary = "Get total count of users in the system",
            operationId = "getUserStats",
            description = "Gets a count of all users in the system, excluding locked and non-activated accounts.  In addition it also provides a count of users from one year ago.",
            parameters = [
            // TODO Locale as a param
            ],
            responses = [
                    @ApiResponse(
                            description = "Successful retrieved user counts",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(
                                                    implementation = Map
                                            )
                                    )
                            ]
                    )
            ]
    )
    @Path("getUserStats")
    @Produces("application/json")
    def getUserStats() {
        def stats = userService.getUsersCounts(request.locale)
        render(stats as JSON, contentType: "application/json")  // getUsersCounts is cached
    }
}
