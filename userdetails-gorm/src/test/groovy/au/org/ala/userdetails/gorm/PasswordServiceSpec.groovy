package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.PasswordService
import au.org.ala.userdetails.IUserService
import au.org.ala.userdetails.IPasswordOperations
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.passay.RuleResultMetadata

class PasswordServiceSpec extends UserDetailsSpec implements ServiceUnitTest<PasswordService>, DataTest {

    def charLength = RuleResultMetadata.CountCategory.Length
    def charLowerCase = RuleResultMetadata.CountCategory.LowerCase
    def charUpperCase = RuleResultMetadata.CountCategory.UpperCase
    def charDigit = RuleResultMetadata.CountCategory.Digit
    def charSpecial = RuleResultMetadata.CountCategory.Special
    def charWhitespace = RuleResultMetadata.CountCategory.Whitespace
    def charAllowed = RuleResultMetadata.CountCategory.Allowed
    def charIllegal = RuleResultMetadata.CountCategory.Illegal

    void setupSpec() {
        mockDomains(Role, User, Password, UserRole, UserProperty)
    }

    def setup() {
        defineBeans {
            userService(InstanceFactoryBean, Mock(IUserService), IUserService)
            passwordOperations(InstanceFactoryBean, Mock(IPasswordOperations), IPasswordOperations)
        }
       service.passwordOperations = new GormPasswordOperations()
    }

    def cleanup() {
        User.deleteAll()
        Role.deleteAll()
        UserRole.deleteAll()
        UserProperty.deleteAll()
    }

    @Override
    Closure doWithConfig() {{ c ->
        c.bcrypt.strength = 10
        c.password.encoder = 'bcrypt'
        c.encoding.algorithm = null
        c.encoding.salt = null
        c.password.policy.minLength = 8
        c.password.policy.enabled = true
        c.password.policy.maxLength = 64
        c.password.policy.excludeUsername = true
        c.password.policy.excludeUsQwertyKeyboardSequence = true
        c.password.policy.excludeCommonPasswords = true
        c.password.policy.charGroupMinRequired = 3
        c.password.policy.charGroupMinUpperCase = 1
        c.password.policy.charGroupMinLowerCase = 1
        c.password.policy.charGroupMinUpperOrLowerCase = 0
        c.password.policy.charGroupMinDigit = 1
        c.password.policy.charGroupMinSpecial = 1
    }}

    void 'test service passwordEncoderType is as expected'() {
        expect:
        service.passwordEncoderType == PasswordService.BCRYPT_ENCODER_TYPE
    }

    void 'test reset password generates and stores a new password for the user'() {
        given:
        def tempAuthKey = "wzAdTZYn1xJDJrjwHgpu"
        def user = createUser(1, tempAuthKey)
        def newPassword = "0hFAnO9dWq6rcUopZ9EN"

        when:
        service.resetPassword(user, newPassword, true, null)

        then:
        0 * _ // no other interactions
        Password.count() == 1
        service.passwordOperations.comparePasswords(newPassword, Password.first().password)
        Password.first().user == user
    }

    void 'test generate new password generates and stores a new password for the user'() {
        given:
        def tempAuthKey = "wzAdTZYn1xJDJrjwHgpu"
        def user = createUser(1, tempAuthKey)

        when:
        def newPassword = service.generatePassword(user)

        then:
        0 * _ // no other interactions
        Password.count() == 1
        service.passwordOperations.comparePasswords(newPassword, Password.first().password)
        Password.first().user == user
    }

    void 'test compare user password is true when given password matches existing password'() {
        given:
        def tempAuthKey = "wzAdTZYn1xJDJrjwHgpu"
        def user = createUser(1, tempAuthKey)
        def password = "0hFAnO9dWq6rcUopZ9EN"
        def encodedPassword = service.encodePassword(password)
        def existingPassword = new Password(
                user: user,
                password: encodedPassword,
                type: PasswordService.BCRYPT_ENCODER_TYPE,
                created: new Date().toTimestamp(),
                status: service.passwordOperations.STATUS_CURRENT
        ).save(flush: true)

        when:
        def isMatch = service.checkUserPassword(user, password)

        then:
        0 * _ // no other interactions
        isMatch
        Password.count() == 1
        Password.first() == existingPassword
        Password.first().password == existingPassword.password
        Password.first().user == user
    }

    void 'test compare user password is false when given password does not match existing password'() {
        given:
        def tempAuthKey = "wzAdTZYn1xJDJrjwHgpu"
        def user = createUser(1, tempAuthKey)
        def password = "0hFAnO9dWq6rcUopZ9EN"
        def wrongPassword = 'wrongpassword'
        def encodedPassword = service.encodePassword(password)
        def existingPassword = new Password(
                user: user,
                password: encodedPassword,
                type: PasswordService.BCRYPT_ENCODER_TYPE,
                created: new Date().toTimestamp(),
                status: service.passwordOperations.STATUS_CURRENT
        ).save(flush: true)

        when:
        def isMatch = service.checkUserPassword(user, wrongPassword)

        then:
        0 * _ // no other interactions
        !isMatch
        Password.count() == 1
        Password.first() == existingPassword
        Password.first().password == existingPassword.password
        Password.first().user == user
    }

