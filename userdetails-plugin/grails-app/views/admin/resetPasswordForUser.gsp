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
    <title>Reset user password |  ${grailsApplication.config.getProperty('skin.orgNameLong')}</title>
    <meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},Administration" />
    <asset:stylesheet src="applicationCss.css" />
</head>
<body>
<div class="row">
    <h1>Reset password for user</h1>

    <g:if test="${emailNotRecognised}">
    <div class="row">
        <div class="col-sm-12">
            <div class="well">
                <p class="text-danger">Email address <strong>${email}</strong> not recognised.</p>
            </div>
        </div>
    </div>
    </g:if>

    <div class="row">
        <div class="col-md-6">
            <g:form action="sendPasswordResetEmail" method="POST">
                <div class="form-group">
                    <label for="email">Email address of user</label>
                    <input id="email" name="email" type="text" class="form-control" value="${params.email ?: email}"/>
                </div>
                <g:submitButton class="btn btn-primary" name="submit" value="Send user password"/>
            </g:form>
        </div>
        <div class="col-md-6">
            <p class="well">
                When you click the Send Password Reset Link button, a one-time link will be emailed to your
                registered email address, allowing you to enter a new password.
                <br/>
                The link will be valid for 48 hours.
            </p>
        </div>
   </div>
</div>
</body>
</html>
