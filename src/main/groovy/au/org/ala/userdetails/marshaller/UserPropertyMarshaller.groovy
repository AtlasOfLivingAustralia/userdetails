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

/**
 * Created by Temi Varghese on 8/09/15.
 */
package au.org.ala.userdetails.marshaller

import grails.converters.JSON
import au.org.ala.userdetails.*

class UserPropertyMarshaller {
    void register(){

        JSON.registerObjectMarshaller(UserProperty){ UserProperty prop ->
            return [
                    property: prop.name,
                    value: prop.value
            ]
        }
    }
}