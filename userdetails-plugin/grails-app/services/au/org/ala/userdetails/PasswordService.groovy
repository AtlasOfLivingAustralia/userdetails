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

import au.org.ala.auth.EnglishCustomCharacterData
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.IUser
import au.org.ala.auth.PasswordPolicy
import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
import au.org.ala.cas.encoding.PasswordEncoder
import grails.core.GrailsApplication
import org.apache.commons.lang3.RandomStringUtils
import org.passay.CharacterCharacteristicsRule
import org.passay.CharacterRule
import org.passay.DictionaryRule
import org.passay.EnglishCharacterData
import org.passay.EnglishSequenceData
import org.passay.IllegalSequenceRule
import org.passay.LengthRule
import org.passay.MatchBehavior
import org.passay.PasswordData
import org.passay.PasswordGenerator
import org.passay.PasswordValidator
import org.passay.Rule
import org.passay.RuleResult
import org.passay.UsernameRule
import org.passay.dictionary.WordListDictionary
import org.passay.dictionary.WordLists
import org.passay.dictionary.sort.ArraysSort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value


import java.security.SecureRandom

class PasswordService {

    GrailsApplication grailsApplication

    static final String BCRYPT_ENCODER_TYPE = 'bcrypt'
    static final String LEGACY_ENCODER_TYPE = 'legacy'

    private PasswordValidator builtPasswordValidator = null
    private PasswordPolicy builtPasswordPolicy = null
    private List<Rule> builtPasswordGeneralRules = null
    private CharacterCharacteristicsRule builtPasswordCharacterRule = null

    @Value('${password.encoder:bcrypt}')
    String passwordEncoderType = 'bcrypt'
    @Value('${bcrypt.strength:10}')
    Integer bcryptStrength = 10
    @Value('${encoding.algorithm}')
    String legacyAlgorithm
    @Value('${encoding.salt}')
    String legacySalt

    @Autowired
    IPasswordOperations passwordOperations

    void resetAndSendTemporaryPassword(IUser<?> user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        passwordOperations.resetAndSendTemporaryPassword(user, emailSubject, emailTitle, emailBody, password)
    }

    boolean resetPassword(IUser<?> user, String newPassword, boolean isPermanent, String confirmationCode) {
        return passwordOperations.resetPassword(user, newPassword, isPermanent, confirmationCode)
    }

    String getResetPasswordUrl(IUser<?> user) {
        return passwordOperations.getResetPasswordUrl(user)
    }

    String getPasswordResetView() {
        return passwordOperations.getPasswordResetView()
    }

    String generatePassword(IUser<?> user) {
        if (user == null) {
            throw new IllegalArgumentException("Must provide the user to generate a password.")
        }

       def newPassword = generateNewPassword(user?.userName ?: user?.email ?: '')
       resetPassword(user, newPassword, false, null)
       return newPassword
    }

    /**
     * Check that a plain-text password matches a user's existing password.
     * @param user The user.
     * @param password The plain-text password to match.
     * @return True if the password matches the existing password, otherwise false.
     */
    boolean checkUserPassword(IUser<?> user, String password) {
        return passwordOperations.checkUserPassword(user, password)
    }

    /**
     * Encode a password ready to store it.
     * @param password The password to encode.
     * @return The encoded password.
     */
    String encodePassword(String password) {
        if (!password || password.length() < 1) {
            throw new IllegalArgumentException("Must supply a password to be encoded.")
        }

        def encoder = getEncoder()
        def encodedPassword = encoder.encode(password)
        return encodedPassword
    }

    /**
     * Validate a password against a password policy.
     * @param username
     * @param password
     * @return
     */
    RuleResult validatePassword(String username, String password) {
        def policy = this.buildPasswordPolicy()
        if (!policy.enabled) {
            return new RuleResult(true)
        }

        if (username?.contains('@')) {
            // if the username is an email address, use only the part before the '@'
            username = username.split('@')[0]
        }
        def passwordData = new PasswordData(username, password, PasswordData.Origin.User)
        def passwordValidator = this.buildPasswordValidator()
        def result = passwordValidator.validate(passwordData)
        return result
    }

    /**
     * Generate a new password that meets the password policy.
     * Note that this method does not save the new password to the database.
     * @return A new password.
     */
    String generateNewPassword(String username) {
        def policy = this.buildPasswordPolicy()

        String newPassword = null

        if (policy.enabled) {
            def maxCount = 10
            def generator = new PasswordGenerator()
            boolean isValid = false
            def count = 0
            def passwordRules = buildPasswordCharacterRules()?.rules
            while (!isValid && count < maxCount) {
                newPassword = generator.generatePassword(policy.generatedLength, passwordRules)
                isValid = validatePassword(username, newPassword)?.valid ?: false
                count += 1
            }
            if (!isValid) {
                def msg = "Tried ${maxCount} times to generate a new password for user name '${username}', but none of the passwords satisfied the rules."
                log.error(msg)
                throw new IllegalStateException(msg)
            }
        } else {
            def randomSource = new SecureRandom()
            newPassword = RandomStringUtils.random(policy.generatedLength, 0, 0, true, true, null, randomSource)
        }

        log.warn("Generated a new password with length ${policy.generatedLength} for user name '${username}'.")
        return newPassword
    }

