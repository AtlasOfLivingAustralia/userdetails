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
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    <title><g:message code="myprofile.myClientAndApikey" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
<div id="myClientAndApikey">
    <h1><g:message code="myprofile.myClientAndApikey" /></h1>
    <h5><g:message code="myprofile.myClientAndApikey.about" /> <a href="https://docs.ala.org.au" target="_blank">docs.ala.org.au</a>.</h5>
    <br/>
    <ul class="nav nav-tabs">
        <li class="active"><a  data-toggle="tab" href="#ex1-tabs-1"><g:message code="myprofile.my.client" /></a></li>
        <li ><a data-toggle="tab" href="#ex1-tabs-2"><g:message code="myprofile.my.apikey" /></a></li>
    </ul>

    <div class="tab-content">
        <div id="ex1-tabs-1" class="tab-pane fade in active">
            <g:if test="${errors}">
                <div class="alert alert-danger">
                    <g:each in="${errors}" var="err">
                        <p><g:message error="${err}"/></p>
                    </g:each>
                </div>
            </g:if>
            <g:if test="${grailsApplication.config.getProperty('oauth.support.dynamic.client.registration', Boolean, false)}">
                <div class="row">
                    <div class="col-md-8">
                        <g:form name="generateClientForm" controller="profile" action="generateClient">
                            <br/>
                            <p><g:message code="myclient.desc.1" /></p>
                            <p><g:message code="myclient.desc.2" /></p>
                            <br/>
                            <g:if test="${clientId}">
                                <g:message code="my.client.id" /><code>${clientId}</code>
                                <br/>
                                <br/>
                                <p><g:message code="myclient.desc.3" /> <a href="${grailsApplication.config.getProperty('oauth.support.dynamic.client.postmanExample')}" target="_blank">Postman example</a>.</p>
                                <p>You can also use the ALA <a href="${grailsApplication.config.getProperty('tokenApp.url')}?step=generation&client_id=${clientId}" target="_blank">tokens app</a> to generate an access token.</p>
                            </g:if>
                            <g:else>

                            %{-- TODO  un-hide after full client app management is implemented. Currently susing default callback url list and galah callback url list for all use cases  - i.e for both galah and non-galah usage--}%
                                <div class="form-group" hidden>
                                    <div class="checkbox">
                                        <label>
                                            <g:checkBox checked="true" name="forGalah"/> Is this for Galah?
                                        </label>
                                    </div>
                                </div>
                                 <p>For generation of full detailed clients with callback urls and custom scopes, please use the ALA <a href="${grailsApplication.config.getProperty('tokenApp.url')}?step=registration" target="_blank">tokens app</a> instead.</p>
                                <button id="generateClient" class="btn btn-primary"><g:message code="myprofile.generate.client" /> </button>
                            </g:else>
                        </g:form>
                    </div>
                    <g:if test="${clientId}">
                        <div class="col-md-4">
                            <p style="margin-top: 10px" class="well">For further documentation on authentication and authorisation, please see ALA API <a href="https://docs.ala.org.au/#authentication-code-flow" target="_blank">authentication docs</a>.</p>
                        </div>
                    </g:if>
                </div>
                <br/>
            </g:if>
        </div>
        <div id="ex1-tabs-2" class="tab-pane fade">
            <div class="row">
                <div class="col-md-8">
                    <br/>
                    <p><g:message code="myprofile.my.apikey.about" /> <a href="https://galah.ala.org.au" target="_blank">galah.ala.org.au</a>.</p>
                    <p><g:message code="myprofile.my.apikey.desc" /></p>
                    <g:form name="generateApikeyForm" controller="profile" action="generateApikey" params="[application:'galah']">
                        <br/>
                        <g:if test="${apikeys}">
                            <g:message code="my.apikey" /><code>${apikeys}</code>
                        </g:if>
                        <g:else>
                            <button id="generateApikey" class="btn btn-primary"><g:message code="myprofile.generate.apikey" /></button>
                        </g:else>
                    </g:form>
                </div>
                <div class="col-md-4">
                    <p style="margin-top: 10px" class="well"><g:message code="generate.apikey.desc.1" /></p>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>