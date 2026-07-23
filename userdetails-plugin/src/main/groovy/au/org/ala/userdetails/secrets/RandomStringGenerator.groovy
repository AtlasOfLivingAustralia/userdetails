package au.org.ala.userdetails.secrets

import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.stream.IntStream

/** Implements the same random string logic as CAS for generating secure client ids and secrets */
class DefaultRandomStringGenerator extends AbstractRandomStringGenerator {

    /**
     * The array of printable characters to be used in our random string.
     */
    private static final char[] PRINTABLE_CHARACTERS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345679'.toCharArray()

    DefaultRandomStringGenerator() {
        super()
    }

    DefaultRandomStringGenerator(final long defaultLength) {
        super(defaultLength)
    }

    /**
     * Convert bytes to string, taking into account {@link #PRINTABLE_CHARACTERS}.
     *
     * @param random the random
     * @return the string
     */
    @Override
    protected String convertBytesToString(final byte[] random) {
        final output = new char[random.length]
        IntStream.range(0, random.length).forEach(i -> {
            final printableCharacters = getPrintableCharacters()
            final index = Math.abs(random[i] % printableCharacters.length)
            output[i] = printableCharacters[index]
        })
        return new String(output)
    }

    /**
     * Get printable characters char [].
     *
     * @return the char []
     */
    protected char[] getPrintableCharacters() {
        return PRINTABLE_CHARACTERS
    }
}

abstract class AbstractRandomStringGenerator implements RandomStringGenerator {
    /**
     * An instance of secure random to ensure randomness is secure.
     */
    protected final SecureRandom randomizer

    /**
     * Default string length before encoding.
     */
    final long defaultLength

    protected AbstractRandomStringGenerator(long defaultLength) {
        this.defaultLength = defaultLength
        SecureRandom secureRandom
        try {
            secureRandom = SecureRandom.getInstance('NativePRNGNonBlocking')
        } catch (NoSuchAlgorithmException e) {
            secureRandom = new SecureRandom()
        }
        this.randomizer = secureRandom
    }
    
    /**
     * Instantiates a new default random string generator
     * with length set to {@link RandomStringGenerator#DEFAULT_LENGTH}.
     */
    protected AbstractRandomStringGenerator() {
        this(DEFAULT_LENGTH)
    }

    @Override
    String getAlgorithm() {
        return randomizer.getAlgorithm()
    }

    /**
     * Converts byte[] to String by simple cast. Subclasses should override.
     *
     * @param random raw bytes
     * @return a converted String
     */
    protected String convertBytesToString(final byte[] random) {
        return new String(random, StandardCharsets.UTF_8)
    }

    @Override
    String getNewString(final int size) {
        final random = getNewStringAsBytes(size)
        return convertBytesToString(random)
    }

    @Override
    String getNewString() {
        return getNewString(Long.valueOf(getDefaultLength()).intValue())
    }

    @Override
    byte[] getNewStringAsBytes(final int size) {
        final random = new byte[size]
        this.randomizer.nextBytes(random)
        return random
    }
}

interface RandomStringGenerator {

    /**
     * The default length.
     */
    int DEFAULT_LENGTH = 36

    /**
     * Default length to use.
     * @return the default length as an int.
     */
    long getDefaultLength()

    /**
     * The algorithm used by the generator's SecureRandom instance.
     * @return the algorithm used by the generator's SecureRandom instance.
     */
    String getAlgorithm()

    /**
     * A new random string of specified initial size.
     * @param size length of random string before encoding
     * @return a new random string of specified initial size
     */
    String getNewString(int size)

    /**
     * A new random string of specified default size.
     * @return a new random string of default initial size
     */
    String getNewString()

    /**
     * Gets the new string as bytes.
     *
     * @param size the size of return
     * @return the new random string as bytes
     */
    byte[] getNewStringAsBytes(int size)
}
