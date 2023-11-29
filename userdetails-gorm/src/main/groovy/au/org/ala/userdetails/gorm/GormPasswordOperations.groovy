package au.org.ala.userdetails.gorm

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
import au.org.ala.cas.encoding.PasswordEncoder
import au.org.ala.userdetails.EmailService
import au.org.ala.userdetails.IPasswordOperations
import au.org.ala.users.IUser
import au.org.ala.users.UserRecord
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Value

@Transactional
class GormPasswordOperations implements IPasswordOperations {

    static final String BCRYPT_ENCODER_TYPE = 'bcrypt'
    static final String LEGACY_ENCODER_TYPE = 'legacy'

    static final String STATUS_CURRENT = 'CURRENT'


    @Value('${password.encoder}')
    String passwordEncoderType = 'bcrypt'
    @Value('${bcrypt.strength}')
    Integer bcryptStrength = 10
    @Value('${encoding.algorithm}')
    String legacyAlgorithm
    @Value('${encoding.salt}')
    String legacySalt

    EmailService emailService

    @Transactional
    @Override
    boolean resetPassword(IUser<?> user, String newPassword, boolean isPermanent, String confirmationCode) {
        assert user instanceof User
        Password.findAllByUser(user).each {
            it.delete()
        }

        boolean isBcrypt = passwordEncoderType.equalsIgnoreCase(BCRYPT_ENCODER_TYPE)

        def encoder = isBcrypt ? new BcryptPasswordEncoder(bcryptStrength) : new LegacyPasswordEncoder(legacySalt, legacyAlgorithm, true)
        def encodedPassword = encoder.encode(newPassword)

        //reuse object if old password
        def password = new Password()
        password.user = user
        password.password = encodedPassword
        password.type = isBcrypt ? BCRYPT_ENCODER_TYPE : LEGACY_ENCODER_TYPE
        password.created = new Date().toTimestamp()
        password.expiry = null
        password.status = "CURRENT"
        password.save(failOnError: true)
        return true
    }

    @Transactional
    @Override
    void resetAndSendTemporaryPassword(IUser<?> user, String emailSubject, String emailTitle, String emailBody, String password = null) throws PasswordResetFailedException {
        assert user instanceof User
        if (user) {
            //set the temp auth key
            user.tempAuthKey = UUID.randomUUID().toString()
            user.save(flush: true)
            //send the email
            emailService.sendPasswordReset(user, user.tempAuthKey, emailSubject, emailTitle, emailBody, password)
        }
    }

    @NotTransactional
    @Override
    String getResetPasswordUrl(IUser<?> user) {
        assert user instanceof User
        if(user.tempAuthKey){
            emailService.getServerUrl() + "resetPassword/" +  user.id +  "/"  + user.tempAuthKey
        }
    }

    @NotTransactional
    @Override
    String getPasswordResetView() {
        return "startPasswordReset"
    }

    /**
     * Check that a plain-text password matches a user's existing password.
     * @param user The user.
     * @param password The plain-text password to match.
     * @return True if the password matches the existing password, otherwise false.
     */
    @Transactional(readOnly = true)
    boolean checkUserPassword(IUser<?> user, String password) {
        assert user instanceof User
        if (!password || password.size() < 1) {
            throw new IllegalArgumentException("The password must not be empty.")
        }
        if (user == null) {
            throw new IllegalArgumentException("Must provide the user to compare a password.")
        }

        def passwordType = getPasswordType()
        def passwordStatus = STATUS_CURRENT
        def existingPasswords = Password.findAllByUserAndTypeAndStatus(user, passwordType, passwordStatus)

        def dateTimeNow = new Date().toTimestamp()

        def matchingPassword = existingPasswords.find { item ->
            comparePasswords(password, item.password) && (item.expiry == null || item.expiry > dateTimeNow)
        }

        return matchingPassword
    }

    /**
     * Compare a plain-text password to an encoded password.
     * @param plainPassword The plain-text password.
     * @param hashedPassword The encoded password.
     * @return True if the passwords match, otherwise false.
     */
    @NotTransactional
    Boolean comparePasswords(String plainPassword, String hashedPassword) {
        if (!plainPassword || plainPassword.length() < 1 || !hashedPassword || hashedPassword.length() < 1) {
            throw new IllegalArgumentException("Must supply a plain text password and a hashed password to be compared.")
        }

        def encoder = getEncoder()
        def encodedPassword = encoder.matches(plainPassword, hashedPassword)
        return encodedPassword
    }

    @NotTransactional
    private PasswordEncoder getEncoder() {
        def encoder = passwordEncoderType.equalsIgnoreCase(BCRYPT_ENCODER_TYPE) ?
                new BcryptPasswordEncoder(bcryptStrength) :
                new LegacyPasswordEncoder(legacySalt, legacyAlgorithm, true)
        return encoder
    }

    @NotTransactional
    private getPasswordType() {
        return passwordEncoderType.equalsIgnoreCase(BCRYPT_ENCODER_TYPE) ? BCRYPT_ENCODER_TYPE : LEGACY_ENCODER_TYPE
    }
}