    PasswordPolicy buildPasswordPolicy() {
        if (this.builtPasswordPolicy == null) {
            Map passwordConfig = grailsApplication.config.password
            this.builtPasswordPolicy = new PasswordPolicy(passwordConfig)
        }
        return this.builtPasswordPolicy
    }

    /**
     * Get the password validator, built from the application settings.
     * @return The password validator.
     */
    PasswordValidator buildPasswordValidator() {
        def policy = this.buildPasswordPolicy()
        if (!policy.enabled) {
            return null
        }
        if (!this.builtPasswordValidator) {
            def rules = ([buildPasswordCharacterRules()] + buildPasswordGeneralRules()) as List<Rule>
            def validator = new PasswordValidator(rules.findAll { it != null })
            this.builtPasswordValidator = validator
        }

        return this.builtPasswordValidator
    }

    /**
     * Get the password character group rule.
     * @return The password character group rule.
     */
    CharacterCharacteristicsRule buildPasswordCharacterRules() {
        def policy = this.buildPasswordPolicy()
        if (!policy.enabled) {
            return null
        }

        if (!this.builtPasswordCharacterRule) {
            if (policy.charGroupMinRequired < 1) {
                log.warn("The 'password.policy' setting does not include any policy for the characters using 'charGroupMinRequired'.")
            }

            if (policy.charGroupMinRequired > 0 && policy.charGroupCount < 1) {
                throw new IllegalArgumentException(
                        "The 'password.policy' setting specifies ${policy.charGroupMinRequired} password character requirements using 'charGroupMinRequired', but has no 'charGroup*' character rules. " +
                                "There must be at least one rule that requires 1 or more characters.")
            }

            def ruleGroup = new CharacterCharacteristicsRule()
            if (policy.charGroupMinRequired > 0) {
                // at least charGroupMinRequired of all character rules must match
                ruleGroup.setNumberOfCharacteristics(policy.charGroupMinRequired)

                if (policy.charGroupMinUpperCase > 0) {
                    ruleGroup.rules.add(new CharacterRule(EnglishCharacterData.UpperCase, policy.charGroupMinUpperCase))
                }
                if (policy.charGroupMinLowerCase > 0) {
                    ruleGroup.rules.add(new CharacterRule(EnglishCharacterData.LowerCase, policy.charGroupMinLowerCase))
                }
                if (policy.charGroupMinUpperOrLowerCase > 0) {
                    ruleGroup.rules.add(new CharacterRule(EnglishCharacterData.Alphabetical, policy.charGroupMinUpperOrLowerCase))
                }
                if (policy.charGroupMinDigit > 0) {
                    ruleGroup.rules.add(new CharacterRule(EnglishCharacterData.Digit, policy.charGroupMinDigit))
                }
                if (policy.charGroupMinSpecial > 0) {
                    ruleGroup.rules.add(new CharacterRule(EnglishCustomCharacterData.Special, policy.charGroupMinSpecial))
                }
            }

            log.warn("The 'password.policy' setting for characters is '${ruleGroup}'.")

            this.builtPasswordCharacterRule = ruleGroup
        }

        if (this.builtPasswordCharacterRule.rules.size() < 1) {
            // if there are no rules, then return null to indicate that the char checks are not enabled
            return null
        }

        return this.builtPasswordCharacterRule
    }

    /**
     * Get the general password policy rules.
     * @return The list of general password rules.
     */
    List<Rule> buildPasswordGeneralRules() {
        def policy = this.buildPasswordPolicy()
        if (!policy.enabled) {
            return null
        }

        if (!this.builtPasswordGeneralRules) {
            List<Rule> rules = []

            rules.add(new LengthRule(policy.minLength, policy.maxLength))

            // ensure password does not contain the user name
            if (policy.excludeUsername) {
                rules.add(new UsernameRule(false, true, MatchBehavior.Contains))
            }

            // don't allow sequences of characters that are next to each other on a standard US qwerty keyboard
            if (policy.excludeUsQwertyKeyboardSequence) {
                rules.add(new IllegalSequenceRule(EnglishSequenceData.USQwerty))
            }

            // password must not be in the list of common passwords
            if (policy.excludeCommonPasswords) {
                // source https://raw.githubusercontent.com/danielmiessler/SecLists/5e1dc9cc79aac54b373349e2a97bbb22f1b63bb3/Passwords/Common-Credentials/10-million-password-list-top-100000.txt
                def commonPassInputStream = this.class.classLoader.getResourceAsStream("common-passwords.txt")
                def commonPassInputStreamReaders = [new InputStreamReader(commonPassInputStream)] as Reader[]
                def caseSensitive = false
                def sorting = new ArraysSort()
                def commonPasswordsList = WordLists.createFromReader(commonPassInputStreamReaders, caseSensitive, sorting)
                def commonPasswordsDict = new WordListDictionary(commonPasswordsList)
                def ruleCommonPasswords = new DictionaryRule(commonPasswordsDict)
                rules.add(ruleCommonPasswords)
            }

            log.warn("The 'password.policy' setting for general rules is '${rules.join("', '")}'.")

            this.builtPasswordGeneralRules = rules
        }

        return this.builtPasswordGeneralRules
    }

    private PasswordEncoder getEncoder() {
        def encoder = passwordEncoderType.equalsIgnoreCase(BCRYPT_ENCODER_TYPE) ?
                new BcryptPasswordEncoder(bcryptStrength) :
                new LegacyPasswordEncoder(legacySalt, legacyAlgorithm, true)
        return encoder
    }

}
