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
import com.opencsv.CSVWriterBuilder
import com.opencsv.RFC4180ParserBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

@PreAuthorise
class AdminController {

    def passwordService
    def emailService
    def exportService
    def profileService
    def authorisedSystemService

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {}

    def resetPasswordForUser(){
    }

    def sendPasswordResetEmail(){

        def user = userService.getUserById(params.email)
        if (user) {
            def password = passwordService.generatePassword(user)
            //email to user
            emailService.sendGeneratedPassword(user, password)
            render(view:'userPasswordResetSuccess', model:[email:params.email, password: password])
       } else {
           render(view:'resetPasswordForUser', model:[email:params.email, emailNotRecognised:true])
       }
    }

    def bulkUploadUsers() {
    }

    def exportUsers() {
        def secondaryFields = profileService.allAvailableProperties
        String extraFields =  secondaryFields.join(",")

        render(view: 'exportUsers',
                model: [roles        : userService.listRoles(),
                        primaryFields: grailsApplication.config.getProperty('admin.export.csv.primary.fields'),
                        extraFields  : extraFields])
    }

    def downloadUsersCsvFile() {
        if (authorisedSystemService.isAuthorisedSystem(request)) {
            //selectedRoles data type will be different depending on whether one or more option were selected
            def roleList = {
                if (!params.selectedRoles) {
                    return []
                } else if (params.selectedRoles instanceof String) {
                    return [params.selectedRoles]
                } else {
                    return params.selectedRoles as List
                }
            }.call()

            //1. Get data based on user inputs
            def userList = userService.findUsersForExport(roleList, params?.includeInactiveUsers)

            //2. Then prepare the format options
            String primaryFieldsProperty = grailsApplication.config.getProperty('admin.export.csv.primary.fields')
            def primaryFields = primaryFieldsProperty ? primaryFieldsProperty.split(',').collect { it as String } : []
            def fields = primaryFields

            def formatters = [:]
            if (params.includeExtraFields) {
                def secondaryFields = profileService.allAvailableProperties
                fields.addAll(secondaryFields)
                secondaryFields.each {
                    formatters[it] = { domain, value ->
                        String fieldName = it
                        domain.additionalAttributes.find {
                            it.name == fieldName
                        }?.value
                    }
                }
            }

            if (params.includeRoles) {
                String roleFieldName = 'roles'
                formatters[roleFieldName] = { IUser domain, value ->
                    def result = ""
                    domain.roles.each {
                        result += it.roleObject.role + " "
                    }
                    result
                }
                fields.add(roleFieldName)
            }

            log.debug("Export fields ${fields}")

            String fileName = "users-" + new Date().format("YYYYMMdd-HHmm")
            //3. And finally generate and send file to browser
            exportService.export("csv", response, fileName, "csv", userList, fields, [:], formatters, [:])
        } else {
            response.sendError(403)
        }
    }

    def loadUsersCSV() {
        if(request instanceof MultipartHttpServletRequest) {
            MultipartFile f = ((MultipartHttpServletRequest) request).getFile('userList')
            if (f && f.size > 0) {
                def allowedMimeTypes = ['text/plain', 'text/csv']
                if (!allowedMimeTypes.contains(f.getContentType())) {
                    flash.message = "The file must be one of: ${allowedMimeTypes}. Submitted file is of type ${f.getContentType()}"
                    redirect(action:"bulkUploadUsers")
                    return
                }

                def firstRow = (boolean) params.firstRowHasFieldNames
                def affiliation = params.affiliation as String
                def subject = params.emailSubject as String
                def title = params.emailTitle as String
                def body = params.emailBody as String

                def results = userService.bulkRegisterUsersFromFile(f.inputStream, firstRow, affiliation, subject, title, body)
                render(view:'loadUsersResults', model:[results: results])
                return
            } else {
                flash.message = "You must select a file to upload!"
            }
        }
        redirect(action:"bulkUploadUsers")
    }

    def surveyResults() {
        def results = userService.countByProfileAttribute('affiliation', null, request.locale)
        respondWithCsv(results, "user-survey-${new Date()}.csv")
    }

    def emailListForm() {

    }

    def emailList() {
        def startDate = params.date('start_date')
        def endDate = params.date('end_date')
        def results = userService.emailList(startDate, endDate)
        respondWithCsv(results, "email-list-$startDate-to-${endDate}.csv")
    }

    private def respondWithCsv(List<String[]> results, String filename) {
        def csvWriter = new CSVWriterBuilder(response.writer)
                .withParser(new RFC4180ParserBuilder().build())
                .build()
        response.status = 200
        response.contentType = 'text/csv'
        response.setHeader('Content-Disposition', "attachment; filename=$filename")
        csvWriter.writeAll(results)
        csvWriter.flush()
    }

}
