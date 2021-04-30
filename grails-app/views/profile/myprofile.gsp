<%@ page import="grails.util.Holders" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumb" content="My Profile" />
    <title><g:message code="userdetails.my.profile" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>

<div class="row">
    <div class="col-md-6">
        <h1><g:message code="myprofile.hello" args="[user.firstName]" /></h1>
        <ul class="userdetails-menu">
            <li>
                <g:link controller="registration" action="editAccount">
                     <g:message code="myprofile.update" />
                </g:link>
            </li>
            <li>
                <a href="${grailsApplication.config.sightings.url}/${user.id}">
                     <g:message code="myprofile.your.timeline" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.spatial.url}">
                    <g:message code="myprofile.spatial.portal" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.volunteer.url}">
                    <g:message code="myprofile.tasks.digivol" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.lists.url}">
                    <g:message code="myprofile.uploaded.species.lists" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.biocache.search.url}%22${user.id}%22">
                    <g:message code="myprofile.view.records.you.annotated" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.biocache.myDownloads.url}">
                    <g:message code="myprofile.your.downloads" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.myData.url}">
                    <g:message code="myprofile.your.datasets" />
                </a>
            </li>
            <li>
                <a href="${grailsApplication.config.alerts.url}">
                    <g:message code="myprofile.your.alerts" />
                </a>
            </li>
            <g:if test="${grailsApplication.config.dataprofile.url}">
            <li>
                <a href="${grailsApplication.config.dataprofile.url}" target="_blank">
                    <g:message code="myprofile.your.dataprofile" />
                </a>
            </li>
            </g:if>
            <li>
                <g:link controller="registration" action="forgottenPassword">
                    <g:message code="userdetails.index.reset.password" />
                </g:link>
            </li>

            <g:if test="${isAdmin}">
            <li>
                <g:link controller="admin">
                    <g:message code="myprofile.admin.tools" />
                </g:link>
            </li>
            </g:if>
        </ul>

        <h3><g:message code="myprofile.external.site.linkages" /></h3>
        <g:if test="${Holders.config.getProperty('oauth.providers.flickr.enabled', Boolean, true)}">
        <div class="well well-small">
            <h4><g:message code="myprofile.flickr.title" /></h4>
            <g:if test="${props.flickrUsername}">
                <strong><g:message code="myprofile.flickr.connected" args="[props.flickrId, props.flickrUsername]" />
                </strong>
                <p>
                <g:message code="myprofile.linking.with.flickr.enables.images" />

                <a href="http://www.flickr.com/groups/encyclopedia_of_life/"><g:message code="myprofile.flickr.eol.group" /></a>

                <g:message code="myprofile.to.be.linked.to.your.atlas" />
                </p>



                <g:link controller="profile" class="btn btn-default" action="removeLink" params="[provider: 'flickr']"><g:message code="myprofile.remove.link.to.flickr.account" /></g:link>
            </g:if>
            <g:else>
                <p>
                <g:message code="myprofile.flicker.link.description" />
                </p>

                <span class="btn btn-default">
                    <oauth:connect provider="flickr"><g:message code="myprofile.link.to.my.flickr.account" /></oauth:connect>
                </span>
            </g:else>
        </div>
        </g:if>
        <g:if test="${Holders.config.getProperty('oauth.providers.inaturalist.enabled', Boolean, false)}">
        <div class="well well-small">
            <h4>${grailsApplication.config.inaturalist.name}</h4>
            <g:if test="${props.inaturalistId}">
                <strong><g:message code="myprofile.inat.you.have.connected.with.user" args="[grailsApplication.config.inaturalist.name]" />
                    <u:link baseProperty="inaturalist.baseUrl" paths="['people', props.inaturalistId]">${props.inaturalistUsername}</u:link>
                </strong>
                <p>
                    <u:link baseProperty="biocache.search.baseUrl" params='[q: grailsApplication.config.inaturalist.searchQuery, fq: "alau_user_id:${props.inaturalistUsername}"]'>View my iNaturalist observations in ${grailsApplication.config.skin.orgNameShort}</u:link><br>
                    <u:link baseProperty="biocache.search.baseUrl" params='[q: grailsApplication.config.inaturalist.searchQuery + " OR " + grailsApplication.config.inaturalist.sightingsSearchQuery, fq: "alau_user_id:${props.inaturalistUsername} OR alau_user_id:\"${user.id}\""]'>View my iNaturalist observations and my ${grailsApplication.config.skin.orgNameShort} Sightings in ${grailsApplication.config.skin.orgNameShort}</u:link>
                </p>

                <g:link controller="profile" class="btn btn-default" action="removeLink" params="[provider: 'inaturalist']"><g:message code="myprofile.remove.link.to.inaturalist" /></g:link>
            </g:if>
            <g:else>
                <p>
                    <g:message code="myprofile.inaturalists.link.description" args="[grailsApplication.config.skin.orgNameShort, grailsApplication.config.skin.orgNameShort]" />
                </p>

                <span class="btn btn-default">
                    <oauth:connect provider="inaturalist"><g:message code="myprofile.link.to.my.inaturalist" /></oauth:connect>
                </span>
            </g:else>
        </div>
        </g:if>
    </div>
</div>
</body>
</html>
