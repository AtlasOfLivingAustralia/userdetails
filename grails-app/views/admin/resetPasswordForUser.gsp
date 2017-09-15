<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title>Reset my password |  ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
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
