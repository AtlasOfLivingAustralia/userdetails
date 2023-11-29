package au.org.ala.userdetails

import au.org.ala.userdetails.marshaller.CustomObjectMarshallers
import au.org.ala.userdetails.marshaller.UserMarshaller
import au.org.ala.userdetails.marshaller.UserPropertyMarshaller
import grails.plugins.*

class UserdetailsGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "5.2.1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "UserDetails base plugin" // Headline display name of the plugin
    def author = "ALA"
    def authorEmail = ""
    def description = '''\
Base functionality for the userdetails application
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://github.com/AtlasOfLivingAustralia/userdetails"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "MPL1.1"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Atlas of Living Australia", url: "http://www.ala.org.au/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
            userdetailsGrailsPluginConfiguration(UserDetailsGrailsPluginConfiguration)
            customObjectMarshallers(CustomObjectMarshallers){
                marshallers =[
                        new UserPropertyMarshaller(),
                        new UserMarshaller()
                ]
            }
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
