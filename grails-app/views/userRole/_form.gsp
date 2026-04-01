<%@ page import="au.org.ala.userdetails.UserRole" %>



<div class="mb-3 fieldcontain ${hasErrors(bean: userRoleInstance, field: 'role', 'error')} required">
	<label for="role">
		<g:message code="userRole.role.label" default="Role" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="role" name="role.id" from="${au.org.ala.userdetails.Role.list()}" optionKey="id" required="" value="${userRoleInstance?.role?.id}" class="form-control many-to-one"/>
</div>

<div class="mb-3 fieldcontain ${hasErrors(bean: userRoleInstance, field: 'user', 'error')} required">
	<label for="user">
		<g:message code="userRole.user.label" default="User" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="user" name="user.id" from="${au.org.ala.userdetails.User.list()}" optionKey="id" required="" value="${userRoleInstance?.user?.id}" class="form-control many-to-one"/>
</div>

