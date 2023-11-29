package au.org.ala.userdetails

import au.org.ala.cas.encoding.CloseShieldWriter
import au.org.ala.users.IUser
import grails.converters.JSON

import javax.servlet.http.HttpServletResponse

class ResultStreamer {

    HttpServletResponse response
    String jsonConfig

    private boolean first = true
    private Writer csw

    def init() {
        response.contentType = 'application/json'
        response.characterEncoding = 'UTF-8'
        csw = new CloseShieldWriter(new BufferedWriter(response.writer))
        csw.print('[')
        if (jsonConfig) JSON.use(jsonConfig)
        else JSON.use('default')
    }

    def offer(IUser user) {
        if (!first) {
            csw.print(',')
        } else {
            first = false
        }
        (user as JSON).render(csw)
    }

    def finalise() {
        JSON.use('default')
    }

    def complete() {
        csw.print(']')
        csw.flush()
        response.flushBuffer()
    }

}
