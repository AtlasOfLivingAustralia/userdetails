<%@ page import="au.org.ala.userdetails.ApplicationType" %>
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

<div id="app-form" class="row">
    <div class="col-md-12">
        <div class="form-group">
            <label for="name">
                <g:message code="application.name.label" default="Name"/> <button class="btn btn-link" aria-label="Help for name field" role="button" type="button" data-toggle="popover" title="Name" data-content="This name will be used to identify your application to end users when logging into their ALA account." data-trigger="hover"><i class="fa fa-question"></i></button>
            </label>
            <g:textField name="name" class="form-control" value="${applicationInstance?.name}" placeholder="Application Name" data-validation-engine="validate[required]"/>
        </div>

        <div class="form-group">
            <label for="type"><g:message code="application.type.label" default="Type"/>  <button class="btn btn-link" aria-label="Help for type field" role="button" type="button" data-toggle="popover" title="Type" data-html="true" data-trigger="hover" data-content="The type of application you're creating:
<br/><b>Public Client</b>: Your application is distributed to clients, such as a JS app in the browser, mobile app or native application.  This will allow your client to generate tokens with the auth code w/ PKCE grant.
<br/><b>Confidential Client</b>: You're accessing ALA APIs. This will allow your client to generate tokens with the auth code grant.
<br/><b>Machine-to-Machine (M2M)</b>: Your application is only doing machine to machine communication and doesn't require end user authentication.  This will allow your client to generate tokens with the client credential grant.
"><i class="fa fa-question"></i></button></label>
            <g:select id="type" name="type"
                class="form-control"
                value="${applicationInstance?.type}"
                from="${ApplicationType.values() - ApplicationType.UNKNOWN}"
                valueMessagePrefix="application.type"
                      data-validation-engine="validate[required]"/>
        </div>
        <g:set var="m2m" value="${!applicationInstance?.type || applicationInstance?.type == ApplicationType.M2M}" />
        <div id="callback-section" style="${m2m ? 'display:none;' : ''}">

            <div class="form-group fieldcontain ${hasErrors(bean: applicationInstance, field: 'callbacks', 'error')} ">
                <label for="callbacks">
                    <g:message code="application.callback.label" default="Callback URLs"/>  <button class="btn btn-link" aria-label="Help for callback field" role="button" type="button" data-toggle="popover" title="Callback" data-content="For applications, provide one or more callback URLs that your application will use to receive the OAuth tokens via callback.  Callback URLs must be https (except for localhost) or a custom app scheme." data-trigger="hover"><i class="fa fa-question"></i></button>
                </label>
                <div id="callback-list">
                <g:each in="${applicationInstance?.callbacks}" status="i" var="callback">
                    <span class="label label-default" data-index="${i}">${callback}</span>
                    <button type="button" class="btn btn-danger delete" data-index="${i}"><i class="fa fa-trash"></i></button>
                    <g:hiddenField name="callbacks" data-index="${i}" value="${callback}"/>
                </g:each>
                </div>
                <div class="input-group">
                    <g:textField type="url" name="callbacks" class="form-control" value="" placeholder="https://yourdomain.com/callback"
                                 data-validation-engine="validate[funcCall[validateCallbacksRequired]]"/>
                    <span class="input-group-btn">
                        <button type="button" id="btn-add-callback" class="btn btn-primary"><i class="fa fa-plus"></i></button>
                    </span>
                </div>

                <div class="form-group">
                    <div class="checkbox">
                        <label>
                            <g:checkBox checked="true" name="needTokenAppAsCallback" id="needTokenAppAsCallback"/> Do you need to add <a href="${grailsApplication.config.getProperty('tokenApp.url')}" target="_blank">tokens app</a> (which can be used to generate JWT tokens) as a callback url?
                        </label>
                    </div>
                </div>

%{--            <div class="fieldcontain ${hasErrors(bean: applicationInstance, field: 'allowTokensApp', 'error')} ">--}%
%{--                <div class="checkbox">--}%
%{--                    <label>--}%
%{--                        <g:checkBox name="allowTokensApp" value="${applicationInstance?.allowTokensApp}"/> <g:message code="applications.form.allowTokensApp" />--}%
%{--                    </label>--}%
%{--                </div>--}%
%{--            </div>--}%
            </div>

        </div>

    </div>

</div>

<asset:script type="text/javascript">

    function show(type) {
        if (type === 'M2M') {
            $('#callback-section').hide();
            reset('#callback');
        } else {
            $('#callback-section').show();
        }
    }

    function reset(div) {
        $(div).find(':input').each(function() {
            switch(this.type) {
                case 'password':
                case 'text':
                case 'textarea':
                case 'file':
                case 'select-one':
                case 'select-multiple':
                case 'date':
                case 'number':
                case 'tel':
                case 'email':
                    $(this).val('');
                    break;
                case 'checkbox':
                case 'radio':
                    this.checked = false;
                    break;
            }
        });
    }

    function addCallback() {
        let $callback = $('#callbacks');

        if (!$callback[0].checkValidity()) {
            alert('not a valid url');
        }
        let value = $callback.val();
        $callback.val('');

        let $callbacks = $('#callback-list');
        let length = $callbacks.children('input').length;

        let span = $('<span></span>', {class: 'tag label label-default', 'data-index': length});
        let innerSpan = $('<span></span>', {text: value});
        let button = $('<a></a>', {'data-index': length, role: 'button', class: 'btn btn-danger delete'}).append('<i class="fa fa-trash"></i>');
        let input = $('<input></input>', {value: value, 'data-index': length, type: 'hidden', name: 'callbacks'});

        span.append(innerSpan);
        span.append(button);
        $callbacks.append(span);
        $callbacks.append(input);
    }

    function removeCallback(i) {
        let $callbacks = $('#callback-list');
        $callbacks.find('[data-index="' + i + '"]').remove();

        $callbacks.find('span').each(function(i) { $(this).attr('data-index', i) });
        $callbacks.find('button').each(function(i) { $(this).attr('data-index', i) });
        $callbacks.find('input').each(function(i) { $(this).attr('data-index', i) });
    }

    $(function() {
        $('#app-form').on('submit', function() {

        });
        $('#type').on('change', function() {
            let val = $(this).val();
            show(val);
        });
        $('#callbacks').on('keypress', function(event) {
            if (event.key === "Enter") {
                event.preventDefault();
                addCallback();
            }
        });
        $('#btn-add-callback').on('click', function(e) {
            addCallback();
        });
        $('#callback-list').on('click', 'a.delete', function(e) {
            e.preventDefault();
            removeCallback($(this).data('index'));
        });
        $('[data-toggle="popover"]').popover()
    });
</asset:script>