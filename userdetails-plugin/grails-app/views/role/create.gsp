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
		<title><g:message code="default.create.label" args="[entityName]" /></title>
		<asset:stylesheet src="jqueryValidationEngine.css" />
		<asset:stylesheet src="userdetails.css" />
		<asset:javascript src="jqueryValidationEngine.js" asset-defer="" />
        <asset:script type="text/javascript">
            $(function(){
                $('#validation-container').validationEngine('attach', {scroll: false});
                $("#saveRoleForm").submit(function() {
                   var valid = $('#validation-container').validationEngine('validate');
                   if(!valid) {
                     event.preventDefault();
                   }
                });
            });
        </asset:script>
		<meta name="breadcrumbParent" content="${createLink(controller:'role', action:'list')},Role list" />
	</head>
	<body>
	<div class="row">
		<div class="col-sm-12">
			<div id="create-role" class="content scaffold-create" role="main">
				<h1><g:message code="default.create.label" args="[entityName]" /></h1>
				<g:if test="${flash.message}">
					<div class="message well warning text-danger" role="status">${flash.message}</div>
				</g:if>
				<g:if test="${error}">
					<div class="alert alert-danger" role="alert">${error}</div>
				</g:if>
				<g:hasErrors bean="${roleInstance}">
					<div class="well warning">
						<ul class="errors" role="alert">
							<g:eachError bean="${roleInstance}" var="error">
								<li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
							</g:eachError>
						</ul>
					</div>
				</g:hasErrors>

				<div id="validation-container" class="validation-container">
					<g:form action="save" id="saveRoleForm">
						<fieldset class="form">
							<g:render template="form"/>
						</fieldset>
						<fieldset class="buttons">
							<g:submitButton name="create" class="btn btn-primary" value="${message(code: 'default.button.create.label', default: 'Create')}" />
						</fieldset>
					</g:form>
				</div>
			</div>
		</div>
	</div>
	</body>
</html>
