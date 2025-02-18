
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

<%@ page import="au.org.ala.users.UserRoleRecord" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}">
		<g:set var="entityName" value="${message(code: 'userRole.label', default: 'UserRoleRecord')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
		<meta name="breadcrumbParent" content="${createLink(controller:'role', action:'list')},Role list" />
	</head>
	<body>
		<div class="row">
			<div class="col-sm-12">
				<div id="list-userRole" class="content scaffold-list" role="main">
					<h1><g:message code="default.list.label" args="[entityName]" /></h1>
					<g:if test="${flash.message}">
						<div class="message" role="status">${flash.message}</div>
					</g:if>
					<table class="table table-bordered table-striped table-condensed">
						<thead>
						<tr>
							<th><g:message code="userRole.user.label" default="ID" /></th>
							<th><g:message code="userRole.user.label" default="User" /></th>
							<th><g:message code="userRole.role.label" default="Role" /></th>
						</tr>
						</thead>
						<tbody>
						<g:each in="${userRoleInstanceList}" status="i" var="userRoleInstance">
							<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

								<td>
									<g:link controller="user" action="show" id="${userRoleInstance.user.id}">
										${fieldValue(bean: userRoleInstance, field: "user.id")}
									</g:link>
								</td>

								<td>${fieldValue(bean: userRoleInstance, field: "user")}</td>

								<td>${fieldValue(bean: userRoleInstance, field: "role")}</td>
							</tr>
						</g:each>
						</tbody>
					</table>
					<div class="text-center">
						<ud:paginate action="list" total="${userRoleInstanceTotal}" nextToken="${nextToken}" params="${params}" />
					</div>
				</div>
			</div>
		</div>
	</body>
</html>
