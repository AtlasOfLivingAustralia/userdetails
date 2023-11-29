<%@ page import="au.org.ala.users.RoleRecord" %>



<div class="form-group fieldcontain ${hasErrors(bean: roleInstance, field: 'role', 'error')} ">
	<label for="role">
		<g:message code="role.role.label" default="Role" />
	</label>
	<g:textField name="role" value="${roleInstance?.role}" class="form-control" data-validation-engine="validate[required]"/>
</div>

<div class="form-group fieldcontain ${hasErrors(bean: roleInstance, field: 'description', 'error')} ">
	<label for="description">
		<g:message code="role.description.label" default="Description" data-validation-engine="validate[required]" />
	</label>
	<g:textField name="description" class="form-control" value="${roleInstance?.description}"/>
</div>

