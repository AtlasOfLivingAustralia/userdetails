%{--
  - Copyright (C) 2022 Atlas of Living Australia
  - All Rights Reserved.
  -
  - The contents of this file are subject to the Mozilla Public
  - License Version 1.1 (the "License"); you may not use this file
  - except in compliance with the License. You may obtain a copy of
  - the License at http://www.mozilla.org/MPL/
  -
  - Software distributed under the License is distributed on an "AS
  - IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  - implied. See the License for the specific language governing
  - rights and limitations under the License.
  --}%

<%@ page import="au.org.ala.users.UserRecord" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}">
    <g:set var="entityName" value="${message(code: 'user.label', default: 'UserRecord')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
    <meta name="breadcrumbParent" content="${g.createLink(action:"list")},${g.message(code:"default.list.label", args:[entityName])}" />
    <asset:stylesheet src="userdetails.css" />
</head>

<body>

<div id="show-user" class="content scaffold-show" role="main">
    <h1><g:message code="default.show.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <ol class="property-list user">

<div class="row">
<div class="col-md-8">

        <g:if test="${userInstance?.firstName}">
            <li class="fieldcontain">
                <span id="firstName-label" class="property-label"><g:message code="user.firstName.label"
                                                                             default="First Name"/></span>

                <span class="property-value" aria-labelledby="firstName-label"><g:fieldValue bean="${userInstance}"
                                                                                             field="firstName"/></span>

            </li>
        </g:if>

        <g:if test="${userInstance?.lastName}">
            <li class="fieldcontain">
                <span id="lastName-label" class="property-label"><g:message code="user.lastName.label"
                                                                            default="Last Name"/></span>

                <span class="property-value" aria-labelledby="lastName-label"><g:fieldValue bean="${userInstance}"
                                                                                            field="lastName"/></span>

            </li>
        </g:if>

        <g:if test="${userInstance?.email}">
            <li class="fieldcontain">
                <span id="email-label" class="property-label"><g:message code="user.email.label"
                                                                         default="Email"/></span>

                <span class="property-value" aria-labelledby="email-label"><g:fieldValue bean="${userInstance}"
                                                                                         field="email"/></span>

            </li>
        </g:if>


        <g:if test="${userInstance?.dateCreated}">
            <li class="fieldcontain">
                <span id="dateCreated-label" class="property-label"><g:message code="user.dateCreated.label"
                                                                           default="Created"/></span>

                <span class="property-value" aria-labelledby="dateCreated-label"><g:fieldValue bean="${userInstance}"
                                                                                           field="dateCreated"/></span>

            </li>
        </g:if>

    <g:if test="${userInstance?.lastUpdated}">
        <li class="fieldcontain">
            <span id="lastUpdated-label" class="property-label"><g:message code="user.lastUpdated.label"
                                                                       default="Last updated"/></span>

            <span class="property-value" aria-labelledby="lastUpdated-label"><g:fieldValue bean="${userInstance}"
                                                                                       field="lastUpdated"/></span>

        </li>
    </g:if>

    <g:if test="${userInstance?.lastLogin}">
        <li class="fieldcontain">
            <span id="lastLogin-label" class="property-label"><g:message code="user.lastLogin.label"
                                                                       default="Last login"/></span>

            <span class="property-value" aria-labelledby="lastLogin-label"><g:fieldValue bean="${userInstance}"
                                                                                       field="lastLogin"/></span>

        </li>
    </g:if>

        <g:if test="${userInstance?.tempAuthKey}">
        %{--If tempAuthKey is present then there should be a resetPasswordUrl--}%
            <li class="fieldcontain">
                <span id="tempAuthKey-label" class="property-label"><g:message code="user.tempAuthKey.label"
                                                                                   default="Temp Auth Key"/></span>

                <span class="property-value" aria-labelledby="tempAuthKey-label">
                    <a  href="${resetPasswordUrl}">   <g:fieldValue bean="${userInstance}"
                                                                  field="tempAuthKey"/></a>
                </span>
            </li>
        </g:if>

        <g:if test="${userInstance?.userProperties}">

                <g:each in="${userInstance.userProperties}" var="u">
                                <li class="fieldcontain">

                    <span id="userProperties-label" class="property-label">${u.name}</span>

                    <span class="property-value" aria-labelledby="userProperties-label">${u.value}</span>
            </li>
                </g:each>

        </g:if>
</div>
<div class="col-md-4">
    <div class="well">

            <span id="activated-label" class="property-label"><g:message code="user.activated.label"
                                                                         default="Activated"/>:</span>

            <span class="property-value" aria-labelledby="activated-label"><g:formatBoolean
                    boolean="${userInstance?.activated}"/></span>
            <br/>

            <span id="locked-label" class="property-label"><g:message code="user.locked.label"
                                                                      default="Locked"/>:</span>

            <span class="property-value" aria-labelledby="locked-label"><g:formatBoolean
                    boolean="${userInstance?.locked}"/></span>
            <br/>

        <h4><g:message code="user.userRoles.label" default="Roles"/></h4>
        <br/>
        <g:if test="${userInstance?.userRoles}">
                <g:each in="${userInstance.userRoles}" var="u">
                    <span class="property-value" aria-labelledby="userRoles-label">
                        <g:link controller="userRole" action="list" params="[role:u?.encodeAsHTML()]">${u?.encodeAsHTML()}</g:link>
                    </span>
                    <br/>
                </g:each>
        </g:if>
        <g:else>
            <p><g:message code="user.show.no.roles" /></p>
        </g:else>

        <br/>
        <g:if test="${userInstance?.tempAuthKey}">
            <p>
                <g:message code="user.show.temp.auth.key" />
            </p>
        </g:if>
    </div>
</div>
</div>

    </ol>
    <g:form>
        <fieldset class="buttons">
            <g:hiddenField name="id" value="${userInstance?.userId}"/>
            <g:link class="edit" action="edit" id="${userInstance?.userId}"><g:message code="default.button.edit.label"
                                                                                   default="Edit"/></g:link>
            %{--<g:actionSubmit class="delete" action="delete"--}%
                            %{--value="${message(code: 'default.button.delete.label', default: 'Delete')}"--}%
                            %{--onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>--}%
        </fieldset>
    </g:form>
</div>
</body>
</html>
