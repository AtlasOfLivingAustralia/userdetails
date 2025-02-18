package au.org.ala.auth

class PasswordPolicy {
    final Integer minLength
    final Boolean enabled
    final Integer maxLength
    final Boolean excludeUsername
    final Boolean excludeUsQwertyKeyboardSequence
    final Boolean excludeCommonPasswords
    final Integer charGroupMinRequired
    final Integer charGroupMinUpperCase
    final Integer charGroupMinLowerCase
    final Integer charGroupMinUpperOrLowerCase
    final Integer charGroupMinDigit
    final Integer charGroupMinSpecial
    final Integer generatedLength

    /**
     * Create a new password policy. Always sets at least the password min and max length, even if disabled.
     * All other settings default to 0 or false.
     * @param attrs
     */
    PasswordPolicy(Map attrs){
        attrs = attrs ?: [:]
        // reference: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#implement-proper-password-strength-controls
        this.minLength = attrs?.policy?.minLength?.toString()?.toInteger() ?: 8
        this.maxLength = attrs?.policy?.maxLength?.toString()?.toInteger() ?: 64
        this.generatedLength = attrs?.generatedLength?.toString()?.toInteger() ?: 10

        this.enabled = attrs?.policy?.enabled?.toString()?.toBoolean() ?: false
        this.excludeUsername = attrs?.policy?.excludeUsername?.toString()?.toBoolean() ?: false
        this.excludeUsQwertyKeyboardSequence = attrs?.policy?.excludeUsQwertyKeyboardSequence?.toString()?.toBoolean() ?: false
        this.excludeCommonPasswords = attrs?.policy?.excludeCommonPasswords?.toString()?.toBoolean() ?: false
        this.charGroupMinRequired = attrs?.policy?.charGroupMinRequired?.toString()?.toInteger() ?: 0
        this.charGroupMinUpperCase = attrs?.policy?.charGroupMinUpperCase?.toString()?.toInteger() ?: 0
        this.charGroupMinLowerCase = attrs?.policy?.charGroupMinLowerCase?.toString()?.toInteger() ?: 0
        this.charGroupMinUpperOrLowerCase = attrs?.policy?.charGroupMinUpperOrLowerCase?.toString()?.toInteger() ?: 0
        this.charGroupMinDigit = attrs?.policy?.charGroupMinDigit?.toString()?.toInteger() ?: 0
        this.charGroupMinSpecial = attrs?.policy?.charGroupMinSpecial?.toString()?.toInteger() ?: 0
    }

    Integer getCharGroupCount() {
        def count =[
                this.charGroupMinUpperCase > 0 ? 1 : 0,
                this.charGroupMinLowerCase > 0 ? 1 : 0,
                this.charGroupMinUpperOrLowerCase > 0 ? 1 : 0,
                this.charGroupMinDigit > 0 ? 1 : 0,
                this.charGroupMinSpecial > 0 ? 1 : 0,
        ].sum() as Integer
        return count
    }
}
