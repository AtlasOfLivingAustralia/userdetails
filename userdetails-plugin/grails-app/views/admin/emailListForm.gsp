%{--
  - Copyright (C) 2023 Atlas of Living Australia
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
    <g:set var="title">Export Users to CSV</g:set>
    <title>${title} | ${grailsApplication.config.getProperty('skin.orgNameLong')}</title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
<g:if test="${flash.message}">
    <div class="alert alert-danger">
        ${flash.message}
    </div>
</g:if>

<div class="row">
    <div class="col-md-12" id="page-body" role="main">
        <g:form name="emailList" action="emailList" method="get"  class="form-horizontal">
            <div class="form-group">
                <label for="start_date">Start Date</label>
                <input type="date" class="form-control" id="start_date" >
%{--                <g:datePicker name="start_date" value="${new Date()}"/>--}%
            </div>
            <div class="form-group">
                <label for="end_date">End Date</label>
                <input type="date" class="form-control" id="end_date" >
%{--                <g:datePicker name="end_date" value="${new Date()}"/>--}%
            </div>
            <button type="submit" class="btn btn-default">Submit</button>
        </g:form>
    </div>
</div>
</body>
</html>