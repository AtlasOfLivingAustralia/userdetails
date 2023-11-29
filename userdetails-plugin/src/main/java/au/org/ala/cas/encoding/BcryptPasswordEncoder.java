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

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

public class BcryptPasswordEncoder implements PasswordEncoder {

    private static final int MIN_LOG_ROUNDS = 4;
    private static final int MAX_LOG_ROUNDS = 31;
    private final int strength;
    private final SecureRandom random;

    public BcryptPasswordEncoder(int strength) {
        this(strength, null);
    }

    public BcryptPasswordEncoder(int strength, SecureRandom random) {
        if (strength != -1 && (strength < MIN_LOG_ROUNDS || strength > MAX_LOG_ROUNDS)) {
            throw new IllegalArgumentException("Strength should be between " + MIN_LOG_ROUNDS + " and " + MAX_LOG_ROUNDS);
        }

        this.strength = strength;
        this.random = random;
    }

    @Override
    public String encode(String password) {
        String salt;
        if (strength > 0) {
            if (random != null) {
                salt = BCrypt.gensalt(strength, random);
            } else {
                salt = BCrypt.gensalt(strength);
            }
        } else {
            salt = BCrypt.gensalt();
        }
        return BCrypt.hashpw(password, salt);
    }

    @Override
    public Boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.length() < 1 || hashedPassword == null || hashedPassword.length() < 1) {
            throw new IllegalArgumentException("Must provide both plain password and hashed password.");
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
