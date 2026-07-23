<%@ page import="au.org.ala.userdetails.ApplicationType" %>
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
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <g:set var="entityName" value="${message(code: 'application.label', default: 'Application')}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    <title><g:message code="myprofile.myClientAndApikey" /></title>
    <asset:stylesheet src="userdetails.css" />
    <asset:javascript src="jqueryValidationEngine.js" asset-defer="" />
    <asset:stylesheet src="jqueryValidationEngine.css" />
</head>
<body>

<div id="my-applications" role="main">
    <div class="border-bottom mb-3 pb-2">
        <h1><g:message code="myprofile.myClientAndApikey" /></h1>
        </br>
        <h3><g:message code="myprofile.myClientAndApikey.subheading" /></h3>
    </div>

    <div>
        <p>The ALA’s <a href="${grailsApplication.config.getProperty('docsPortal.url')}" target="_blank">API Gateway</a> provides web service access to ALA features, including species occurrence records, taxonomic and scientific name information, images, and downloads for use offline. ALA data are still open and freely accessible, and most of our endpoints are publicly available. For the protected APIs (such as sensitive or private data), you’ll need a JSON Web Token (JWT), which can be generated via a Client ID and Client Secret. </p>
        </br>
    </div>

    <!-- Nav tabs -->
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item" role="presentation"><a class="nav-link active" href="#applications" data-bs-toggle="tab" role="tab" aria-controls="applications" aria-selected="true"><g:message code="myprofile.applications" default="Applications" /></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link" href="#help" data-bs-toggle="tab"><g:message code="help" default="Help" /></a></li>
    </ul>

    <!-- Tab panes -->
    <div class="tab-content">
        <div class="tab-pane active" id="applications" role="tabpanel">
            <div class="row">
                <div class="col-md-4">
                </div>
                <div class="col-md-8">
                    <div class="text-end" style="padding: 10px">
                        <button type="button" id="btn-create-app" class="btn btn-primary"><i class="fas fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></button>
                    </div>
                </div>
                <div class="col-md-12">
                    <g:if test="${errors}">
                        <div class="alert alert-danger">
                            <g:each in="${errors}" var="err">
                                <p><g:message error="${err}"/></p>
                            </g:each>
                        </div>
                    </g:if>

                    <g:if test="${flash.message}">
                        <div class="alert alert-info" role="status">${flash.message}</div>
                    </g:if>
                    <table id="apps-table" class="table table-bordered table-striped" style="${applicationList ? '' : 'display:none;'}">
                        <thead>
                        <tr>
                            <g:sortableColumn property="name" title="${message(code: 'application.name.label', default: 'Name')}" />
                            <g:sortableColumn property="clientId" title="${message(code: 'application.clientId.label', default: 'Client ID')}" />
                            <g:sortableColumn property="type" title="${message(code: 'application.type.label', default: 'Type')}" />

                            <td></td>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${applicationList}" status="i" var="application">
                            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                                <td>${application.name}</td>
                                <td>${application.clientId}</td>
                                <td><g:message code="${application.type}"/></td>

                                <td>
