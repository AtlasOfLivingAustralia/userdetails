<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title>Please supply your new password</title>
    <asset:stylesheet src="application.css" />
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
    <h1>Please supply your new password</h1>

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
                <label for="reenteredPassword">Re-enter Password</label>
                <input id="reenteredPassword" type="password" class="form-control" name="reenteredPassword" value=""/>
            </div>

            <button id="submitResetBtn" class="btn btn-primary">Set my password</button>
        </g:form>
   </div>
</div>
</body>
</html>
