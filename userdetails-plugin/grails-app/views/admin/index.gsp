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
    <title>User Administration | ${grailsApplication.config.getProperty('skin.orgNameLong')}</title>
    <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    <asset:stylesheet src="userdetails.css" />
</head>
<body>
    <div class="row">
        <div class="col-md-12" id="page-body" role="main">
            <h1>User Administration</h1>
            <div class="row userdetails-grid">
                <div class="col-md-3">
                    <div class="thumbnail">
                        <div class="image">
                            <i class="glyphicon glyphicon-user"></i>
                        </div>
                        <div class="caption">
                            <h3>Find a user</h3>
                            <g:link controller="user" action="list" class="btn btn-primary">Find a user</g:link>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="thumbnail">
                        <div class="image">
                            <i class="glyphicon glyphicon-lock"></i>
                        </div>
                        <div class="caption">
                            <h3>Reset user password</h3>
                            <g:link controller="admin" action="resetPasswordForUser" class="btn btn-primary">Reset user password</g:link>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="thumbnail">
                        <div class="image">
                            <i class="glyphicon glyphicon-wrench"></i>
                        </div>
                        <div class="caption">
                            <h3>Roles</h3>
                            <g:link controller="role" action="list" class="btn btn-primary">Roles</g:link>
                        </div>
                    </div>
                </div>
                <!-- TODO have these contributed by implementations instead of controlled by feature flags -->
                <g:if test="${grailsApplication.config.getProperty('userdetails.features.authorisedSystems', Boolean, false)}">
                    <div class="col-md-3">
                        <div class="thumbnail">
                            <div class="image">
                                <i class="glyphicon glyphicon-inbox"></i>
                            </div>
                            <div class="caption">
                                <h3>Authorised systems</h3>
                                <g:link controller="authorisedSystem" action="list" class="btn btn-primary">Authorised systems</g:link>
                            </div>
                        </div>
                    </div>
                </g:if>
                <g:if test="${grailsApplication.config.getProperty('userdetails.features.bulkCreate', Boolean, false)}">
                    <div class="col-md-3">
                        <div class="thumbnail">
                            <div class="image">
                                <i class="glyphicon glyphicon-upload"></i>
                            </div>
                            <div class="caption">
                                <h3>Bulk create user accounts</h3>
                                <g:link controller="admin" action="bulkUploadUsers" class="btn btn-primary">Bulk create user accounts</g:link>
                            </div>
                        </div>
                    </div>
                </g:if>
                <g:if test="${grailsApplication.config.getProperty('userdetails.features.exportUsers', Boolean, false)}">
                    <div class="col-md-3">
                        <div class="thumbnail">
                            <div class="image">
                                <i class="glyphicon glyphicon-download"></i>
                            </div>
                            <div class="caption">
                                <h3>Export users to CSV file</h3>
                                <g:link controller="admin" action="exportUsers" class="btn btn-primary">Export users to CSV file</g:link>
                            </div>
                        </div>
                    </div>
                </g:if>
                <g:if test="${grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)}">
                    <div class="col-md-3">
                        <div class="thumbnail">
                            <div class="image">
                                <i class="glyphicon glyphicon-th-list"></i>
                            </div>
                            <div class="caption">
                                <h3>Get user survey results</h3>
                                <g:link controller="admin" action="surveyResults" class="btn btn-primary">Get user survey results</g:link>
                            </div>
                        </div>
                    </div>
                </g:if>
                <div class="col-md-3">
                    <div class="thumbnail">
                        <div class="image">
                            <i class="glyphicon glyphicon-asterisk"></i>
                        </div>
                        <div class="caption">
                            <h3>ALA admin page</h3>
                            <g:link controller="alaAdmin" action="index" class="btn btn-primary">ALA admin page</g:link>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
