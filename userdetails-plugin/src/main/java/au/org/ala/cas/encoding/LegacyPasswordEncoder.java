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

package au.org.ala.cas.encoding;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LegacyPasswordEncoder implements PasswordEncoder {

    private final String salt;
    private final String algorithm;
    private final boolean base64Encoding;

    public LegacyPasswordEncoder(String salt, String algorithm, boolean base64Encoding) {
        this.salt = salt;
        this.algorithm = algorithm;
        this.base64Encoding = base64Encoding;
    }

    @Override
    public String encode(final String password) {
        String salted = password + "{" + this.salt + "}";

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(this.algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        byte[] digest;

        try {
            digest = messageDigest.digest(salted.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported!");
        }

        if (base64Encoding) {
            return new String(au.org.ala.cas.encoding.Base64.encode(digest));
        } else {
            return new String(digest);
        }
    }

    @Override
    public Boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.length() < 1 || hashedPassword == null || hashedPassword.length() < 1) {
            throw new IllegalArgumentException("Must provide both plain password and hashed password.");
        }
        String encoded = encode(plainPassword);
        return encoded.equals(hashedPassword);
    }
}