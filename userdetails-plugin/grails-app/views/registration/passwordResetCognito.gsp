<!doctype html>
<html xmlns="http://www.w3.org/1999/html">
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

        <div class="col-md-6">
            <g:form useToken="true" name="resetPasswordForm" controller="registration" action="updateCognitoPassword">
                <input id="email" type="hidden" name="email" value="${email}"/>

                <div class="form-group">
                    <label for="code">Code sent to your email</label>
                    <input id="code" type="number" class="form-control" name="code" value=""/>
                </div>

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
        <div class="col-md-6">
            <div class="well">
                <p><g:message code="updatePassword.string" />
                <ul>
                    <li><g:message code="updatePassword.constraint.1" /></li>
                    <li><g:message code="updatePassword.constraint.2" /></li>
                    <li><g:message code="updatePassword.constraint.3" /></li>
                    <li><g:message code="updatePassword.constraint.4" /></li>
                    <li><g:message code="updatePassword.constraint.5" /></li>
                </ul>
            </div>
        </div>
   </div>
</div>
</body>
</html>
