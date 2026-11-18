package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.IUserService
import au.org.ala.userdetails.MailchimpNewsletterService
import au.org.ala.userdetails.ProfileService
import au.org.ala.users.UserRecord
import spock.lang.Specification

class ProfileServiceSpec extends Specification {

    IUserService userService = Mock()
    MailchimpNewsletterService mailchimpNewsletterService = Mock()
    ProfileService service = new ProfileService(
            userService: userService,
            mailchimpNewsletterService: mailchimpNewsletterService
    )

    void 'subscribes the latest user record without saving a property'() {
        given:
        def user = new UserRecord(id: '123', email: 'old@example.org')
        def updatedUser = new UserRecord(id: '123', email: 'new@example.org')

        when:
        boolean updated = service.updateNewsletterSubscription(user, [newsletterSubscribed: 'true'])

        then:
        1 * userService.getUserById('123') >> updatedUser
        1 * mailchimpNewsletterService.subscribe(updatedUser) >> true
        0 * userService.addOrUpdateProperty(_, _, _)
        0 * userService.removeUserProperty(_, _)
        updated
    }

    void 'unsubscribes by email without saving a property'() {
        given:
        def user = new UserRecord(id: '123', email: 'user@example.org')

        when:
        boolean updated = service.updateNewsletterSubscription(user, [_newsletterSubscribed: ''])

        then:
        1 * userService.getUserById('123') >> null
        1 * mailchimpNewsletterService.unsubscribe('user@example.org') >> true
        0 * userService.addOrUpdateProperty(_, _, _)
        0 * userService.removeUserProperty(_, _)
        updated
    }

    void 'returns false when Mailchimp cannot be updated'() {
        given:
        def user = new UserRecord(id: '123', email: 'user@example.org')

        when:
        boolean updated = service.updateNewsletterSubscription(user, [newsletterSubscribed: 'true'])

        then:
        1 * userService.getUserById('123') >> user
        1 * mailchimpNewsletterService.subscribe(user) >> false
        0 * userService.removeUserProperty(_, _)
        !updated
    }

    void 'does nothing when newsletter parameters were not submitted'() {
        given:
        def user = new UserRecord(id: '123', email: 'user@example.org')

        when:
        boolean updated = service.updateNewsletterSubscription(user, [:])

        then:
        0 * userService._
        0 * mailchimpNewsletterService._
        updated
    }

}
