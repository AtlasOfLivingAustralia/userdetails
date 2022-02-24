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

<%@ page contentType="text/html"%>
<html>
  <head>
      <title>${emailTitle}</title>
      <asset:stylesheet src="application.css" />
      <style>
      body {
          font-family: 'Roboto';
      }
      </style>
  </head>

  <body>
    <a href="${grailsApplication.config.getProperty('homeUrl', String, 'https://www.ala.org.au')}" title="<g:message code='email.logo.title' />">
        <img src="${grailsApplication.config.getProperty('homeLogoUrl', String, 'https://www.ala.org.au/app/uploads/2020/06/ALA_Logo_Inline_RGB-300x63.png')}"
             alt="<g:message code='email.logo.alt' />" >
    </a>

    <div class="email-body">
        <h3>${emailTitle}</h3>
        <p><g:message code="email.greeting" /> ${userName}</p>
        <p><markdown:renderHtml text="${emailBody1}"/></p>
        <g:if test="${password}">
            <g:message code="reset.password.description" args="[password]" />
        </g:if>
        <p><a href="${link}">${link}</a></p>
        <g:if test="${emailBody2}">
            <p><markdown:renderHtml text="${emailBody2}"/></p>
        </g:if>
    </div>
  </body>
</html>