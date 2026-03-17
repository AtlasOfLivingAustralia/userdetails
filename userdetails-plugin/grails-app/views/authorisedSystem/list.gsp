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

<%@ page import="au.org.ala.users.AuthorisedSystemRecord" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}">
		<g:set var="entityName" value="${message(code: 'authorisedSystem.label', default: 'AuthorisedSystem')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
        <meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},Administration" />
        <asset:stylesheet src="userdetails.css" />
	</head>
	<body>
		<div id="list-authorisedSystem" class="content scaffold-list" role="main">
			<h2><g:message code="default.list.label" args="[entityName]" /></h2>
			<g:if test="${flash.message}">
			<div class="alert alert-info" role="status">${flash.message}</div>
			</g:if>
            <div class="row mb-3">
                <div class="col-md-8">
                    <div class="d-flex justify-content-end">
                        <div class="mb-3">
                            <g:if test="${grailsApplication.config.getProperty('authorised-systems.edit-enabled', boolean, true)}">
                                <g:link class="btn btn-primary" action="create"><i class="fas fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></g:link>
                            </g:if>
                            <g:else>
                                <g:link class="btn btn-primary" action="list" params="[reload: true]"><g:message code="reload.config"/></g:link>
                            </g:else>
                            <div class="mb-3" style="display: inline-block;">
                                <label class="visually-hidden" for="q">Query</label>
                                <g:textField name="q" class="form-control" value="${params.q}" />
                            </div>
                            <button type="button" class="btn btn-outline-dark" id="btnSearch">Search</button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-md-8">
                    <table class="table table-bordered table-striped table-sm align-middle">
                        <thead>
                            <tr>
                                <g:sortableColumn property="host" title="${message(code: 'authorisedSystem.host.label', default: 'Host')}" />
                                <th>Hostname</th>
                                <th>Description</th>
                                <g:if test="${grailsApplication.config.getProperty('authorised-systems.edit-enabled', boolean, true)}">
                                    <th></th>
                                </g:if>
                            </tr>
                        </thead>
                        <tbody>
                        <g:each in="${authorisedSystemInstanceList}" status="i" var="authorisedSystemInstance">
                            <tr>
                                <td><g:link action="show" id="${authorisedSystemInstance.id}">${fieldValue(bean: authorisedSystemInstance, field: "host")}</g:link></td>
                                <td><div class="hostname" host="${authorisedSystemInstance.host}"><i class="fas fa-cog fa-spin"></i></div></td>
                                <td>${authorisedSystemInstance.description}</td>
                                <g:if test="${grailsApplication.config.getProperty('authorised-systems.edit-enabled', boolean, true)}">
                                    <td>
                                    <a href="${createLink(action:'edit', id:authorisedSystemInstance.id)}" class="btn btn-outline-dark btn-sm"><i class="fas fa-edit"></i></a>
                                    </td>
                                </g:if>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                    <div class="d-flex justify-content-center">
                        <hf:paginate total="${authorisedSystemInstanceTotal}" params="${params}" />
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="alert alert-well">
                        This is a list of IP address that can access the web services providing user information.
                        Requests from IP addresses not listed here will get a HTTP 403 Forbidden response.
                    </div>
                </div>
            </div>
		</div>
	</body>
    <asset:script type="text/javascript">

    function doSearch() {
        var query = $("#q").val();
        window.location = "${createLink(action:'list')}?q=" + query
    }

    $(document).ready(function() {

        $("#q").keydown(function(e) {
            if (e.which == 13) {
                e.preventDefault();
                doSearch();
            }
        }).focus();

        $("#btnSearch").click(function(e) {
            e.preventDefault();
            doSearch();
        });

        $(".hostname").each(function() {
            var host = $(this).attr("host");
            var target = $(this); // create a copy of current scope
            if (host) {
                $.ajax("${createLink(action:'ajaxResolveHostName')}?host=" + host).done(function(results) {
                    var iconClass= results.reachable ? "fas fa-check" : "fas fa-exclamation-triangle";
                    var tooltip = results.reachable ? "Host is reachable" : "Host is not currently reachable";
                    target.html(results.hostname + "&nbsp;<i title='" + tooltip + "' class='" +  iconClass + "'></i>");
                });
            }
        });

    });

    </asset:script>
</html>
