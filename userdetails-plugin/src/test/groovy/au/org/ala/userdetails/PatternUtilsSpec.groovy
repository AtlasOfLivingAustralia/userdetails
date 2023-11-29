package au.org.ala.userdetails

import spock.lang.Specification

import java.util.regex.Pattern

class PatternUtilsSpec extends Specification {

    def "test unquote"() {
        setup:
        def value = '!@#$\\E%^&*()ASJHDG\\E!@#$%^SKD\\ELFGJjksdhlf'
        when:
        def result = PatternUtils.unquotePattern(Pattern.quote(value))
        then:
        result == value
    }

}