%{--                                    <button class="app-enable" data-id="${application.clientId}">Enable</button>--}%
                                    <button class="btn btn-outline-secondary btn-sm app-edit" aria-label="View/Edit" data-id="${application.clientId}"><i class="fas fa-eye"></i></button>
                                    <button class="btn btn-outline-secondary btn-sm app-delete" aria-label="Delete" data-id="${application.clientId}"><i class="fas fa-trash"></i></button>
                                    <g:set var="m2m" value="${!application?.type || application?.type == ApplicationType.M2M}" />
                                    <a href="${grailsApplication.config.getProperty('tokenApp.url')}?step=generation&client_id=${application.clientId}&client_secret=${application.secret}" target="_blank" style="${m2m ? 'display:none;' : ''}">Generate JWT</a>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                    <p id="newApplication" style="${applicationList ? 'display:none;' : ''}">Click the 'New Application' button to generate your first client ID. Once created, your entries will be displayed here.</p>

                    <div class="d-flex justify-content-center">
                        <ud:paginate action="list" total="${applicationList}" params="${params}"/>
                    </div>
                </div>
            </div>
            <div class="alert alert-well">
                <p>For further configuration information, please refer to <a href="${grailsApplication.config.getProperty('security.oidc.discovery-uri')}" target="_blank" >Discovery endpoint.</a></p>
                <p>How to access ALA restricted APIs using <a href="https://github.com/AtlasOfLivingAustralia/jwt-usage-examples/blob/main/python/example.py"
                        target="_blank">Python example client</a> and <a href="https://github.com/AtlasOfLivingAustralia/jwt-usage-examples/blob/main/R/example.R"
                                                                         target="_blank">R example client</a>.</p>
                <p>Further documentation and a full list of available endpoints are available on the <a href="${grailsApplication.config.getProperty('docsPortal.url')}" target="_blank">ALA API Docs Portal</a>. For more information or assistance, please contact us at <a href="mailto:support@ala.org.au">support@ala.org.au</a>.</p>
            </div>
        </div>
        <div class="tab-pane" id="help" role="tabpanel">
            <br/>
            <h4>FAQ</h4>
            <br/>
            <div class="accordion" id="faq">
                <g:each in="${ (1..13) }" var="c">
                    <div class="accordion-item">
                        <h2 class="accordion-header" id="q${c}">
                            <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#a${c}" aria-expanded="false" aria-controls="a${c}">
                                <g:message code="application.q${c}"/>
                            </button>
                        </h2>
                        <div id="a${c}" class="accordion-collapse collapse" aria-labelledby="q${c}" data-bs-parent="#faq">
                            <div class="accordion-body">
                                <g:message code="application.a${c}" args="[grailsApplication.config.getProperty('docsPortal.url')]"/>
                            </div>
                        </div>
                    </div>
                </g:each>
            </div>
        </div>
    </div>
    <div id="client-modal" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-lg" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h4 class="modal-title" id="heading">Create Application</h4>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <g:form name="modal-save-form" action="generateClient">
                        <fieldset class="form">
                            <div class="mb-3">
                                <label for="clientId">
                                    <g:message code="application.clientId.label" default="Client ID" />
                                </label>
                                <!-- not read by server -->
                                <input id="clientId" name="clientId" type="text" form="none" readonly class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label for="clientSecret">
                                    <g:message code="application.clientSecret.label" default="Client Secret" />
                                </label>
                                <div class="input-group">
                                    <input id="clientSecret" name="clientSecret" form="none" type="password" readonly class="form-control" />
                                    <button id="show-secret" type="button" class="btn btn-outline-dark" aria-label="Show secret" aria-label-open="Show Secret" aria-label-closed="Hide Secret">
                                        <span class="fas fa-eye"></span>
                                    </button>
                                </div><!-- /input-group -->
                            </div>
                            <g:render template="applicationForm"/>
                        </fieldset>
                    </g:form>
                </div>
                <div class="modal-footer">
                    <button id="btn-close-app" type="button" class="btn btn-outline-dark" data-bs-dismiss="modal">Close</button>
                    <button id="btn-save-app" type="submit" form="modal-save-form" class="btn btn-primary">Save changes</button>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
