
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

<%@ page import="au.org.ala.users.RoleRecord" %>
<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}">
	<g:set var="entityName" value="${message(code: 'role.label', default: 'Role')}" />
	<title><g:message code="default.list.label" args="[entityName]" /></title>
	<meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},Administration" />
	<asset:stylesheet src="userdetails.css" />
</head>
<body>
<div id="list-role" class="content container-fluid content scaffold-list" role="main">
	<div class="row align-items-center">
		<div class="col-sm-4">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
		</div>
		<div class="col-sm-8">
			<div class="d-flex justify-content-md-end mt-2 mt-md-0">
				<g:link class="btn btn-primary" action="create"><i class="fas fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></g:link>
			</div>
		</div>
		<div class="col-sm-12">
			<g:if test="${flash.message}">
				<div class="alert alert-info" role="status">${flash.message}</div>
			</g:if>
			<table class="table table-bordered table-striped align-middle mb-3">
				<thead>
				<tr>
					<g:sortableColumn property="role" title="${message(code: 'role.label', default: 'Role')}" mapping="adminRoleList"/>
					<g:sortableColumn property="description" title="${message(code: 'role.description.label', default: 'Description')}" mapping="adminRoleList"/>

					<th><g:message code="user.list.actions" /></th>
				</tr>
				</thead>
				<tbody>
				<g:each in="${roleInstanceList}" status="i" var="roleInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

						<td>${fieldValue(bean: roleInstance, field: "role")}</td>

						<td>${fieldValue(bean: roleInstance, field: "description")}</td>

						<td><a href="${createLink(controller: 'userRole', action:'list', params:[role:roleInstance.role])}">View users</a></td>
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="d-flex justify-content-center">
				<ud:paginate action="list" total="${roleInstanceTotal}" nextToken="${nextToken}" params="${params}" />
			</div>
		</div>
	</div>
</div>
</body>
</html>
