package au.org.ala.userdetails

import groovy.xml.MarkupBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.support.RequestContextUtils
import org.springframework.web.util.UriComponentsBuilder

import java.nio.charset.StandardCharsets

import static grails.web.http.HttpHeaders.*

class UserDetailsTagLib {

    static namespace = 'ud'

    //static defaultEncodeAs = [taglib:'html']
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]
    static returnObjectForTags = ['roleList', 'userList']

    def messageSource

    @Autowired
    IUserService userService

    def roleList = { attrs, body ->
        userService.listRoles()
    }

    def userList = { attrs, body ->
        userService.listUsers()
    }

    /**
     * Adds pagination for both offset and token based pagination.  If there is a total count,
     * this method will fall back to the HeaderFooterTagLib (aka standard) pagination
     * but if total is omitted then this creates pagination based on next and previous pagination tokens.
     *
     * @emptyTag
     *
     * @attr total The total number of results to paginate or null to use the pagination token
     * @attr nextToken The token for the next page of results
     * @attr action the name of the action to use in the link, if not specified the default action will be linked
     * @attr controller the name of the controller to use in the link, if not specified the current controller will be linked
     * @attr id The id to use in the link
     * @attr params A map containing request parameters
     * @attr prev The text to display for the previous link (defaults to "Previous" as defined by default.paginate.prev property in I18n messages.properties)
     * @attr next The text to display for the next link (defaults to "Next" as defined by default.paginate.next property in I18n messages.properties)
     * @attr max The number of records displayed per page (defaults to 10). Used ONLY if params.max is empty
     * @attr maxsteps The number of steps displayed for pagination (defaults to 10). Used ONLY if params.maxsteps is empty
     * @attr offset Used only if params.offset is empty
     * @attr fragment The link fragment (often called anchor tag) to use
     */
    def paginate = { attrs, body ->
        def total = attrs.get('total')
        def params = attrs.get('params')

        if (total != null) {
            out << hf.paginate(attrs, body)
        } else {

            def writer = out

            def locale = RequestContextUtils.getLocale(request)

            def action = (attrs.action ? attrs.action : (params.action ? params.action : "index"))
            def prevToken = extractPrevToken() //this gives the token for current search not for previous page
            def nextToken = attrs.nextToken
            def max = params.int('max')

            if (!max) max = (attrs.int('max') ?: 20)

            def linkParams = [:]
            if (attrs.params) linkParams.putAll(attrs.params)
            linkParams.max = max

            def linkTagAttrs = [action: action]
            if (attrs.namespace) {
                linkTagAttrs.namespace = attrs.namespace
            }
            if (attrs.controller) {
                linkTagAttrs.controller = attrs.controller
            }
            if (attrs.id != null) {
                linkTagAttrs.id = attrs.id
            }
            if (attrs.fragment != null) {
                linkTagAttrs.fragment = attrs.fragment
            }
            //add the mapping attribute if present
            if (attrs.mapping) {
                linkTagAttrs.mapping = attrs.mapping
            }

            linkTagAttrs.params = linkParams

            def cssClasses = "pagination"
            if (attrs.class) {
                cssClasses = "pagination " + attrs.class
            }


            MarkupBuilder mb = new MarkupBuilder(writer)
            mb.nav('aria-label': "Page navigation") {
                mb.ul('class': cssClasses) {
                    mb.li {
                        mb.mkp.yieldUnescaped(
                            g.link(withParams(linkTagAttrs, [token: null])) {
                                (attrs.start ?: messageSource.getMessage('paginate.start', null, 'First', locale))
                            }
                        )
                    }

//                    if (prevToken) {
//                        mb.li {
//                            mb.mkp.yieldUnescaped(
//                                    g.link(withParams([token: prevToken], linkTagAttrs)) {
//                                (attrs.prev ?: messageSource.getMessage('paginate.prev', null, '&laquo;', locale))
//                                }
//                            )
//                        }
//                    } else {
//                        mb.li('class': 'disabled') {
//                            mb.span {
//                                mb.mkp.yieldUnescaped(
//                                        (attrs.prev ?: messageSource.getMessage('paginate.prev', null, '&laquo;', locale))
//                                )
//                            }
//                        }
//                    }

                    if (nextToken) {
                        mb.li {
                            mb.mkp.yieldUnescaped(
                                    g.link(withParams(linkTagAttrs, [token: nextToken])) {
                                (attrs.next ?: messageSource.getMessage('paginate.next', null, '&raquo;', locale))
                            }
                            )
                        }
                    } else {
                        mb.li('class': 'disabled') {
                            mb.span {
                                mb.mkp.yieldUnescaped(
                                        (attrs.next ?: messageSource.getMessage('paginate.next', null, '&raquo;', locale))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private extractPrevToken() {
        // This is dodgy, won't work if passing around URLs but will keep URLs clean without previous tokens in them
        // does anyone else even care about this though?
        def referrer = request.getHeader(REFERER)
        def result = ''
        if (referrer) {
            try {
                def params = UriComponentsBuilder.fromHttpUrl(referrer).build().queryParams
                def token = params.getFirst('token')
                if (token) {
                    result = URLDecoder.decode(token, StandardCharsets.UTF_8)
                }
            } catch (e) {
                log.warn("Couldn't parse referrer $referrer because ${e.message}")
            }
        }
        return result
    }

    private Map withParams(Map attrs, Map<String, Object> extraParams) {
        Map params = ((Map) attrs.params?.clone()) ?: [:]
        extraParams.each {
            if (it.value == null) {
                params.remove(it.key)
            } else {
                params.put(it.key, it.value)
            }
        }
        def newAttrs = (Map) attrs.clone()
        newAttrs.params = params
        newAttrs
    }

}
