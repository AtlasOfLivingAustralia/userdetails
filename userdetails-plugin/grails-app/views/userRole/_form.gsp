<%@ page import="au.org.ala.users.UserRoleRecord" %>



<div class="form-group fieldcontain ${hasErrors(bean: userRoleInstance, field: 'role', 'error')} required">
	<label for="role">
		<g:message code="role.label" default="Role" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="role" name="role.id" from="${ud.roleList()}" optionKey="id" required="" value="${userRoleInstance?.role?.id}" class="form-control many-to-one"/>
</div>

<div class="form-group fieldcontain ${hasErrors(bean: userRoleInstance, field: 'user', 'error')} required">
	<label for="user">
		<g:message code="user.label" default="User" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="user" name="user.id" from="${ud.userList()}" optionKey="id" required="" value="${userRoleInstance?.user?.id}" class="form-control many-to-one"/>
</div>

