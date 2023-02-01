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
		<title><g:message code="default.create.label" args="[entityName]" /></title>
		<asset:stylesheet src="userdetails.css" />
		<meta name="breadcrumbParent" content="${createLink(controller:'user', action:'list')},UserRecord list" />
	</head>
	<body>
	<div class="row">
		<div class="col-sm-12">
			<div id="create-userRole" class="content scaffold-create" role="main">
				<h1>Add role for ${user}</h1>
				<g:form action="addRole" >
					<fieldset class="form">
						<input type="hidden" id="userId" name="userId" value="${user.id}"/>
						<div class="form-group">
							<label for="role">
								<g:message code="role.label" default="Role" />
							</label>
							<g:select id="role" name="role.id" from="${roles}"
									  optionKey="role"  value="" class="form-control many-to-one"/>
						</div>

					</fieldset>
					<fieldset class="buttons">
						<g:submitButton name="add" class="btn btn-primary" value="${message(code: 'default.button.add.label', default: 'Add role')}" />
					</fieldset>
				</g:form>
			</div>
		</div>
	</div>
	</body>
</html>
