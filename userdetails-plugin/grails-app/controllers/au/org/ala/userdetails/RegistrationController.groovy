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

import au.org.ala.auth.UpdatePasswordCommand
import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.users.IUser
import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.service.WebService
import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.passay.RuleResult
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.validation.Errors

import javax.ws.rs.Path
import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY

/**
 * Controller that handles the interactions with general public.
 * Supports:
 *
 * 1. Account creation
 * 2. Account editing
 * 3. Password reset
 * 4. Account activation
 */
class RegistrationController {

    def simpleCaptchaService
    def emailService
    def passwordService

    @Qualifier('userService')
    IUserService userService
    def locationService
    RecaptchaClient recaptchaClient
    WebService webService
    def messageSource
    @Autowired
    JwtProperties jwtProperties

    @Value('${userdetails.features.requirePasswordForUserUpdate:true}')
    boolean requirePasswordForUserUpdate

    def index() {
        redirect(action: 'createAccount')
    }

    def createAccount() {
        render(view: 'createAccount', model: [
                passwordPolicy: passwordService.buildPasswordPolicy(), visibleMFA: false
        ])
    }

    def editAccount() {
        def user = userService.currentUser
        render(view: 'createAccount', model: [edit: true, user: user, props: user?.propsAsMap(), passwordPolicy: passwordService.buildPasswordPolicy(),
                visibleMFA: isMFAVisible(user)])
    }

    def passwordReset() {
        IUser user = userService.getUserById(params.userId)
        boolean isAuthkeyCheckRequired = grailsApplication.config.getProperty("password.requireAuthKeyCheck", Boolean)

        if (!user) {
            render(view: 'accountError', model: [msg: "User not found with ID ${params.userId}"])
        }
        if(isAuthkeyCheckRequired) {
            if (user.tempAuthKey == params.authKey) {
                //keys match, so lets reset password
                render(view: 'passwordReset', model: [user: user, authKey: params.authKey, passwordPolicy: passwordService.buildPasswordPolicy()])
            } else {
                render(view: 'authKeyExpired')
            }
        }
        else{
            render(view: 'passwordReset', model: [user: user, authKey: null, passwordPolicy: passwordService.buildPasswordPolicy()])
        }
    }

    def updatePassword(UpdatePasswordCommand cmd) {
        IUser<?> user = userService.getUserById(cmd.userId)
        if(!user) {
            log.error "Invalid User"
            render(view: 'accountError', model: [msg: "Invalid User"])
        }

        // since the email address is the user name, use the part before the @ as the username
        def username = user?.userName ?: user?.email ?: ''
        def validationResult = passwordService.validatePassword(username, cmd?.password)

        boolean isCodeRequired = grailsApplication.config.getProperty("password.requireCodeToResetPassword", Boolean)
        if(isCodeRequired && !cmd.code){
            cmd.errors.rejectValue('code', "updatePasswordCommand.code.blank.error", "Code is required to reset the password.")
        }
        buildErrorMessages(validationResult, cmd.errors)

        if (cmd.hasErrors()) {
            render(view: 'passwordReset', model: [user: user, authKey: cmd.authKey, errors:cmd.errors, passwordMatchFail: true, passwordPolicy: passwordService.buildPasswordPolicy()])
        }
        else {
            withForm {
                boolean isAuthkeyCheckRequired = grailsApplication.config.getProperty("password.requireAuthKeyCheck", Boolean)
                //update the password
                try {
                    if(!isAuthkeyCheckRequired || (isAuthkeyCheckRequired && user.tempAuthKey == cmd.authKey)) {
                        def success = passwordService.resetPassword(user, cmd.password, true, cmd.code)
                        if (isAuthkeyCheckRequired) {
                            userService.clearTempAuthKey(user)
                        }

                        if (success) {
                            redirect(uri: '/logout', params: [url: grailsLinkGenerator.link(controller: 'registration', action: 'passwordResetSuccess')])
                            log.info("Password successfully reset for user: " + user.email)
                        } else {
                            render(view: 'accountError', model: [msg: "Failed to reset password"])
                        }
                    }
                    else if(isAuthkeyCheckRequired && user.tempAuthKey != cmd.authKey){
                        log.error "Password was not reset as AUTH_KEY did not match -- ${user.tempAuthKey} vs ${cmd.authKey}"
                        render(view: 'accountError', model: [msg: "Password was not reset as AUTH_KEY did not match"])
                    }
                } catch (e) {
                    log.error("Couldn't reset password", e)
                    render(view: 'accountError', model: [msg: "Failed to reset password"])
                }
            }
                    .invalidToken {
                        redirect(action: 'duplicateSubmit', model: [msg: ""])
                    }
        }

    }

