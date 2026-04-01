
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

<%@ page import="au.org.ala.userdetails.UserRole" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}">
		<g:set var="entityName" value="${message(code: 'userRole.label', default: 'UserRole')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
		<meta name="breadcrumbParent" content="${g.createLink(action:"list")},${g.message(code:"default.list.label", args:[entityName])}" />
		<asset:stylesheet src="application.css" />
	</head>
	<body>
		<div class="row">
			<div class="col-sm-12">
				<div id="show-userRole" class="content scaffold-show" role="main">
					<h1><g:message code="default.show.label" args="[entityName]" /></h1>
					<g:if test="${flash.message}">
						<div class="alert alert-info" role="status">${flash.message}</div>
					</g:if>
					<ol class="property-list userRole">

						<g:if test="${userRoleInstance?.role}">
							<li class="fieldcontain">
								<span id="role-label" class="property-label"><g:message code="userRole.role.label" default="Role" /></span>

								<span class="property-value" aria-labelledby="role-label"><g:link controller="role" action="show" id="${userRoleInstance?.role?.id}">${userRoleInstance?.role?.encodeAsHTML()}</g:link></span>

							</li>
						</g:if>

						<g:if test="${userRoleInstance?.user}">
							<li class="fieldcontain">
								<span id="user-label" class="property-label"><g:message code="userRole.user.label" default="User" /></span>

								<span class="property-value" aria-labelledby="user-label"><g:link controller="user" action="show" id="${userRoleInstance?.user?.id}">${userRoleInstance?.user?.encodeAsHTML()}</g:link></span>

							</li>
						</g:if>

					</ol>
					<g:form>
						<fieldset class="buttons">
							<g:hiddenField name="id" value="${userRoleInstance?.id}" />
							<g:link class="edit" action="edit" id="${userRoleInstance?.id}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
							<g:actionSubmit class="btn btn-danger delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
						</fieldset>
					</g:form>
				</div>
			</div>
		</div>
	</body>
</html>
