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

package au.org.ala.userdetails.gorm

import java.sql.Timestamp

/*
+----------+--------------+------+-----+---------------------+-----------------------------+
| Field    | Type         | Null | Key | Default             | Extra                       |
+----------+--------------+------+-----+---------------------+-----------------------------+
| userid   | bigint(20)   | NO   | PRI | NULL                |                             |
| password | varchar(255) | NO   | PRI | NULL                |                             |
| type     | varchar(255) | NO   |     | NULL                |                             |
| created  | timestamp    | NO   |     | CURRENT_TIMESTAMP   |                             |
| expiry   | timestamp    | YES  |     | NULL                |                             |
| status   | varchar(10)  | NO   |     | NULL                |                             |
+----------+--------------+------+-----+---------------------+-----------------------------+
 */
class Password implements Serializable {
    static belongsTo = [user: User]
    String password //encoded
    String type
    Timestamp created
    Timestamp expiry
    String status

    static mapping = {
        table 'passwords'
        id composite: ['user', 'password']
        user column:  'userid'
        created sqlType: 'timestamp'
        expiry sqlType: 'timestamp'
        version false
    }
    static constraints = {
        password nullable: false, blank: false
        type nullable: false, blank: false
        status nullable: false, blank: false
        expiry nullable: true
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        Password password1 = (Password) o

        if (password != password1.password) return false
        if (user != password1.user) return false

        return true
    }

    int hashCode() {
        int result
        result = password.hashCode()
        result = 31 * result + (user != null ? user.hashCode() : 0)
        return result
    }
}
