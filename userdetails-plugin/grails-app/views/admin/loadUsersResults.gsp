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
        <meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},User Details Administration" />
        <g:set var="title">Bulk Load Users Results |  ${grailsApplication.config.getProperty('skin.orgNameLong')} </g:set>
        <title>${title}</title>
        <asset:stylesheet src="applicationCss.css" />
    </head>
    <body>

        <g:if test="${flash.message}">
            <div class="alert alert-danger">
                ${flash.message}
            </div>
        </g:if>

        <div class="row">
            <div class="col-md-12" id="page-body" role="main">
                <h1>Bulk Load Users Results</h1>
                <p>
                    <g:if test="${results.success}">
                        Success!
                        <g:if test="${results.failedRecords}">
                            Although some rows where rejected. See below for more detail.
                        </g:if>
                    </g:if>
                    <g:else>
                        Failed! - ${results.message}
                    </g:else>
                </p>
                <h3>Upload results</h3>
                <div class="row">
                    <div class="col-md-4">
                        <table class="table table-bordered">
                            <tbody>
                                <tr>
                                    <td>User accounts created</td>
                                    <td>${results.userAccountsCreated} (with ${results.warnings?.size() ?: 0} warning(s))</td>
                                </tr>
                                <tr>
                                    <td>Rows rejected</td>
                                    <td>${results.failedRecords?.size() ?: 0}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <g:each in="${[[title:'Errors (Users not created)', collection: results.failedRecords], [title:'Warnings', collection:results.warnings]]}" var="messageCollection">
                    <g:if test="${messageCollection.collection}">
                        <h3>${messageCollection.title}</h3>
                        <table class="table table-bordered table-striped">
                            <thead>
                                <tr>
                                    <th>Line #</th>
                                    <th>Row</th>
                                    <th>Message</th>
                                </tr>
                            </thead>
                            <tbody>
                                <g:each in="${messageCollection.collection}" var="errorRow" status="rowIndex">
                                    <tr>
                                        <td>${errorRow.lineNumber}</td>
                                        <td>${errorRow.tokens?.join(",")}</td>
                                        <td>${errorRow.reason}</td>
                                    </tr>
                                </g:each>
                            </tbody>
                        </table>
                    </g:if>
                </g:each>
            </div>
        </div>
    </body>
</html>
