package au.org.ala.userdetails

import grails.testing.web.taglib.TagLibUnitTest
import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.spring.beans.factory.InstanceFactoryBean
import spock.lang.Specification

class UserDetailsTagLibSpec extends Specification implements TagLibUnitTest<UserDetailsTagLib> {

    def setup() {
        defineBeans {
            userService(InstanceFactoryBean, Stub(IUserService), IUserService)
        }
    }

    def cleanup() {
    }

    def "test token paginate"() {
        expect:
        tagLib.paginate(controller: 'user', action: 'list', total: null, nextToken: 'asdf1234', params: new GrailsParameterMap(request)) == '''<nav aria-label='Page navigation'>
  <ul class='pagination'>
    <li class='page-item'><a href="/user/list?max=20" class="page-link">First</a></li>
    <li class='page-item'><a href="/user/list?max=20&amp;token=asdf1234" class="page-link">&raquo;</a></li>
  </ul>
</nav>'''
    }

}
