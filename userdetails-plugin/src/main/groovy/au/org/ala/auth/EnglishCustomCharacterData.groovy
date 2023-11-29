package au.org.ala.auth


import org.passay.CharacterData
import org.passay.EnglishCharacterData

enum EnglishCustomCharacterData implements CharacterData {

    /** Special characters, only those that can be typed on a US QWERTY keyboard. */
    Special(EnglishCharacterData.Special.errorCode, "~`!@#\$%^&*()_-+={[}]:;\"'<,>.?/\\|")

    /** Error code. */
    private final String errorCode

    /** Characters. */
    private final String characters

    /**
     * Creates a new english custom character data.
     *
     * @param code Error code.
     * @param charString Characters as string.
     */
    EnglishCustomCharacterData(final String code, final String charString) {
        errorCode = code
        characters = charString
    }

    @Override
    String getErrorCode() {
        return errorCode
    }

    @Override
    String getCharacters() {
        return characters
    }
}
