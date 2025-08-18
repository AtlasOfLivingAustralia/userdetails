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

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import grails.converters.JSON
import grails.plugin.cache.Cacheable
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.util.Environment
import grails.web.servlet.mvc.GrailsParameterMap
import org.apache.http.HttpStatus
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.springframework.beans.factory.annotation.Value

@Transactional
class UserService {

    def emailService
    def passwordService
    def authService
    def grailsApplication
    def locationService
    def messageSource
    def webService

    @Value('${attributes.affiliations.enabled:false}')
    boolean affiliationsEnabled = false

    def updateUser(User user, GrailsParameterMap params) {
        def emailRecipients = [user.email]
        if (params.email != user.email) {
            emailRecipients << params.email
        }
        try {
            user.setProperties(params)
            user.activated = true
            user.locked = false
            user.save(failOnError: true, flush:true)
            updateProperties(user, params)
            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            true
        } catch (Exception e){
            log.error(e.getMessage(), e)
            false
        }
    }

    def disableUser(User user) {
        try {
            user.activated = false
            user.save(failOnError: true, flush: true)
            Map resp = webService.post("${grailsApplication.config.getProperty('alerts.url')}/api/alerts/user/${user.id}/unsubscribe", [:])
            if (resp.statusCode != HttpStatus.SC_OK) {
                log.error("Alerts returned ${resp} when trying to disable the user's alerts. " +
                        "The user has been disabled, but their alerts are still active.")
            }
            true
        } catch (Exception e){
            log.error(e.getMessage(), e)
            false
        }
    }

    @Transactional(readOnly = true)
    boolean isActive(String email) {
        def user = User.findByEmailOrUserName(email?.toLowerCase(), email?.toLowerCase())
        return user?.activated ?: false
    }

    @Transactional(readOnly = true)
    boolean isLocked(String email) {
        def user = User.findByEmailOrUserName(email?.toLowerCase(), email?.toLowerCase())
        return user?.locked ?: false
    }

    @Transactional(readOnly = true)
    boolean isEmailRegistered(String email) {
        return User.findByEmailOrUserName(email?.toLowerCase(), email?.toLowerCase()) != null
    }

    @Transactional(readOnly = true)
    boolean isEmailInUse(String newEmail, User user) {
        def userByEmail = User.findByEmailOrUserName(newEmail?.toLowerCase(), newEmail?.toLowerCase())
        if (userByEmail == null) {
            return false
        } else {
            return user.userName != userByEmail.userName
        }
    }

