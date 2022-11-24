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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.gorm.annotation.Entity
import grails.web.databinding.WebDataBinding
import groovy.transform.EqualsAndHashCode

//@Entity
@EqualsAndHashCode(includes = 'id')
@JsonIgnoreProperties(['metaClass','errors'])
class UserProperty implements WebDataBinding, Serializable {

    User user
    String name
    String value

    static def addOrUpdateProperty(user, name, value){

        def up = UserProperty.findByUserAndName(user, name)
        if(!up){
           up = new UserProperty(user:user, name:name, value:value)
        } else {
           up.value = value
        }
        up.save(flush:true)
        up
    }

    static constraints = {
        value nullable: false, blank: true
        name nullable: false, blank: false
    }

    String toString(){
        name + " : " + value
    }
}
