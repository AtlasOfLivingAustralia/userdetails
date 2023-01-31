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
    <title><g:message code="auth.key.expired.title" /></title>
    <asset:stylesheet src="applicationCss.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="auth.key.expired.header" /></h1>
    <div class="row">
        <p>
            <g:message code="auth.key.expired.initial.description" /><br/>
            If you have completed the <g:link controller="registration" action="forgottenPassword">Reset your password</g:link> form recently, please check your email for a new email with a link that will take your through to page where you can provide a new password.
            <br/>
            Alternatively you can start the process again <g:link controller="registration" action="forgottenPassword">here</g:link>.

            <br/>
            <g:message code="auth.key.expired.mailto" args="[grailsApplication.config.getProperty('supportEmail')]" />
        </p>
   </div>
</div>
</body>
</html>
