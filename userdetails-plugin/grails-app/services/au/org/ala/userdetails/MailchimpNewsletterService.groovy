/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 */

package au.org.ala.userdetails

import au.org.ala.users.IUser
import au.org.ala.ws.service.WebService
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.springframework.beans.factory.annotation.Value

import java.security.MessageDigest

@Slf4j
class MailchimpNewsletterService {

    WebService webService

    @Value('${mailchimp.enabled:false}')
    boolean enabled
    @Value('${mailchimp.apiKey:}')
    String apiKey
    @Value('${mailchimp.serverPrefix:}')
    String serverPrefix
    @Value('${mailchimp.audienceId:}')
    String audienceId
    @Value('${mailchimp.newsletterTag:}')
    String newsletterTag

    Map getNewsletterStatus(def params) {
        if (params) {
            boolean subscribed = params.containsKey('newsletterSubscribed')
            return [available: isConfigured(), subscribed: subscribed, status: subscribed ? 'subscribed' : 'unsubscribed']
        }
        return [available: isConfigured(), subscribed: false, status: 'unavailable']
    }

    Map getUserStatus(IUser user) {
        String email = user?.email
        if (!isConfigured() || !email) {
            return [available: false, subscribed: false, status: 'unavailable']
        }

        try {
            Map response = getMember(memberUrl(email), headers)
            if (response.statusCode == HttpStatus.SC_NOT_FOUND) {
                Map createResponse = putMember(memberUrl(email), [
                        email_address: email,
                        status       : 'subscribed',
                        status_if_new: 'subscribed',
                        merge_fields : [FNAME: user.firstName ?: '', LNAME: user.lastName ?: ''],
                        tags         : [newsletterTag]
                ], headers)
                if (createResponse.statusCode in [HttpStatus.SC_OK, HttpStatus.SC_CREATED]) {
                    return [available: true, subscribed: true, status: 'subscribed']
                }
                log.warn('Mailchimp returned status {} while adding user subscription', createResponse.statusCode)
                return [available: false, subscribed: false, status: 'unavailable']
            }
            if (response.statusCode != HttpStatus.SC_OK) {
                log.warn('Mailchimp returned status {} while reading newsletter preference', response.statusCode)
                return [available: false, subscribed: false, status: 'unavailable']
            }

            Map member = response.resp as Map
            boolean tagActive = (member.tags as Collection ?: []).any { tag -> tag.name == newsletterTag }
            boolean subscribed = member.status == 'subscribed' && tagActive
            return [available: true, subscribed: subscribed, status: subscribed ? 'subscribed' : 'not_subscribed']
        } catch (Exception e) {
            log.warn('Could not read Mailchimp newsletter preference', e)
            return [available: false, subscribed: false, status: 'unavailable']
        }
    }

    boolean subscribe(IUser user) {
        if (!isConfigured() || !user?.email?.trim()) {
            return false
        }

        try {
            Map body = [
                    email_address: user.email,
                    status       : 'subscribed',
                    status_if_new: 'subscribed',
                    merge_fields : [FNAME: user.firstName ?: '', LNAME: user.lastName ?: ''],
                    tags          : [newsletterTag]
            ]
            Map response = putMember(memberUrl(user.email), body, headers)
            if (response.statusCode in [HttpStatus.SC_OK, HttpStatus.SC_CREATED]) {
                return true
            }
            log.warn('Mailchimp returned status {} while subscribing a user to the newsletter', response.statusCode)
        } catch (Exception e) {
            log.warn('Could not subscribe user to the Mailchimp newsletter', e)
        }
        false
    }

    boolean unsubscribe(String email) {
        if (!isConfigured() || !email?.trim()) {
            return false
        }

        try {
            Map body = [
                    email_address: email,
                    status       : 'unsubscribed',
                    status_if_new: 'unsubscribed'
            ]
            Map response = putMember(memberUrl(email), body, headers)
            if (response.statusCode in [HttpStatus.SC_OK, HttpStatus.SC_CREATED]) {
                return true
            }
            log.warn('Mailchimp returned status {} while unsubscribing a user from the newsletter', response.statusCode)
        } catch (Exception e) {
            log.warn('Could not unsubscribe user from the Mailchimp newsletter', e)
        }
        false
    }

    static String subscriberHash(String email) {
        MessageDigest.getInstance('MD5')
                .digest(email.trim().toLowerCase(Locale.ROOT).getBytes('UTF-8'))
                .encodeHex()
                .toString()
    }

    private String memberUrl(String email) {
        "https://${serverPrefix}.api.mailchimp.com/3.0/lists/${audienceId}/members/${subscriberHash(email)}"
    }

    private Map getHeaders() {
        [Authorization: "Bearer ${apiKey}", Accept: 'application/json']
    }

    private boolean isConfigured() {
        enabled && apiKey && serverPrefix && audienceId && newsletterTag
    }

    protected Map getMember(String url, Map requestHeaders) {
        webService.get(url, [:], ContentType.APPLICATION_JSON, false, false, requestHeaders)
    }

    protected Map putMember(String url, Map body, Map requestHeaders) {
        webService.put(url, body, [:], ContentType.APPLICATION_JSON, false, false, requestHeaders)
    }

    protected Map postTags(String url, Map body, Map requestHeaders) {
        webService.post(url, body, [:], ContentType.APPLICATION_JSON, false, false, requestHeaders)
    }
}
