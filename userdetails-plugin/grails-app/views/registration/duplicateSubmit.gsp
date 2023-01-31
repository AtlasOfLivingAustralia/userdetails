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
    <title><g:message code="duplicate.submit.title" /></title>
    <asset:stylesheet src="applicationCss.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="duplicate.submit.results" /></h1>
    <div class="row">
        <div class="alert alert-warning">
            <g:message code="duplicate.submit.description.new.password" />
        </div>

        <strong><g:message code="duplicate.submit.next.steps" /></strong>
        <ul class="userdetails-menu">
            <li>
                <g:message code="duplicate.submit.reset.successfully" args="[g.createLink(controller: 'login')]" />
            </li>
            <li>
                If your new password doesn't work, please start the process again <g:link controller="registration" action="forgottenPassword">here</g:link>.
            </li>
            <li>
                <g:message code="duplicate.submit.mailto" args="[grailsApplication.config.getProperty('supportEmail')]" />
            </li>
        </ul>

   </div>
</div>
</body>
</html>
