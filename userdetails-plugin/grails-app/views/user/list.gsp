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
    <g:set var="entityName" value="${message(code: 'user.label', default: 'User')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
    <meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},Administration" />
</head>

<body>

<div id="list-user" class="content scaffold-list" role="main">

    <div class="row">
        <div class="col-md-4">
            <h1><g:message code="default.list.label" args="[entityName]"/></h1>
        </div>
        <div class="col-md-8">
            <div class="pull-right">
                <g:form class="form-inline" action="list" controller="user" method="get">
                    <g:link class="btn btn-primary" action="create"><i class="fa fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></g:link>
                    <div class="input-group">
                        <input type="text" class="form-control" name="q" value="${q?:''}" placeholder="Search for user"/>
                        <span class="input-group-btn">
                            <input type="submit" class="btn btn-default"/>
                        </span>
                    </div>
                </g:form>
            </div>
        </div>
        <div class="col-md-12">

    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table class="table table-bordered table-striped table-condensed">
        <thead>
        <tr>
            <g:sortableColumn property="id" title="${message(code: 'user.id.label', default: 'ID')}" mapping="adminUserList"/>

            <g:sortableColumn property="email" title="${message(code: 'user.email.label', default: 'Email')}" mapping="adminUserList"/>

            <g:sortableColumn property="firstName"
                              title="${message(code: 'user.firstName.label', default: 'First Name')}" mapping="adminUserList"/>

            <g:sortableColumn property="lastName"
                              title="${message(code: 'user.lastName.label', default: 'Last Name')}" mapping="adminUserList"/>

            <g:sortableColumn property="activated"
                              title="${message(code: 'user.activated.label', default: 'Activated')}" mapping="adminUserList"/>

            <g:sortableColumn property="locked" title="${message(code: 'user.locked.label', default: 'Locked')}" mapping="adminUserList"/>

            <g:sortableColumn property="dateCreated" title="${message(code: 'user.dateCreated.label', default: 'Created')}" mapping="adminUserList"/>

        </tr>
        </thead>
        <tbody>
        <g:each in="${userInstanceList}" status="i" var="userInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td><g:link action="show"
                            id="${userInstance.userId}">${userInstance.userId}</g:link></td>

                <td><g:link action="show"
                            id="${userInstance.userId}">${fieldValue(bean: userInstance, field: "email")}</g:link></td>

                <td>${fieldValue(bean: userInstance, field: "firstName")}</td>

                <td>${fieldValue(bean: userInstance, field: "lastName")}</td>

                <td><g:formatBoolean boolean="${userInstance.activated}"/></td>

                <td><g:formatBoolean boolean="${userInstance.locked}"/></td>

                <td>${fieldValue(bean: userInstance, field: "dateCreated")}</td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <g:if test="${!q}">
        <div class="text-center">
            <ud:paginate action="list" total="${userInstanceTotal}" nextToken="${nextToken}" params="${params}"/>
        </div>
    </g:if>
        </div>
    </div>
</div>
</body>
</html>
