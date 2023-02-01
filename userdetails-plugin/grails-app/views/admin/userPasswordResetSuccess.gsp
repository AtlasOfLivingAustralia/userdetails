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
    <title>Please check your email | ${grailsApplication.config.getProperty('skin.orgNameLong')} </title>
    <meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},Administration" />
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
<div class="row">
    <h1>User password reset successfully</h1>
    <div class="row">
        <div class="col-md-12">
            <p class="well">
                The password has been reset for <strong>${email}</strong> and an email has been sent to the user containing the new password.
            </p>
            <p class="well">
                The new password: <strong>${password}</strong>
            </p>
        </div>
   </div>
</div>
</body>
</html>
