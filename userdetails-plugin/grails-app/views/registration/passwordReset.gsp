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
    <title><g:message code="password.reset.title" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
<asset:script type="text/javascript">
    $(function(){
        // Used to prevent double clicks from submitting the form twice.  Doing so will result in a confusing
        // message sent back to the user.
        var processingPasswordReset = false;
        var $form = $("form[name='resetPasswordForm']");
        $form.submit(function(event) {

            // Double clicks result in a confusing error being presented to the user.
            if (!processingPasswordReset) {
                processingPasswordReset = true;
                $('#submitResetBtn').attr('disabled','disabled');
                if($('#reenteredPassword').val() != $('#password').val()) {
                    event.preventDefault();
                    processingPasswordReset = false;
                    alert("The supplied passwords do not match!")
                    $('#submitResetBtn').removeAttr('disabled');
                }
            }
            else {
                event.preventDefault();
            }
        });
    });
</asset:script>

<div class="row">
    <h1><g:message code="password.reset.description" /></h1>

    <g:if test="${grailsApplication.config.getProperty('userdetails.features.requirePasswordForUserUpdate', Boolean, true)}">
        <g:render template="passwordPolicy"
              model="[passwordPolicy: passwordPolicy]"/>
    </g:if>

    <g:hasErrors>
    <div class="alert alert-danger">
        <g:eachError var="err">
            <p><g:message error="${err}"/></p>
        </g:eachError>
    </div>
    </g:hasErrors>

    <div class="row">

        <g:form useToken="true" name="resetPasswordForm" controller="registration" action="updatePassword">
            <input id="authKey" type="hidden" name="authKey" value="${authKey}"/>
            <input id="userId" type="hidden" name="userId" value="${user.id}"/>

            <div class="form-group">
                <label for="password">Your new password</label>
                <input id="password" type="password" class="form-control" name="password" value=""/>
            </div>

            <div class="form-group">
                <label for="reenteredPassword"><g:message code="password.reset.re.enter.password" /></label>
                <input id="reenteredPassword" type="password" class="form-control" name="reenteredPassword" value=""/>
            </div>

            <button id="submitResetBtn" class="btn btn-primary"><g:message code="password.reset.set.btn" /></button>
        </g:form>
   </div>
</div>
</body>
</html>