    /**
     * Used only to display the view.
     * This is required given that the view is not rendered directly by #disableAccount but rather a chain
     * of redirects:  userdetails logout, cas logout and finally this view
     */
    def accountDisabled() {
    }

    /** Displayed as a result of a password update with a duplicate form submission. */
    def duplicateSubmit() {
        [serverUrl: grailsApplication.config.getProperty('grails.serverURL') + '/myprofile']
    }

    def passwordResetSuccess() {
        [serverUrl: grailsApplication.config.getProperty('grails.serverURL') + '/myprofile']
    }

    def startPasswordReset() {
        //check for human
        def recaptchaKey = grailsApplication.config.getProperty('recaptcha.secretKey')
        if (recaptchaKey) {
            def recaptchaResponse = params['g-recaptcha-response']
            def call = recaptchaClient.verify(recaptchaKey, recaptchaResponse, request.remoteAddr)
            def response = call.execute()
            if (response.isSuccessful()) {
                def verifyResponse = response.body()
                if (!verifyResponse.success) {
                    log.warn('Recaptcha verify reported an error: {}', verifyResponse)
                    flash.message = 'There was an error with the captcha, please try again'
                    render(view: 'forgottenPassword', model: [email: params.email, captchaInvalid: true])
                    return
                }
            } else {
                //send password reset link
                render(view: 'forgottenPassword', model: [email: params.email, captchaInvalid: true])
                return
            }
        }

        log.info("Starting password reset for email address: " + params.email)
        def user = userService.getUserById(params.email)
        if (user) {
            try {
                passwordService.resetAndSendTemporaryPassword(user, null, null, null, null)
                render(view: passwordService.getPasswordResetView(), model: [user: user, passwordPolicy: passwordService.buildPasswordPolicy()])
            } catch (Exception e) {
                log.error("Problem starting password reset for email address: " + params.email)
                log.error(e.getMessage(), e)
                render(view: 'accountError', model: [msg: e.getMessage()])
            }
        } else {
            //invalid email address entered
            log.warn("email address {} is not recognised.", params.email)
            render(view: 'forgottenPassword', model: [email: params.email, emailInvalid: true])
        }

    }

    def disableAccount() {
        def user = userService.currentUser

        log.debug("Disabling account for " + user)
        if (user) {
            def success = userService.disableUser(user)

            if (success) {
                redirect(uri: '/logout', params: [url: grailsLinkGenerator.link(controller: 'registration', action: 'accountDisabled')])
            } else {
                render(view: "accountError", model: [msg: "Failed to disable user profile - unknown error"])
            }
        } else {
            render(view: "accountError", model: [msg: "The current user details could not be found"])
        }
    }

