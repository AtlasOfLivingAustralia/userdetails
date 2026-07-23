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

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.IUser
import grails.web.mapping.LinkGenerator

class EmailService {

    def grailsApplication

    LinkGenerator linkGenerator

    static transactional = false

    def sendPasswordReset(user, authKey, emailSubject = null, emailTitle = null, emailBody1 = null, password = null)
            throws PasswordResetFailedException {
        String emailBody2 = null

        if (!emailSubject) {
            emailSubject = "Reset your password"
        }
        if (!emailTitle) {
            emailTitle = "Reset your password"
        }

        if (!emailBody1) {
            // user requested password reset
            if (!password) {
                emailBody1 = "We have received a password reset request. You can reset your password by clicking the link below.  " +
                        "This will take you to a form where you can provide a new password for your account."
            } else { // bulk load users
                emailBody1 = "Welcome to the ALA!"
            }
        }
        if (!password) {
            // only if user requested password reset, no temp password generated
            emailBody2 = "If you did not request a new password, please let us know immediately by replying to this email."
        }
        try {
            sendMail {
              from grailsApplication.config.getProperty('emailSenderTitle')+"<" + grailsApplication.config.getProperty('emailSender') + ">"
              subject emailSubject
              to user.email
              body (view: '/email/resetPassword',
                    plugin:"email-confirmation",
                    model:[userName: user.firstName, link: getServerUrl() + "resetPassword/" +  user.id +  "/"  + authKey, emailTitle: emailTitle, emailBody1: emailBody1, emailBody2: emailBody2, password: password ]
              )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendAccountActivation(user, authKey) throws PasswordResetFailedException {
        try {
            sendMail {
                from grailsApplication.config.getProperty('emailSenderTitle') + "<" + grailsApplication.config.getProperty('emailSender') + ">"
                subject "Activate your account"
                to user.email
                body(view: '/email/activateAccount',
                        plugin: "email-confirmation",
                        model: [userName: user.firstName, link: getServerUrl() + "activateAccount/" + user.id + "/" + authKey, orgNameLong: grailsApplication.config.getProperty('skin.orgNameLong')]
                )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendAccountActivationSuccess(user, activatedAlerts) throws PasswordResetFailedException {
        try {
            sendMail {
                from grailsApplication.config.getProperty('emailSenderTitle') + "<" + grailsApplication.config.getProperty('emailSender') + ">"
                subject "Account activated successfully"
                to user.email
                body(view: '/email/activateAccountSuccess',
                        plugin: "email-confirmation",
                        model: [userName: user.firstName, activatedAlerts: activatedAlerts, alertsUrl: grailsApplication.config.getProperty('alerts.url')]
                )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendUpdateProfileSuccess(IUser user, List<String> emailRecipients) throws PasswordResetFailedException {
        try {
            sendMail {
                from grailsApplication.config.getProperty('emailSenderTitle')+"<" + grailsApplication.config.getProperty('emailSender') + ">"
                subject "Account updated successfully"
                to (emailRecipients.toArray())
                body (view: '/email/updateAccountSuccess',
                        plugin:"email-confirmation",
                        model:[userName: user.firstName, support: grailsApplication.config.getProperty('supportEmail')]
                )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendGeneratedPassword(user, generatedPassword) throws PasswordResetFailedException {
        try {
            sendMail {
              from grailsApplication.config.getProperty('emailSenderTitle')+"<" + grailsApplication.config.getProperty('emailSender') + ">"
              subject "Accessing your account"
              to user.email
              body (view: '/email/accessAccount',
                    plugin:"email-confirmation",
                    model:[userName: user.firstName, link: getMyProfileUrl(), generatedPassword: generatedPassword]
              )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def getMyProfileUrl(){
            grailsApplication.config.getProperty('grails.serverURL')  +
                    "/myprofile/"
    }

    def getServerUrl(){
        grailsApplication.config.getProperty('grails.serverURL') +
                    "/registration/"
    }
}
