package au.org.ala.userdetails

import grails.validation.Validateable
import grails.web.databinding.WebDataBinding


class ApplicationRecord implements WebDataBinding, Validateable {

    String name
    String clientId
    String secret
    ApplicationType type
    List<String> callbacks = []
    boolean needTokenAppAsCallback

    static constraints = {
        clientId blank: true
        secret blank: true
        name blank: false
        type nullable: false
    }

}

enum ApplicationType {
    CONFIDENTIAL,
    M2M,
    PUBLIC,
    UNKNOWN
}