    def update() {

        def user = userService.currentUser

        log.debug("Updating account for " + user)

        if (user) {
            if (params.email != user.email) {
                // email address has changed
                if (userService.isEmailInUse(params.email)) {
                    def msg = message(code: "update.account.failure.msg", default: "Failed to update user profile - A user is already registered with the email address.")
                    render(view: "accountError", model: [msg: msg])
                    return
                }
            }

            if (requirePasswordForUserUpdate) {
                def isCorrectPassword = passwordService.checkUserPassword(user, params.confirmUserPassword)
                if (!isCorrectPassword) {
                    flash.message = 'Incorrect password. Could not update account details. Please try again.'
                    render(view: 'createAccount', model: [edit: true, user: user, props: user?.propsAsMap(), passwordPolicy: passwordService.buildPasswordPolicy(),
                                                          visibleMFA: isMFAVisible(user)])
                    return
                }
            }

            def success = userService.updateUser(user.userId, params, request.locale)

            if (success) {
                redirect(controller: 'profile')
                log.info("Account details updated for user: " + user.id + " username: " + user.userName)
            } else {
                render(view: "accountError", model: [msg: "Failed to update user profile - unknown error"])
            }
        } else {
            render(view: "accountError", model: [msg: "The current user details could not be found"])
        }
    }

