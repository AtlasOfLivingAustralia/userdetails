<%@ page import="au.org.ala.userdetails.UserRole" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="${grailsApplication.config.skin.layout}">
		<g:set var="entityName" value="${message(code: 'userRole.label', default: 'UserRole')}" />
		<title><g:message code="default.edit.label" args="[entityName]" /></title>
		<meta name="breadcrumbParent" content="${g.createLink(action:"list")},${g.message(code:"default.list.label", args:[entityName])}" />
		<asset:stylesheet src="application.css" />
	</head>
	<body>
	<div class="row">
		<div class="col-sm-12">
			<div id="edit-userRole" class="content scaffold-edit" role="main">
				<h1><g:message code="default.edit.label" args="[entityName]" /></h1>
				<g:if test="${flash.message}">
					<div class="message" role="status">${flash.message}</div>
				</g:if>
				<g:hasErrors bean="${userRoleInstance}">
					<ul class="errors" role="alert">
						<g:eachError bean="${userRoleInstance}" var="error">
							<li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
						</g:eachError>
					</ul>
				</g:hasErrors>
				<g:form method="post" >
					<g:hiddenField name="id" value="${userRoleInstance?.id}" />
					<g:hiddenField name="version" value="${userRoleInstance?.version}" />
					<fieldset class="form">
						<g:render template="form"/>
					</fieldset>
					<fieldset class="buttons">
						<g:actionSubmit class="btn btn-primary save" action="update" value="${message(code: 'default.button.update.label', default: 'Update')}" />
						<g:actionSubmit class="btn btn-danger delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" formnovalidate="" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
					</fieldset>
				</g:form>
			</div>
		</div>
	</div>
	</body>
</html>
