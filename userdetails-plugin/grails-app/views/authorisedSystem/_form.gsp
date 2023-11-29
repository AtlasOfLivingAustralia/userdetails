<%@ page import="au.org.ala.users.AuthorisedSystemRecord" %>

<div class="form-group fieldcontain ${hasErrors(bean: authorisedSystemInstance, field: 'host', 'error')} ">
	<label for="host">
		<g:message code="authorisedSystem.host.label" default="Host" />
		
	</label>
	<g:textField class="form-control" name="host" value="${authorisedSystemInstance?.host}"/>
</div>

<div class="form-group fieldcontain ${hasErrors(bean: authorisedSystemInstance, field: 'description', 'error')} ">
	<label for="description">
		<g:message code="authorisedSystem.description.label" default="Description" />
	</label>
	<g:textField class="form-control" name="description" value="${authorisedSystemInstance?.description}"/>
</div>