    @Transactional
    def activateAccount(User user) {
        Map resp = webService.post("${grailsApplication.config.getProperty('alerts.url')}/api/alerts/user/createAlerts", [:], [userId: user.id, email: user.email, firstName: user.firstName, lastName: user.lastName])
        if (resp.statusCode == HttpStatus.SC_CREATED) {
            emailService.sendAccountActivationSuccess(user, resp.resp)
        } else if (resp.statusCode != HttpStatus.SC_OK) {
            log.error("Alerts returned ${resp} when trying to create user alerts for " + user.id + " with email: " + user.email)
        }

        user.activated = true
        user.save(flush:true)
    }

    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {

        def results = new BulkUserLoadResults()

        if (!stream) {
            results.message = "No data specified!"
            return results
        }

        int lineNumber = 0
        int expectedColumns = 4

        def roleUser = Role.findByRole("ROLE_USER")

        stream.eachCsvLine { tokens ->
            // email_address,first_name,surname,roles
            if (++lineNumber == 1 && firstRowContainsFieldNames) {
                // ignore...
            } else {

                if (tokens.size() == 1 && !tokens[0]?.trim()?.length()) {
                    return // skip empty lines
                }

                if (tokens.size() != expectedColumns) {
                    results.failedRecords << [lineNumber: lineNumber, tokens: tokens, reason: "Incorrect number of columns - expected ${expectedColumns}, got ${tokens.size()}"]
                    return
                }

                def emailAddress = tokens[0]
                // Check to see if this email address is already in use...
                def userInstance = User.findByEmail(emailAddress)
                def isNewUser = true

                def existingRoles = [] as Set
                if (userInstance) {
                    isNewUser = false
                    // keep track of their current roles
                    existingRoles.addAll(UserRole.findAllByUser(userInstance)*.role.collect { GrailsHibernateUtil.unwrapIfProxy(it) })
                } else {
                    userInstance = new User(email: emailAddress, userName: emailAddress, firstName: tokens[1], lastName: tokens[2])
                    userInstance.activated = true
                    userInstance.locked = false
                }

                // Now add roles
                def roles = []
                if (!existingRoles.contains(roleUser)) {
                    roles << roleUser
                }
                if (tokens[3]?.trim()) {
                    def roleArray = tokens[3].trim()?.split(" ")
                    for (String roleName : roleArray) {
                        def role = Role.findByRole(roleName)
                        if (!role) {
                            results.failedRecords << [lineNumber: lineNumber, tokens: tokens, reason: "Specified role '${roleName} does not exist"]
                            return
                        } else {
                            if (!roles.contains(role) && !existingRoles.contains(role)) {
                                roles << role
                            }
                        }
                    }
                }

                String password = null

                if (isNewUser) {
                    userInstance.save(failOnError: true)
                    password = passwordService.generatePassword(userInstance)
                    results.userAccountsCreated++
                }

                roles?.each { role ->
                    def userRole = new UserRole(user: userInstance, role: role)
                    userRole.save(failOnError: true)
                }

                if (!isNewUser) {
                    results.warnings << [lineNumber: lineNumber, tokens: tokens, reason: "Email address already exists in database. Added roles ${roles}"]
                } else {
                    // User Properties
                    def userProps = [:]

                    userProps['affiliation'] = affiliation ?: 'disinclinedToAcquiesce'
                    userProps['bulkCreatedOn'] = new Date().format("yyyy-MM-dd HH:mm:ss")
                    setUserPropertiesFromMap(userInstance, userProps)

                    // Now send a temporary password to the user...
                    try {
                        resetAndSendTemporaryPassword(userInstance, emailSubject, emailTitle, emailBody, password)
                        passwordService
                    } catch (PasswordResetFailedException ex) {
                        // Catching the checked exception should prevent the transaction from failing
                        log.error("Failed to send temporary password via email!", ex)
                        results.warnings << [lineNumber: lineNumber, tokens: tokens, reason: "Failed to send password reset email. Check mail configuration"]
                    }
                }


            }
        }

        results.success = true

        return results
    }

    private setUserPropertiesFromMap(User user, Map properties) {

        properties.keySet().each { String propName ->
            def propValue = properties[propName] ?: ''
            setUserProperty(user, propName, propValue)
        }

    }

    private setUserProperty(User user, String propName, String propValue) {
        def existingProperty = UserProperty.findByUserAndName(user, propName)
        if (existingProperty) {
            existingProperty.value = propValue
            existingProperty.save()
        } else {
            def newProperty = new UserProperty(user: user, name: propName, value: propValue)
            newProperty.save(failOnError: true)
        }
    }

    User registerUser(GrailsParameterMap params) throws Exception {

        //does a user with the supplied email address exist
        def user = new User(params)
        user.userName = params.email
        user.activated = false
        user.locked = false
        user.tempAuthKey = UUID.randomUUID().toString()
        def createdUser = user.save(flush: true, failOnError: true)
        updateProperties(createdUser, params)

        //add a role of user
        def roleUser = Role.findByRole("ROLE_USER")
        new UserRole(user:user, role:roleUser).save(flush:true, failOnError: true)

        log.info("Newly created user: " + createdUser.id)
        createdUser
    }

    def updateProperties(User user, GrailsParameterMap params) {
        ['city', 'organisation', 'state', 'country'].each { propName ->
            setUserProperty(user, propName, params.get(propName, ''))
        }
        if (affiliationsEnabled) {
            setUserProperty(user, 'affiliation', params.get('affiliation', ''))
        }
    }

