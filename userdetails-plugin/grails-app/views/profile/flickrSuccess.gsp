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
    <title><g:message code="userdetails.my.profile" /> | ${grailsApplication.config.getProperty('skin.orgNameLong')}</title>
    <asset:stylesheet src="applicationCss.css" />
</head>
<body>
<div class="row">
    <h1>Flickr success</h1>
    <table class="table">
        <tr>
            <td>Flickr user ID</td>
            <td><a href="http://www.flickr.com/photos/${user_nsid}">${user_nsid}</a>    </td>
        </tr>
        <tr>
            <td>Flickr images</td>
            <td>http://www.flickr.com/photos/${user_nsid}</td>
        </tr>
        <tr>
            <td>Flickr user name</td>
            <td>${username}</td>
        </tr>
    </table>
</div>
</body>
</html>
