<!doctype html>
<html>
    <head>
        <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
        <meta name="section" content="home"/>
        <g:set var="title">Bulk Load Users</g:set>
        <title>${title} | ${grailsApplication.config.skin.orgNameLong}</title>
        <asset:stylesheet src="application.css" />
    </head>
    <body>
        <g:if test="${flash.message}">
            <div class="alert alert-danger">
                ${flash.message}
            </div>
        </g:if>

        <div class="row">
            <div class="col-md-12" id="page-body" role="main">
                <h1>Bulk Load Users</h1>
                <p>
                Choose a CSV file to load. The file should be in the following format:
                </p>
                <p>
                    <code>
                    email_address,first_name,surname,roles
                    </code>
                </p>
                <p>
                Where <code>roles</code> is an optional space separated list of the roles that the user should have.
                </p>
                <p>
                    <em>Note:</em> If an email address in the file already exists in the database then that user will not be created, but any missing roles will be added to the existing account.
                </p>
            </div>
        </div>
        <g:form action="loadUsersCSV" method="post" enctype="multipart/form-data" class="form-horizontal well well-small">
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <input type="file" name="userList" />
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <div class="checkbox">
                        <label>
                            <g:checkBox name="firstRowHasFieldNames"/> First row contains field names
                        </label>
                    </div>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label" for="primaryUsage">
                    Primary usage (will default to 'Not Supplied')
                </label>

                <div class="col-sm-10">
                    <g:textField class="form-control" name="primaryUsage" />
                </div>
            </div>

            <h4>Password Reset Email</h4>
            <div class="form-group">
            An email will be sent out to each user created, prompting them to reset their password.
            You can customize the content of this email by filling out the fields below, or you can leave them blank to use the default wording.
            </div>

            <div class="form-group">
                <label class="col-sm-2 control-label" for="emailSubject">
                    Subject
                </label>
                <div class="col-sm-10">
                    <g:textField name="emailSubject" class="form-control" />
                </div>
            </div>

            <div class="form-group">
                <label class="col-sm-2 control-label" for="emailTitle">
                    Title
                </label>
                <div class="col-sm-10">
                    <g:textField name="emailTitle" class="form-control"/>
                </div>
            </div>

            <div class="form-group">
                <label class="col-sm-2 control-label" for="emailBody">
                    Body
                </label>
                <div class="col-sm-10">
                    <g:textArea name="emailBody" class="form-control" rows="5"/>
                </div>
            </div>

            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <button class="btn btn-primary">Upload</button>
                </div>
            </div>
        </g:form>
    </body>
</html>