    def register() {
        def paramsEmail = params?.email?.toString()
        def paramsPassword = params?.password?.toString()
        withForm {

            def recaptchaKey = grailsApplication.config.getProperty('recaptcha.secretKey')
            if (recaptchaKey) {
                def recaptchaResponse = params['g-recaptcha-response']
                def call = recaptchaClient.verify(recaptchaKey, recaptchaResponse, request.remoteAddr)
                def response = call.execute()
                if (response.isSuccessful()) {
                    def verifyResponse = response.body()
                    if (!verifyResponse.success) {
                        log.warn('Recaptcha verify reported an error: {}', verifyResponse)
                        flash.message = 'There was an error with the captcha, please try again'
                        render(view: 'createAccount', model: [edit: false, user: params, props: params, passwordPolicy: passwordService.buildPasswordPolicy(),
                                                              visibleMFA: false])
                        return
                    }
                } else {
                    log.warn("error from recaptcha {}", response)
                    flash.message = 'There was an error with the captcha, please try again'
                    render(view: 'createAccount', model: [edit: false, user: params, props: params, passwordPolicy: passwordService.buildPasswordPolicy(),
                                                          visibleMFA: false])
                    return
                }
            }

            def isEmailInUse = userService.isEmailInUse(paramsEmail)

            //create user account...
            if (!paramsEmail || isEmailInUse) {
                def user = null
                if (isEmailInUse) {
                    user = userService.getUserByEmail(paramsEmail)
                }
                def inactiveUser = user?.activated ?: false
                def lockedUser = user?.locked ?: false
                render(view: 'createAccount', model: [edit: false, user: params, props: params, alreadyRegistered: true, inactiveUser: inactiveUser,
                                                      lockedUser: lockedUser, passwordPolicy: passwordService.buildPasswordPolicy(),visibleMFA: false])
            } else {

                def passwordValidation = passwordService.validatePassword(paramsEmail, paramsPassword)
                if (!passwordValidation.valid) {
                    log.warn("The password for user name '${paramsEmail}' did not meet the validation criteria '${passwordValidation}'")
                    flash.message = "The selected password does not meet the password policy. Please try again with a different password. ${buildErrorMessages(passwordValidation)}"
                    render(view: 'createAccount', model: [edit: false, user: params, props: params, passwordPolicy: passwordService.buildPasswordPolicy(),
                                                          visibleMFA: false])
                    return
                }

                try {
                    //does a user with the supplied email address exist
                    def user = userService.registerUser(params)

                    if (user) {
                        //store the password
                        try {
                            passwordService.resetPassword(user, params.password, true, null)
                            //store the password
                            userService.sendAccountActivation(user)
                            redirect(action: 'accountCreated', id: user.id)
                        } catch (e) {
                            log.error("Couldn't reset password", e)
                            render(view: "accountError", model: [msg: "Failed to reset password"])
                        }
                    } else {
                        log.error('Couldn\'t create user')
                        render(view: "accountError", model: [msg: 'Couldn\'t create user'])
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    render(view: "accountError", model: [msg: e.getMessage()])
                }
            }
        }.invalidToken {
            redirect action: 'createAccount'
        }
    }

    def accountCreated() {
        def user= userService.getUserById(params.id)
        render(view: 'accountCreated', model: [user: user])
    }

    def forgottenPassword() {
        def currentUser = userService.getCurrentUser()
        def validHours = 48

        boolean isCodeRequired = grailsApplication.config.getProperty("password.requireCodeToResetPassword", Boolean)
        if(isCodeRequired){
            validHours = 1
        }
        render(view: 'forgottenPassword', model: [currentUser: currentUser, validHours: validHours])
    }

    def activateAccount() {
        def user= userService.getUserById(params.userId)
        boolean isSuccess = userService.activateAccount(user, params)

        if (isSuccess) {
            render(view: 'accountActivatedSuccessful', model: [user: user])
        } else {
            render(view: "accountError")
        }
    }

    @Operation(
            method = "GET",
            tags = "registration",
            summary = "Get list of registrable countries",
            operationId = "countries",
            description = "Get a list of registrable countries",
            parameters = [],
            responses = [
                    @ApiResponse(
                            description = "Get a list of countries",
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
    @Path("/ws/registration/countries.json")
    @Produces("application/json")
    def countries() {
        Map locations = locationService.getStatesAndCountries()
        respond locations.countries
    }
    @Operation(
            method = "GET",
            tags = "registration",
            summary = "Get list of registrable states",
            operationId = "states",
            description = "Get a list of registrable states, optionally for a specified country",
            parameters = [
                    @Parameter(
                            name = "country",
                            in = QUERY,
                            description = "Country to return states for - specified by ISO country code e.g AU, NZ etc.",
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get a list of states",
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
    @Path("/ws/registration/states.json")
    @Produces("application/json")
    def states(String country) {
        Map locations = locationService.getStatesAndCountries()
        if (country)
            respond locations.states[country] ?: []
        else
            respond locations.states
    }

    def getSecretForMfa() {
        def mfaResponse
        try {
            mfaResponse = [success: true, code: userService.getSecretForMfa()]
        } catch (e) {
            mfaResponse = [success: false, error: e.message]
        }
        render(mfaResponse as JSON)
    }

    def verifyAndActivateMfa() {
        try  {
            def success = userService.verifyUserCode(params.userCode)
            if (success) {
                userService.enableMfa(params.userId, true)
                render([success: true] as JSON)
            }
            else {
                render([success: false] as JSON)
            }
        } catch (e) {
            def result = [success: false, error: e.message]
            render result as JSON
        }

    }

    def disableMfa() {
        userService.enableMfa(params.userId, false)
        redirect(action: 'editAccount')
    }

    private String buildErrorMessages(RuleResult validationResult, Errors errors = null) {
        if (validationResult.valid) {
            return null
        }
        def results = []
        if (!validationResult.valid) {
            def details = validationResult.details
            for (def detail in details) {
                for (String errorCode in detail.errorCodes) {
                    def fullErrorCode = "user.password.error.${errorCode?.toLowerCase()}"
                    def errorValues = detail.values as Object[]
                    if (errors) {
                        errors.rejectValue('password', fullErrorCode, errorValues, "Invalid password.")
                    }
                    results.add(messageSource.getMessage(fullErrorCode, errorValues, "Invalid password.", LocaleContextHolder.locale))
                }
            }
        }
        return results.unique().sort().join(' ')
    }

    private isMFAVisible(userInstance) {
        boolean isMFAEnabled = grailsApplication.config.getProperty('account.MFAenabled', Boolean, false)
        List MFAUnsupportedRoles = grailsApplication.config.getProperty('users.delegated-group-names', List, []).collect { jwtProperties.getRolePrefix() + it.toUpperCase()}
        boolean hasMFAUnsupportedRoles = userInstance.roles.stream().anyMatch(userRoleRecord ->
                MFAUnsupportedRoles.contains(userRoleRecord.role.role))

        return isMFAEnabled && !hasMFAUnsupportedRoles
    }
}