    def addUserRole(User user, String roleString) {

        def role = Role.findByRole(roleString)

        def userRole = UserRole.findByUserAndRole(user, role)

        if(!userRole) {
            new UserRole(user:user, role:role).save(flush:true, failOnError: true)
        }
    }

    def deleteUser(User user) {

        if (user) {
            // First need to delete any user properties
            def userProperties = UserProperty.findAllByUser(user)
            userProperties.each { userProp ->
                userProp.delete()
            }
            // Then delete any roles
            def userRoles = UserRole.findAllByUser(user)
            userRoles.each { userRole ->
                userRole.delete()
            }

            // Delete password
            def passwords = Password.findAllByUser(user)
            passwords.each { password ->
                password.delete()
            }

            // and finally delete the use object
            user.delete()
        }

    }

    def resetAndSendTemporaryPassword(User user, String emailSubject, String emailTitle, String emailBody, String password = null) throws PasswordResetFailedException {
        if (user) {
            //set the temp auth key
            user.tempAuthKey = UUID.randomUUID().toString()
            user.save(flush: true)
            //send the email
            emailService.sendPasswordReset(user, user.tempAuthKey, emailSubject, emailTitle, emailBody, password)
        }
    }

    def clearTempAuthKey(User user) {
        if (user) {
            //set the temp auth key
            user.tempAuthKey = null
            user.save(flush: true)
        }
    }

    /**
     * This service method returns the User object for the current user.
     */
    @Transactional(readOnly = true)
    User getCurrentUser() {

        def userId = authService.getUserId()
        if (userId == null) {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        User user = null
        if(userId.toString().isLong()){
            user = User.get(userId.toLong())
            if (user == null && Environment.current != Environment.PRODUCTION) {
                // try looking up by email, as this may be a dev session, and the id's might not line up because this service is talking to the local database
                def email = authService.getEmail()
                if (email) {
                    user = User.findByEmail(email)
                }
            }
        } else {
            user = User.findByEmail(authService.getEmail())
        }

        return user
    }

    @NotTransactional
    String getResetPasswordUrl(User user) {
        if(user.tempAuthKey){
            emailService.getServerUrl() + "resetPassword/" +  user.id +  "/"  + user.tempAuthKey
        }
    }

    @Transactional(readOnly = true)
    def findUsersForExport(List usersInRoles, includeInactive) {
        def roles = usersInRoles? Role.findAllByRoleInList(usersInRoles) : []
        def criteria = User.createCriteria()
        def results
            results = criteria.listDistinct {
                and {
                    if(roles) {
                        userRoles {
                            'in'('role', roles)
                        }
                    }
                    if(!includeInactive) {
                        eq('activated', true)
                    }
                }
            }
        results
    }

    /**
     * Calculate the number of active users (not locked and is activated), as well as the number
     * of active users 1 year ago (for comparison).
     *
     * @return Map jsonMap
     */
    @Cacheable('dailyCache')
    @Transactional(readOnly = true)
    Map getUsersCounts(Locale locale) {
        Map jsonMap = [description: messageSource.getMessage("getUserCounts.description", null, locale?:Locale.default)]
        jsonMap.totalUsers = User.countByLockedAndActivated(false, true)
        // calculate number of users 1 year ago
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1) // minus 1 year
        Date oneYearAgoDate = cal.getTime()
        jsonMap.totalUsersOneYearAgo = User.countByLockedAndActivatedAndDateCreatedLessThan(false, true, oneYearAgoDate)
        log.debug "jsonMap = ${jsonMap as JSON}"
        jsonMap
    }

    List<String[]> countByProfileAttribute(String s, Date startDate, Date endDate, Locale locale) {
        def results = UserProperty.withCriteria {
            if (startDate) {
                user {
                    gt 'lastLogin', startDate
                }
            }
            if (endDate) {
                user {
                    lt 'lastLogin', endDate
                }
            }
            eq 'name', s

            projections {
                groupProperty "value"
                count 'name', 'count'
            }
            order('count')
        }
        def affiliations = locationService.affiliationSurvey(locale)
        return results.collect {
            [affiliations[it[0]] ?: it[0], it[1].toString()].toArray(new String[0])
        }
    }
}
