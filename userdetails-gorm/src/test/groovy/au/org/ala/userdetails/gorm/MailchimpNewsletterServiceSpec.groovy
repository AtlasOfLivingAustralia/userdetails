package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.MailchimpNewsletterService
import au.org.ala.users.IUser
import au.org.ala.users.UserRecord
import spock.lang.Specification

class MailchimpNewsletterServiceSpec extends Specification {

    TestMailchimpNewsletterService service

    void setup() {
        service = new TestMailchimpNewsletterService(
                enabled: true,
                apiKey: 'test-key',
                serverPrefix: 'us1',
                audienceId: 'audience-1',
                newsletterTag: 'ALA Newsletter'
        )
    }

    void 'subscriber hash normalises the email address'() {
        expect:
        MailchimpNewsletterService.subscriberHash(' Test@Example.ORG ') == '0c17bf66e649070167701d2d3cd71711'
    }

    void 'newsletter status reflects submitted parameters'() {
        expect:
        service.getNewsletterStatus([newsletterSubscribed: 'on', _newsletterSubscribed: '']) ==
                [available: true, subscribed: true, status: 'subscribed']
        service.getNewsletterStatus([_newsletterSubscribed: '']) ==
                [available: true, subscribed: false, status: 'unsubscribed']
        service.getNewsletterStatus(null) ==
                [available: true, subscribed: false, status: 'unavailable']
    }

    void 'subscribe upserts the member and newsletter tag'() {
        given:
        service.response = [statusCode: 200]
        def user = new UserRecord(email: 'test@example.org', firstName: 'Test', lastName: 'User')

        expect:
        service.subscribe(user)
        service.lastUrl.contains('/lists/audience-1/members/')
        service.lastBody.email_address == 'test@example.org'
        service.lastBody.status == 'subscribed'
        service.lastBody.tags == ['ALA Newsletter']
        service.lastHeaders.Authorization == 'Bearer test-key'
    }

    void 'unsubscribe adds or updates the member as unsubscribed'() {
        given:
        service.response = [statusCode: 200]

        expect:
        service.unsubscribe('test@example.org')
        service.lastUrl.contains('/lists/audience-1/members/')
        service.lastBody == [
                email_address: 'test@example.org',
                status       : 'unsubscribed',
                status_if_new: 'unsubscribed'
        ]
    }

    void 'status requires both a subscribed member and newsletter tag'() {
        given:
        service.response = [statusCode: 200, resp: [status: 'subscribed', tags: [[name: 'ALA Newsletter']]]]

        when:
        Map status = service.getUserStatus(Stub(IUser) {
            getEmail() >> 'test@example.org'
        })

        then:
        status.available
        status.subscribed
    }

    void 'missing member is added as subscribed and shown checked'() {
        given:
        service.getResponse = [statusCode: 404]
        service.putResponse = [statusCode: 200]

        when:
        Map status = service.getUserStatus(Stub(IUser) {
            getEmail() >> 'existing@example.org'
            getFirstName() >> 'Existing'
            getLastName() >> 'User'
        })

        then:
        status == [available: true, subscribed: true, status: 'subscribed']
        service.lastBody == [
                email_address: 'existing@example.org',
                status       : 'subscribed',
                status_if_new: 'subscribed',
                merge_fields : [FNAME: 'Existing', LNAME: 'User'],
                tags         : ['ALA Newsletter']
        ]
    }

    private static class TestMailchimpNewsletterService extends MailchimpNewsletterService {
        Map response
        Map getResponse
        Map putResponse
        String lastUrl
        Map lastBody
        Map lastHeaders

        @Override
        protected Map getMember(String url, Map requestHeaders) {
            lastUrl = url
            lastHeaders = requestHeaders
            getResponse ?: response
        }

        @Override
        protected Map putMember(String url, Map body, Map requestHeaders) {
            lastUrl = url
            lastBody = body
            lastHeaders = requestHeaders
            putResponse ?: response
        }

        @Override
        protected Map postTags(String url, Map body, Map requestHeaders) {
            lastUrl = url
            lastBody = body
            lastHeaders = requestHeaders
            response
        }
    }
}
