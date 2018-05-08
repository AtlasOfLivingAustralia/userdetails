import org.scribe.builder.api.FlickrApi

/******************************************************************************\
 *  CONFIG MANAGEMENT
\******************************************************************************/
def appName = 'userdetails'
def ENV_NAME = "${appName.toUpperCase()}_CONFIG"
default_config = "/data/${appName}/config/${appName}-config.properties"
if(!grails.config.locations || !(grails.config.locations instanceof List)) {
    grails.config.locations = []
}

if(System.getenv(ENV_NAME) && new File(System.getenv(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified in environment: " + System.getenv(ENV_NAME);
    grails.config.locations.add "file:" + System.getenv(ENV_NAME)
} else if(System.getProperty(ENV_NAME) && new File(System.getProperty(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified on command line: " + System.getProperty(ENV_NAME);
    grails.config.locations.add "file:" + System.getProperty(ENV_NAME)
} else if(new File(default_config).exists()) {
    println "[${appName}] Including default configuration file: " + default_config;
    grails.config.locations.add "file:" + default_config
} else {
    println "[${appName}] No external configuration file defined."
}

skin.layout = "main"
skin.orgNameLong = "Atlas of Living Australia"
skin.orgNameShort = "ALA"
privacyPolicy = "http://www.ala.org.au/about/terms-of-use/privacy-policy/"

println "[${appName}] (*) grails.config.locations = ${grails.config.locations}"

/******************************************************************************\
 *  All configuration properties are stored in the template properties file in
 *  the ALA-INSTALL source repository:
 *  https://github.com/AtlasOfLivingAustralia/ala-install/blob/master/ansible/roles/userdetails/templates/userdetails-config.properties
\******************************************************************************/


/******************************************************************************/
grails.project.groupId = 'au.org.ala' // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}


grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// enable query caching by default
grails.hibernate.cache.queries = true

// convert empty strings to NULL in DB
grails.databinding.convertEmptyStringsToNull = false

grails.plugins.twitterbootstrap.fixtaglib = true

security {
    cas {
        uriFilterPattern = '/admin/.*,/registration/editAccount,/my-profile,/my-profile/,/myprofile/,/myprofile,/profile/.*,/admin/,/admin,/registration/disableAccount/.*,/registration/disableAccount,/registration/update,/registration/update/.*,/monitoring,/monitoring/*,/alaAdmin.*'
        uriFilterPattern = '/admin/.*,/registration/editAccount,/my-profile,/my-profile/,/myprofile/,/myprofile,/profile/.*,/admin/,/admin,/registration/update,/registration/update/.*,/monitoring,/monitoring/*,/registration/disableAccount/.*,/registration/disableAccount,/admin/admin/downloadUsersCsvFile,/alaAdmin.*'
    }
}

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.serverURL = "http://devt.ala.org.au:8080/userdetails"
        security.cas.appServerName = "http://devt.ala.org.au:8080"
        grails.logging.jul.usebridge = true
        grails {
          mail {
            host = "localhost"
            port = 1025
            username = postie.emailSender
          }
        }
    }
    test {
        security.cas.appServerName = "http://auth.ala.org.au"
    }
    production {
        grails.serverURL = "http://auth.ala.org.au/userdetails"
        security.cas.appServerName = "http://auth.ala.org.au"
        grails.logging.jul.usebridge = false
        grails {
          mail {
            host = "localhost"
            port = 25
            username = postie.emailSender
          }
        }
    }
}

def loggingDir = (System.getProperty('catalina.base') ? System.getProperty('catalina.base') + '/logs' : './logs')
//def appName = grails.util.Metadata.current.'app.name'
// log4j configuration
log4j = {
// Example of changing the log pattern for the default console
// appender:
    appenders {
        environments {
            production {
                String logFilename = "${loggingDir}/${appName}.log"
                println("Application will log to file: ${logFilename}")
                rollingFile name: "tomcatLog", maxFileSize: '1MB', file: "${logFilename}", threshold: org.apache.log4j.Level.WARN, layout: pattern(conversionPattern: "%d %-5p [%c{1}] %m%n")
            }
            development {
                console name: "stdout", layout: pattern(conversionPattern: "%d %-5p [%c{1}] %m%n"), threshold: org.apache.log4j.Level.DEBUG
            }
            test {
                String logFilename = "/tmp/${appName}"
                println("Application will log to file: ${logFilename}")
                rollingFile name: "tomcatLog", maxFileSize: '1MB', file: "${logFilename}", threshold: org.apache.log4j.Level.DEBUG, layout: pattern(conversionPattern: "%d %-5p [%c{1}] %m%n")
            }
        }
    }
    root {
        // change the root logger to my tomcatLog file
        error 'tomcatLog'
        warn 'tomcatLog'
        additivity = true
    }

    error   'grails.spring.BeanBuilder',
            'grails.plugin.webxml',
            'grails.plugin.cache.web.filter',
            'grails.app.services.org.grails.plugin.resource',
            'grails.app.taglib.org.grails.plugin.resource',
            'grails.app.resourceMappers.org.grails.plugin.resource'

    warn   'au.org.ala.cas.client'

    debug   'grails.app','au.org.ala.cas','au.org.ala.userdetails'
}

// these placeholder values are overridden at runtime using the external configuration properties file
oauth {
    providers {
        flickr {
            api = FlickrApi
            key = "xxxxxxxxxxxxxxxxxxxxx"
            secret = "xxxxxxxxxxxxxxxxxxxxx"
            successUri = '/profile/flickrSuccess'
            failureUri = '/profile/flickrFail'
            callback = "xxxxxxxxxxxxx/yyyyyyyyyy/profile/flickrCallback"
        }
    }
//   debug = true
}

config.grails.cache.config = {
    defaults {
        eternal false
        overflowToDisk false
        maxElementsInMemory 10000
        timeToLiveSeconds 3600
    }
    cache {
        name 'dailyCache'
        timeToLiveSeconds (3600 * 24)
    }
}