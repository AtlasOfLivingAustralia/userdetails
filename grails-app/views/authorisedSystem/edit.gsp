<%@ page import="au.org.ala.userdetails.AuthorisedSystem" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="${grailsApplication.config.skin.layout}">
		<g:set var="entityName" value="${message(code: 'authorisedSystem.label', default: 'AuthorisedSystem')}" />
		<title><g:message code="default.edit.label" args="[entityName]" /></title>
		<meta name="breadcrumbParent" content="${g.createLink(action:"list")},${g.message(code:"default.list.label", args:[entityName])}" />
		<asset:stylesheet src="application.css" />
	</head>
	<body>
		<div class="row">
			<div class="col-sm-8">
				<div id="edit-authorisedSystem" class="content scaffold-edit" role="main">
					<h1><g:message code="default.edit.label" args="[entityName]" /></h1>
					<g:if test="${flash.message}">
						<div class="message" role="status">${flash.message}</div>
					</g:if>
					<g:hasErrors bean="${authorisedSystemInstance}">
						<ul class="errors" role="alert">
							<g:eachError bean="${authorisedSystemInstance}" var="error">
								<li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
							</g:eachError>
						</ul>
					</g:hasErrors>
					<g:form method="post" >
						<g:hiddenField name="id" value="${authorisedSystemInstance?.id}" />
						<g:hiddenField name="version" value="${authorisedSystemInstance?.version}" />
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
