package au.org.ala.userdetails;

import org.apache.commons.lang3.StringUtils;

public class PatternUtils {

    /**
     * Revert Pattern.quote(String)
     */
    public static String unquotePattern(String s) {
        if (s.startsWith("\\Q")) {
            s = s.substring(2);
        }
        if (s.endsWith("\\E")) {
            s = s.substring(0, s.length() - 2);
        }
        s = StringUtils.replace(s, "\\E\\\\E\\Q", "\\E");
        return s;
    }

}
