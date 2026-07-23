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
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <meta name="section" content="home"/>
    <title><g:message code="userdetails.index.accounts" args="[grailsApplication.config.getProperty('skin.orgNameShort')]" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
<div class="row">
    <div class="col-md-12" id="page-body" role="main">

        <g:if test="${flash.errorMessage || flash.message}">
            <div class="col-md-12">
                <div class="alert alert-danger">
                    <button class="btn-close" data-bs-dismiss="alert" onclick="$('.alert').fadeOut();" href="#">×</button>
                    ${flash.errorMessage?:flash.message}
                </div>
            </div>
        </g:if>

        <h1><g:message code="userdetails.index.accounts" args="[grailsApplication.config.getProperty('skin.orgNameShort')]" /></h1>

        <div class="row g-4 userdetails-grid">
            <div class="col-md-3">

                <div class="card">
                    <div class="card-body">
                        <div>
                            <i class="fas fa-plus-circle fs-1 pb-4"></i>
                        </div>
                        <div class="caption">
                            <h3 class="card-title"><g:message code="userdetails.index.create.new.account" /></h3>
                            <g:link controller="registration" action="createAccount" class="btn btn-primary"><g:message code="userdetails.index.create.new.account" /></g:link>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-md-3">
                <div class="card">
                    <div class="card-body">
                        <div>
                            <i class="fas fa-lock fs-1 pb-4"></i>
                        </div>
                        <div class="caption">
                            <h3 class="card-title"><g:message code="userdetails.index.reset.password" /></h3>
                            <g:link controller="registration" action="forgottenPassword" class="btn btn-primary"><g:message code="userdetails.index.reset.password" /></g:link>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-md-3">
                <div class="card">
                    <div class="card-body">
                        <div>
                            <i class="fas fa-user fs-1 pb-4"></i>
                        </div>
                        <div class="caption">
                            <h3 class="card-title"><g:message code="userdetails.index.my.profile" /></h3>
                            <g:link controller="profile" class="btn btn-primary"><g:message code="userdetails.index.my.profile" /></g:link>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
    <auth:ifAllGranted roles="ROLE_ADMIN">
        <div class="col-md-12 text-end mt-3">
            <g:link class="fw-bold text-decoration-none" style="color:#DDDDDD;" controller="admin"><g:message code="userdetails.index.admin.tools" args="[grailsApplication.config.getProperty('skin.orgNameShort')]" /></g:link>
        </div>
    </auth:ifAllGranted>
</div>
</body>
</html>