    void 'test compare user password is false when given password does not match existing password for legacy encoder'() {
        given:
        def tempAuthKey = "wzAdTZYn1xJDJrjwHgpu"
        service.passwordEncoderType = 'legacy'
        service.legacyAlgorithm = 'md5'
        service.legacySalt = 'RSOU5UBkJq8OT6SeaFQI'
        service.passwordOperations.passwordEncoderType = 'legacy'
        service.passwordOperations.legacyAlgorithm = 'md5'
        service.passwordOperations.legacySalt = 'RSOU5UBkJq8OT6SeaFQI'
        def user = createUser(1, tempAuthKey)
        def password = "0hFAnO9dWq6rcUopZ9EN"
        def wrongPassword = 'wrongpassword'
        def encodedPassword = service.encodePassword(password)
        def existingPassword = new Password(
                user: user,
                password: encodedPassword,
                type: PasswordService.BCRYPT_ENCODER_TYPE,
                created: new Date().toTimestamp(),
                status: service.passwordOperations.STATUS_CURRENT
        ).save(flush: true)

        when:
        def isMatch = service.checkUserPassword(user, wrongPassword)

        then:
        0 * _ // no other interactions
        service.passwordEncoderType == 'legacy'
        !isMatch
        Password.count() == 1
        Password.first() == existingPassword
        Password.first().password == existingPassword.password
        Password.first().user == user
    }

    void 'test compare user password is false when user does not have an existing password'() {
        given:
        def tempAuthKey = "wzAdTZYn1xJDJrjwHgpu"
        def user = createUser(1, tempAuthKey)
        def password = "wzAdTZYn1xJDJrjwHgpu"

        when:
        def isMatch = service.checkUserPassword(user, password)

        then:
        0 * _ // no other interactions
        !isMatch
        Password.count() == 0
    }

    void 'test compare passwords is true when passwords match'() {
        given:
        def password = "0hFAnO9dWq6rcUopZ9EN"

        when:
        def encoded = service.encodePassword(password)
        def isMatch = service.passwordOperations.comparePasswords(password, encoded)

        then:
        0 * _ // no other interactions
        isMatch
    }

    void 'test compare passwords is false when passwords do not match'() {
        given:
        def password = "0hFAnO9dWq6rcUopZ9EN"
        def wrongPassword = "hFAnO9dWq6rcUopZ9EN"

        when:
        def encoded = service.encodePassword(password)
        def isMatch = service.passwordOperations.comparePasswords(wrongPassword, encoded)

        then:
        0 * _ // no other interactions
        !isMatch
    }

    void 'test compare passwords is true when passwords do match for legacy encoder'() {
        given:
        def password = "0hFAnO9dWq6rcUopZ9EN"
        service.passwordEncoderType = 'legacy'
        service.legacyAlgorithm = 'md5'
        service.legacySalt = 'RSOU5UBkJq8OT6SeaFQI'
        service.passwordOperations.passwordEncoderType = 'legacy'
        service.passwordOperations.legacyAlgorithm = 'md5'
        service.passwordOperations.legacySalt = 'RSOU5UBkJq8OT6SeaFQI'

        when:
        def encoded = service.encodePassword(password)
        def isMatch = service.passwordOperations.comparePasswords(password, encoded)

        then:
        0 * _ // no other interactions
        service.passwordEncoderType == 'legacy'
        isMatch
    }

    void 'test compare passwords is false when passwords do not match for legacy encoder'() {
        given:
        def password = "0hFAnO9dWq6rcUopZ9EN"
        def wrongPassword = "hFAnO9dWq6rcUopZ9EN"
        service.passwordEncoderType = 'legacy'
        service.legacyAlgorithm = 'md5'
        service.legacySalt = 'RSOU5UBkJq8OT6SeaFQI'
        service.passwordOperations.passwordEncoderType = 'legacy'
        service.passwordOperations.legacyAlgorithm = 'md5'
        service.passwordOperations.legacySalt = 'RSOU5UBkJq8OT6SeaFQI'

        when:
        def encoded = service.encodePassword(password)
        def isMatch = service.passwordOperations.comparePasswords(wrongPassword, encoded)

        then:
        0 * _ // no other interactions
        service.passwordEncoderType == 'legacy'
        !isMatch
    }

