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
    <title><g:message code="default.create.label" args="[entityName]"/></title>
    <meta name="breadcrumbParent" content="${createLink(controller:'user', action:'list')},UserRecord List" />
    <asset:stylesheet src="applicationCss.css" />
    <asset:stylesheet src="createAccount.css" />
</head>

<body>
<div class="row">
    <div class="col-sm-12">
        <div id="create-user" class="content scaffold-create" role="main">
            <h1><g:message code="default.create.label" args="[entityName]"/></h1>
            <g:if test="${flash.message}">
                <div class="message" role="status">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${userInstance}">
                <ul class="errors" role="alert">
                    <g:eachError bean="${userInstance}" var="error">
                        <li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message
                                error="${error}"/></li>
                    </g:eachError>
                </ul>
            </g:hasErrors>
            <g:form action="save">
                <fieldset class="form">
                    <g:render template="form"/>
                </fieldset>
                <fieldset class="buttons">
                    <g:submitButton name="create" class="btn btn-primary"
                                    value="${message(code: 'default.button.create.label', default: 'Create')}"/>
                </fieldset>
            </g:form>
        </div>
    </div>
</div>
<asset:javascript src="createAccount.js"/>
<asset:script type="text/javascript">
    $(function() {
        userdetails.initCountrySelect('.chosen-select', '#country', '#state', "${g.createLink(uri: '/ws/registration/states')}");
    });
</asset:script>
</body>
</html>
