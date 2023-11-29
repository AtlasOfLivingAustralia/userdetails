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
    <title><g:message code="account.disabled.title" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="account.disabled.successfully" /></h1>
    <div class="row">
        <div class="col-md-12">
            <p class="well">
                <g:message code="account.disabled.description" />
            </p>
        </div>
   </div>
</div>
</body>
</html>