    void 'test valid password passes password check'() {
        given:
        def username = 'test'
        def password = '=!Vx,Zj^44\\f|xOd8Fjp'

        when:
        def result = service.validatePassword(username, password)

        then:
        0 * _ // no other interactions
        result.valid
        (result.metadata.hasCount(charLength) ? result.metadata.getCount(charLength) : 0) == 20
        (result.metadata.hasCount(charLowerCase) ? result.metadata.getCount(charLowerCase) : 0) == 7
        (result.metadata.hasCount(charUpperCase) ? result.metadata.getCount(charUpperCase) : 0) == 4
        (result.metadata.hasCount(charDigit) ? result.metadata.getCount(charDigit) : 0) == 3
        (result.metadata.hasCount(charSpecial) ? result.metadata.getCount(charSpecial) : 0) == 6
        (result.metadata.hasCount(charWhitespace) ? result.metadata.getCount(charWhitespace) : 0) == 0
        (result.metadata.hasCount(charAllowed) ? result.metadata.getCount(charAllowed) : 0) == 0
        (result.metadata.hasCount(charIllegal) ? result.metadata.getCount(charIllegal) : 0) == 0
        result.details.size() == 0
    }

    void 'test invalid password with username fails password check'() {
        given:
        def username = 'test'
        def password = "=!Vx,Zj^4test4\\f|xOd8Fjp"

        when:
        def result = service.validatePassword(username, password)

        then:
        0 * _ // no other interactions
        !result.valid
        (result.metadata.hasCount(charLength) ? result.metadata.getCount(charLength) : 0) == 24
        (result.metadata.hasCount(charLowerCase) ? result.metadata.getCount(charLowerCase) : 0) == 11
        (result.metadata.hasCount(charUpperCase) ? result.metadata.getCount(charUpperCase) : 0) == 4
        (result.metadata.hasCount(charDigit) ? result.metadata.getCount(charDigit) : 0) == 3
        (result.metadata.hasCount(charSpecial) ? result.metadata.getCount(charSpecial) : 0) == 6
        (result.metadata.hasCount(charWhitespace) ? result.metadata.getCount(charWhitespace) : 0) == 0
        (result.metadata.hasCount(charAllowed) ? result.metadata.getCount(charAllowed) : 0) == 0
        (result.metadata.hasCount(charIllegal) ? result.metadata.getCount(charIllegal) : 0) == 0
        result.details.size() == 1
        result.details[0].getErrorCodes() == ['ILLEGAL_USERNAME']
        result.details[0].getParameters().size() == 2
        result.details[0].getParameters().username == 'test'
        result.details[0].getParameters().matchBehavior.description == 'contains'
    }

    void 'test invalid password too short and not enough required chars fails password check'() {
        given:
        def username = 'test'
        def password = "AWDsdf"

        when:
        def result = service.validatePassword(username, password)

        then:
        0 * _ // no other interactions
        !result.valid
        (result.metadata.hasCount(charLength) ? result.metadata.getCount(charLength) : 0) == 6
        (result.metadata.hasCount(charLowerCase) ? result.metadata.getCount(charLowerCase) : 0) == 3
        (result.metadata.hasCount(charUpperCase) ? result.metadata.getCount(charUpperCase) : 0) == 3
        (result.metadata.hasCount(charDigit) ? result.metadata.getCount(charDigit) : 0) == 0
        (result.metadata.hasCount(charSpecial) ? result.metadata.getCount(charSpecial) : 0) == 0
        (result.metadata.hasCount(charWhitespace) ? result.metadata.getCount(charWhitespace) : 0) == 0
        (result.metadata.hasCount(charAllowed) ? result.metadata.getCount(charAllowed) : 0) == 0
        (result.metadata.hasCount(charIllegal) ? result.metadata.getCount(charIllegal) : 0) == 0
        result.details.size() == 4

        then:
        def tooShort = result.details.find{ it.errorCodes == ['TOO_SHORT'] }
        tooShort.getParameters().size() == 2
        tooShort.getParameters().minimumLength == 8
        tooShort.getParameters().maximumLength == 64

        then:
        def insuffDigit = result.details.find{ it.errorCodes == ['INSUFFICIENT_DIGIT'] }
        insuffDigit.getParameters().size() == 4
        insuffDigit.getParameters().minimumRequired == 1
        insuffDigit.getParameters().matchingCharacterCount == 0

        then:
        def insuffSpecial = result.details.find{ it.errorCodes == ['INSUFFICIENT_SPECIAL'] }
        insuffSpecial.getParameters().size() == 4
        insuffSpecial.getParameters().minimumRequired == 1
        insuffSpecial.getParameters().matchingCharacterCount == 0

        then:
        def insuffChar = result.details.find{ it.errorCodes == ['INSUFFICIENT_CHARACTERISTICS'] }
        insuffChar.getParameters().size() == 3
        insuffChar.getParameters().successCount == 2
        insuffChar.getParameters().minimumRequired == 3
        insuffChar.getParameters().ruleCount == 4
    }

    void 'test generate new password produces a valid password'() {
        given:
        def username = 'myname'

        when:
        def password = service.generateNewPassword(username)

        then:
        0 * _ // no other interactions
        password
        password.size() == 10
        !password.contains(username)
    }
}