<asset:script type="text/javascript">
    function refreshAppTable() {
        let url = '<g:createLink controller="profile" action="listApplications" />';
        $.get(url)
        .done(function(data) {
            if(data.length > 0){
                $('#newApplication').hide()
                $('#apps-table').show()
                let $appsTableBody = $('#apps-table tbody');
                $appsTableBody.children().remove();
                for (var i = 0; i < data.length; ++i) {
                    let $tr = $('<tr></tr>', {class: (i % 2) === 0 ? 'even': 'odd'});
                    $tr.append($('<td></td>', {text: data[i].name}));
                    $tr.append($('<td></td>', {text: data[i].clientId}));
                    var type;
                    if(data[i].type.name === "M2M") type = "Machine-to-Machine (M2M)";
                    else if(data[i].type.name === "PUBLIC") type = "Public Client (Client-side Application)";
                    else if(data[i].type.name === "CONFIDENTIAL") type = "Confidential Client (Server-side Application)";
                    $tr.append($('<td></td>', {text: type}));
                    let $buttonsTd = $('<td></td>');
                    $buttonsTd.append($('<button>', {class: 'btn btn-outline-secondary btn-sm me-1 app-edit', 'aria-label': 'View/Edit', 'data-id': data[i].clientId}).append($('<i></i>', {class: 'fas fa-eye'})));
                    $buttonsTd.append($('<button>', {class: 'btn btn-outline-secondary btn-sm me-1 app-delete', 'aria-label': 'Delete', 'data-id': data[i].clientId}).append($('<i></i>', {class: 'fas fa-trash'})));
                    if(data[i].type.name !== "M2M"){
                        var url = "${grailsApplication.config.getProperty('tokenApp.url')}?step=generation&client_id=" + data[i].clientId + (data[i].secret ? "&client_secret=" + data[i].secret : "");
                        var createA = document.createElement('a');
                        var createAText = document.createTextNode("Generate JWT");
                        createA.setAttribute('href', url);
                        createA.setAttribute('target', "_blank");
                        createA.appendChild(createAText);
                        $buttonsTd.append(createA);
                    }
                    $tr.append($buttonsTd);
                    $appsTableBody.append($tr);
                }
            }
            else{
                $('#newApplication').show()
                $('#apps-table').hide()
            }
        });

    }

    function showEditModal(data) {
        clientModal.show();
        $('#heading').text("Update Application");
        $('#name').val(data.name);
        $('#type').val(data.type.name).trigger('change');
        $('#clientId').val(data.clientId).parents('.mb-3').show();
        $("#needTokenAppAsCallback").prop("checked", data.needTokenAppAsCallback);
        if(data.needTokenAppAsCallback == true){
            document.getElementById("needTokenAppAsCallback").disabled = true;
        }
        else{
            document.getElementById("needTokenAppAsCallback").disabled = false;
        }
        let $clientSecret = $('#clientSecret');
        $clientSecret.val(data.secret);
        if (data.secret) {
            $clientSecret.parents('.mb-3').show();
        } else {
            $clientSecret.parents('.mb-3').hide();
        }

        let callbacks = data.callbacks ? data.callbacks : [];

        let $callbacks = $('#callback-list')
        $callbacks.children().remove();

        for (var i = 0; i < callbacks.length; ++i) {
            addCallbackToForm($callbacks, callbacks, i, true);
        }

        $('#callbacks').val('');

        let url = '<g:createLink controller="profile" action="updateClient" id="clientId"/>'.replace('clientId', data.clientId);
        $('#modal-save-form').validationEngine('attach', { scroll: false });
        $("#modal-save-form").off('submit').on('submit', function (e) {
            var valid = $('#modal-save-form').validationEngine('validate');
            if(valid){
                e.preventDefault();
                let $this = $(this);
                let $saveButton = $this.find('btn-save-app');
                let $saveButtonContent = $saveButton.content;
                $saveButton.html('<i class="fas fa-spinner"></i>');
                setModalButtonsDisabled(true);
                $('#callbacks').val('');
                $.post(
                    url,
                    $(this).serialize()
                ).done(function(data) {
                    refreshAppTable();
                    clientModal.hide();
                }).fail(function(data) {
                    alert( "Error when updating the application ");
                  }).always(function() {
                    $saveButton.html($saveButtonContent);
                    setModalButtonsDisabled(false);
                });
            }
            else {
                e.preventDefault();
            }
        });
    }

    function showCreateModal() {
        clientModal.show();
        $('#heading').text("Create Application");
        $('#name').val('');
        $('#type').val('PUBLIC').trigger('change');
        $('#clientId').val('').parents('.mb-3').hide();
        $('#clientSecret').val('').parents('.mb-3').hide();
        $('#callback-list').find('[data-index').remove()
        $("#needTokenAppAsCallback").prop("checked", true);
        document.getElementById("needTokenAppAsCallback").disabled = false;

        let $callbacks = $('#callback-list')
        $callbacks.children().remove();
        addCallbackToForm($callbacks, ["http://localhost:8080/callback"], 0, false);

        $('#callbacks').val('');

        let url = '<g:createLink controller="profile" action="generateClient" />';
        $('#modal-save-form').validationEngine('attach', { scroll: false });
        $("#modal-save-form").off('submit').on('submit', function (e) {
            var valid = $('#modal-save-form').validationEngine('validate');
            if(valid){
                e.preventDefault();
                let $this = $(this);
                let $saveButton = $this.find('btn-save-app');
                let $saveButtonContent = $saveButton.content;
                $saveButton.html('<i class="fas fa-spinner"></i>');
                setModalButtonsDisabled(true);
                $('#callbacks').val('');
                $.post(
                    url,
                    $(this).serialize()
                ).done(function(data) {
                    if(data.errors.errors.length != 0){
                        showCreateModal();
                    }
                    else {
                        showEditModal(data);
                        refreshAppTable();
                    }
                }).fail(function(data) {
                    alert( "Error when creating the application");
                  }).always(function() {
                    $saveButton.html($saveButtonContent);
                    setModalButtonsDisabled(false);
                });
            }
            else {
                e.preventDefault();
            }
        });
    }

    function addCallbackToForm($callbacks, callbacks, index, isEdit) {
        let value = callbacks[index];
        let span = $('<span></span>', {class: 'tag badge bg-secondary', 'data-index': index, style: "display: inline-block;"});
        let innerSpan = $('<span></span>', {text: value});
        let button = $('<a></a>', {'data-index': index, role: 'button', class: 'btn btn-danger delete'}).append('<i class="fas fa-trash"></i>');
        let input = $('<input></input>', {value: value, 'data-index': index, type: 'hidden', name: 'callbacks'});

        span.append(innerSpan);
        if(!isEdit) {
            span.append(button);
        }
        $callbacks.append(span);
        $callbacks.append(input);
    }

    function setModalButtonsDisabled(disabled) {
        $('#btn-close-app').prop('disabled', disabled);
        $('#btn-save-app').prop('disabled', disabled);
    }

    function validateCallbacksRequired(field, rules, i, options) {
      if (($('#type').val() =='PUBLIC' || $('#type').val() == 'CONFIDENTIAL') && $('#callback-list input').length == 0) {
        rules.push('required');
        return "At least one callback url is required.";
      }
    }

    let clientModal;

    $(function() {
        clientModal = new bootstrap.Modal(document.getElementById('client-modal'));
        $('#btn-create-app').on('click', function() {
            let $this = $(this);
            showCreateModal();
        });
        $('.app-enable').on('click', function() {

        });
        $('#apps-table tbody').on('click', '.app-edit', function() {
            let $this = $(this);
            let $i = $this.find('i');
            $i.toggleClass('fa-spinner').toggleClass('fa-eye');
            let id = $this.data('id');
            let url = "<g:createLink controller="profile" action="application" id="clientId"/>".replace('clientId', id);
            $.get({url: url, dataType: "json"})
                .always(function() {
                    $i.toggleClass('fa-spinner').toggleClass('fa-eye');
                }).done(function(data) {
                    showEditModal(data);
                }).fail(function() {
                    alert("Error loading application");
                });
        }).on('click', '.app-delete', function() {
            let confirmDelete = confirm("Are you sure?");
            if (confirmDelete) {
                let id = this.getAttribute('data-id')
                $.post("<g:createLink controller="profile" action="deleteApplication" id="clientId"/>".replace('clientId', id))
                .done(function(data) {
                    refreshAppTable();
                }).fail(function() {
                  alert("Error deleting application");
                });
            }
        });
        $('#show-secret').on('click', function(e) {
            e.preventDefault()
            let $this = $(this);
            let $icon = $this.find('span');
            let $secretInput = $('#clientSecret')
            if ($secretInput.is('[type=password]')) {
                $secretInput.attr('type', 'text');
                $icon.removeClass('fa-eye');
                $icon.addClass('fa-eye-slash');
                $this.attr('aria-label', $this.attr('aria-label-open'));
            } else {
                $secretInput.attr('type', 'password');
                $icon.removeClass('fa-eye-slash');
                $icon.addClass('fa-eye');
                $this.attr('aria-label', $this.attr('aria-label-closed'));
            }
        });
    });
</asset:script>


</div>
</body>
</html>