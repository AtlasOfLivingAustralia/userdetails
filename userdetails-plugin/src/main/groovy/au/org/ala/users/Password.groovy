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

package au.org.ala.users

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
    static belongsTo = [user: UserRecord]
    String password //encoded
    String type
    Timestamp created
    Timestamp expiry
    String status

    static constraints = {
        password nullable: false, blank: false
        type nullable: false, blank: false
        status nullable: false, blank: false
        expiry nullable: true
    }
}
