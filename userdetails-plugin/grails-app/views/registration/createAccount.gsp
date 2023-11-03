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
    <g:if test="${!alreadyRegistered && edit}">
        <g:set var="title"><g:message code="create.account.edit.account" /></g:set>
        <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    </g:if>
    <g:else>
        <g:set var="title"><g:message code="create.account.title" /></g:set>
    </g:else>
    <title>${title}</title>
    <asset:stylesheet src="userdetails.css" />
    <asset:stylesheet src="createAccount.css" />
    <g:if test="${grailsApplication.config.getProperty('recaptcha.siteKey')}">
        <script src="https://www.google.com/recaptcha/api.js" async defer></script>
    </g:if>
    <script src="https://cdn.rawgit.com/davidshimjs/qrcodejs/gh-pages/qrcode.min.js"></script>
</head>
<body>

<div class="row">
    <h1>${title}</h1>
    <g:if test="${flash.message}">
        <div class="alert alert-warning">
            ${flash.message}
        </div>
    </g:if>
    <g:if test="${inactiveUser}">
        <div class="row">
            <div class="col-sm-12">
                <div class="well">
                    <p class="text-danger"><g:message code="create.account.already-reg" args="[params.email]" />
                    </p>

                    <p>
                        <g:message code="create.account.if.error" args="[grailsApplication.config.getProperty('supportEmail')]" />
                    </p>
                </div>
            </div>
        </div>
    </g:if>
    <g:elseif test="${lockedUser}">
        <div class="row">
            <div class="col-sm-12">
                <div class="well">
                    <p class="text-danger"><g:message code="create.account.locked" args="[params.email]" />
                    </p>

                    <p>
                        <g:message code="create.account.if.error" args="[grailsApplication.config.getProperty('supportEmail')]" />
                    </p>
                </div>
            </div>
        </div>
    </g:elseif>
    <g:elseif test="${alreadyRegistered}">
        <div class="row">
            <div class="col-sm-12">
                <div class="well">
                    <p class="text-danger"><g:message code="create.account.already.registered" /> <strong>${params.email}</strong>.</p>

                    <p>
                        <g:message code="create.account.login.with.username" /> <g:link controller="login"><g:message code="create.account.click.here" /></g:link>.<br/>
                        <g:message code="create.account.resetting.your.password" /> <g:link controller="registration"
                                                                                 action="forgottenPassword"
                                                                                 params="${[email: params.email]}"><g:message code="create.account.click.here" /></g:link>.
                    </p>
                </div>
            </div>
        </div>
    </g:elseif>

    <div class="row">
        <div class="col-md-8 col-md-push-4">
            <div class="well">
                <g:if test="${!edit}">
                    <h2><g:message code="create.account.do.i.need.account" /></h2>

                    <p><g:message code="create.account.motivation.intro" />

                    <ul>
                    <li><g:message code="create.account.motivation.1" />
                    <li><g:message code="create.account.motivation.2" />
                    <li><g:message code="create.account.motivation.3" />
                    <li><g:message code="create.account.motivation.4" />
                    </ul>
                    <p><g:message code="create.account.motivation.footer" />
                </g:if>
                <h2><g:message code="create.account.your.account.title" /></h2>
                <p>
                    <g:message code="create.account.your.email.will.be.your.account.login"
                               args="[grailsApplication.config.skin.orgNameShort]" />
                    <g:if test="${grailsApplication.config.getProperty('registration.showAlaMessage')}">
                        <g:message code="create.account.your.email.will.be.your.account.login.ala" args="[grailsApplication.config.getProperty('registration.resetPasswordArticle'), grailsApplication.config.getProperty('registration.alertArticle')]" />
                    </g:if>
                </p>
                <g:if test="${grailsApplication.config.getProperty('registration.showAlaMessage')}">
                    <p><b><g:message code="create.account.your.email.will.be.your.account.confirm.ala" /></b></p>
                </g:if>
                <g:if test="${!edit}">
                    <p><g:message code="create.account.activation.description" />
                    <g:if test="${grailsApplication.config.getProperty('registration.showAlaMessage')}">
                        <g:message code="create.account.activation.description.ala" args="[grailsApplication.config.getProperty('registration.activationArticle')]" />
                    </g:if></p>
                </g:if>

                <g:render template="passwordPolicy"
                          model="[passwordPolicy: passwordPolicy]"/>

                <h2><g:message code="create.account.policy.title" /></h2>
                <p>
                    <g:message code="create.account.privacy.title" />
                    <a href="${grailsApplication.config.getProperty('privacyPolicy')}">
                    <g:message code="create.account.privacy.link" /></a>.
                </p>
                <h2><g:message code="create.account.tos.title" /></h2>
                <p>
                    <g:message code="create.account.tos.description"
                               args="[grailsApplication.config.getProperty('skin.orgNameShort'), grailsApplication.config.getProperty('termsOfUse')]" />
                </p>
                <g:if test="${edit && visibleMFA}">
                    <h2><g:message code="user.enableMFA.title" /></h2>
                    <g:if test="${props?.enableMFA == 'true'}">
                        <p><g:message code="user.enabledMFA.description" />
                    </g:if>
                    <g:else>
                        <input name="setupMFA" class="btn btn-default" id="setupMFA" value="${message(code: 'user.setupMFA')}">
                    </g:else>

                    <div id="mfa" hidden="hidden">
                        <p id="instruction">
                            <g:message code="user.mfa.instruction" />
                            <br/>
                            <g:message code="user.mfa.secret" /><label id="secret"></label>
                        </p>
                        <div id="qrcode"></div>
                        <label for="code" id="codeLabel"><g:message code="user.mfa.code" /></label>
                        <p id="message" hidden></p>
                        <input id="code" name="code" type="text" class="form-control" data-validation-engine="validate[required]"/>
                        <div id="buttonsDiv">
                            <button class="btn btn-primary" id="verifyMFA"><g:message code="user.mfa.verify" /></button>
                            <button id="hide" class="btn btn-danger">Hide</button>
                        </div>
                    </div>
                </g:if>
            </div>
        </div>
        <div class="col-md-4 col-md-pull-8">
            <div>
            <g:form name="updateAccountForm" method="POST" action="${edit ? 'update' : 'register'}" controller="registration" useToken="true" onsubmit="updateAccountSubmit.disabled = true; return true;">
                <div class="form-group">
                    <label for="firstName"><g:message code="create.account.first.name" /></label>
                    <input id="firstName" name="firstName" type="text" class="form-control" value="${user?.firstName}" data-validation-engine="validate[required]"/>
                </div>
                <div class="form-group">
                    <label for="lastName"><g:message code="create.account.last.name" /></label>
                    <input id="lastName" name="lastName" type="text" class="form-control" value="${user?.lastName}"  data-validation-engine="validate[required]"/>
                </div>
                <div class="form-group">
                    <label for="email"><g:message code="create.account.email.address" /></label>
                    <input id="email" name="email" type="text" class="form-control" value="${user?.email}"
                           data-validation-engine="validate[required,custom[email]]"
                           data-errormessage-value-missing="${message(code:'create.account.email.is.required')}"
                    />
                </div>
                <div class="form-group">
                    <label for="confirm-email"><g:message code="create.account.confirm.email.address" /></label>
                    <input id="confirm-email" name="confirm-email" type="text" class="form-control" value="${user?.email}"
                           data-validation-engine="validate[required,custom[email],equals[email]]"
                           data-errormessage-value-missing="${message(code:'create.account.confirm.email.is.required')}"
                           data-errormessage-pattern-mismatch="${message(code:'create.account.confirm.email.mismatch')}"
                    />
                </div>

                <g:if test="${!edit}">
                    <div class="form-group">
                    <label for="password"><g:message code="create.account.password" /></label>
                    <input id="password"
                           name="password"
                           class="form-control"
                           value=""
                           data-validation-engine="validate[required, minSize[8]]"
                           data-errormessage-value-missing="${message(code:'create.account.password.is.required')}"
                           type="password"
                    />
                    </div>
                    <div class="form-group">
                    <label for="reenteredPassword"><g:message code="create.account.reentered.password" /></label>
                    <input id="reenteredPassword"
                           name="reenteredPassword"
                           class="form-control"
                           value=""
                           data-validation-engine="validate[required, minSize[8]]"
                           data-errormessage-value-missing="${message(code:'create.account.password.is.required')}"
                           type="password"
                    />
                    </div>
                </g:if>
                <g:if test="${grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)}">
                    <div class="form-group">
                        <label for="affiliation"><g:message code="create.account.affiliation" default="What is your primary affiliation?" /> *</label>
                        <g:select id="affiliation" name="affiliation"
                                  class="form-control"
                                  value="${props?.affiliation}"
                                  from="${l.affiliations()}"
                                  optionKey="key"
                                  optionValue="value"
                                  noSelection="${['': message(code:'create.account.choose.affiliation', default: '-- Choose one --')]}"
                                  data-validation-engine="validate[required]"
                        />
                    </div>
                </g:if>
                <div class="form-group">
                    <label for="organisation"><g:message code="create.account.organisation" /></label>
                    <input id="organisation" name="organisation" type="text" class="form-control" value="${props?.organisation}"/>
                </div>
                <div class="form-group">
                    <label for="country"><g:message code="create.account.country" /> *</label>
                    <g:select id="country" name="country"
                              class="form-control chosen-select"
                              autocomplete="off"
                              value="${props?.country ?: edit ? null :'AU'}"
                              keys="${l.countries()*.isoCode}"
                              from="${l.countries()*.name}"
                              noSelection="${['': message(code:'create.account.choose.your.country')]}"
                              valueMessagePrefix="ala.country"
                              data-validation-engine="validate[required]"
                    />
                </div>
                <div class="form-group">
                    <label for="state"><g:message code="create.account.state.province" /></label>
                    <g:select id="state" name="state"
                              class="form-control chosen-select"
                              autocomplete="off"
                              value="${props?.state}"
                              keys="${l.states(country: props?.country ?: 'AU')*.isoCode}"
                              from="${l.states(country: props?.country ?: 'AU')*.name}"
                              noSelection="${['': message(code:'create.account.choose.your.state')]}"
                              valueMessagePrefix="ala.state"
                    />
                </div>
                <div class="form-group">
                    <label for="city"><g:message code="create.account.city" /></label>
                    <input id="city" name="city" type="text" class="form-control" value="${props?.city}" />
                </div>
                <g:if test="${edit}">
                    <g:if test="${visibleMFA}">
                        <div class="form-group checkbox">
                            <label>
                                <g:checkBox name="enableMFA" value="${props?.enableMFA == 'true'}" id="enableMFA" disabled="disabled"/> <g:message code="user.enabledMFA" />
                            </label>
                            <g:if test="${props?.enableMFA == 'true'}">
                                <g:link controller="Registration" action="disableMfa" params="[userId:user?.email]">Disable MFA</g:link>
                            </g:if>
                        </div>
                    </g:if>
                    <g:if test="${grailsApplication.config.getProperty('userdetails.features.requirePasswordForUserUpdate', Boolean, true)}">
                        <div class="form-group">
                            <label for="confirmUserPassword">
                                <g:message code="create.account.confirm.password" />
                            </label>
                            <input id="confirmUserPassword"
                                   name="confirmUserPassword"
                                   class="form-control"
                                   value=""
                                   data-validation-engine="validate[required, minSize[8]]"
                                   data-errormessage-value-missing="Password is required!"
                                   type="password"
                                   autocomplete="current-password"/>
                        </div>
                    </g:if>

                    <button id="updateAccountSubmit" class="btn btn-primary"><g:message code="create.account.update.account" /></button>
                    <button id="disableAccountSubmit" class="btn btn-danger"><g:message code="create.account.disable.account" /></button>
                </g:if>
                <g:else>
                    <g:if test="${grailsApplication.config.getProperty('recaptcha.siteKey')}">
                        <div class="g-recaptcha" data-sitekey="${grailsApplication.config.getProperty('recaptcha.siteKey')}"></div>
                        <br/>
                    </g:if>
                    <button id="updateAccountSubmit" class="btn btn-primary"><g:message code="create.account.btn" /></button>
                </g:else>
            </g:form>
            </div>
            <g:if test="${flash.invalidToken}">
                <g:message code="create.account.button.twice" />
            </g:if>
        </div>
   </div>
</div>
</body>
<asset:javascript src="createAccount.js" asset-defer="" />
<asset:script type="text/javascript">
    $(function() {
        userdetails.initCountrySelect('.chosen-select', '#country', '#state', "${g.createLink(uri: '/ws/registration/states')}");

        $('#updateAccountForm').validationEngine('attach', { scroll: false });
        $("#updateAccountSubmit").click(function(e) {

            $("#updateAccountSubmit").attr('disabled','disabled');

            var pm = $('#password').val() == $('#reenteredPassword').val();
            if(!pm){
                alert("The supplied passwords do not match!");
            }

            var valid = $('#updateAccountForm').validationEngine('validate');

            if (valid && pm) {
                $("form[name='updateAccountForm']").submit();
            } else {
                $('#updateAccountSubmit').removeAttr('disabled');
                e.preventDefault();
            }
        });

        $("#disableAccountSubmit").click(function(e) {

            $("#disableAccountSubmit").attr('disabled','disabled');

            var valid = confirm("${message(code: 'default.button.delete.user.confirm.message', default: 'Are you sure want to disable your account? You won\'t be able to login again. You will have to contact us in the future if you want to reactivate your account.')}");

            if (valid) {
                $('#updateAccountForm').validationEngine('detach');
                $("form[name='updateAccountForm']").attr('action','disableAccount');
                $("form[name='updateAccountForm']").submit();
            } else {
                $('#disableAccountSubmit').removeAttr('disabled');
                e.preventDefault();
            }
        });

         $("#setupMFA").click(function(e) {
             $.ajax({
             url: "${createLink(action:'getSecretForMfa', controller: 'Registration')}",
             type: "GET",
             success: function(result){
                 if(result.success){
                    document.getElementById("secret").innerHTML = ""
                    document.getElementById("secret").innerHTML = result.code
                    document.getElementById("mfa").hidden = false
                    document.getElementById("qrcode").innerHTML = ""
                    new QRCode(document.getElementById("qrcode"), "otpauth://totp/${grailsApplication.config.getProperty('serverName')}:${raw(user?.email)}?secret=" + result.code);
                }
                 else{
                     document.getElementById("message").value = result.error
                     document.getElementById("message").hidden = false
                 }
            }});
         });

         $("#verifyMFA").click(function(e) {
             var code = $("#code").val();
             if(code == null || code === "" || isNaN(code)) {
                 document.getElementById("message").innerHTML = "Invalid code"
                 document.getElementById("message").hidden = false
             }
             else {
                 $.ajax({
                 url: "${createLink(action:'verifyAndActivateMfa', controller: 'Registration')}?userCode=" + code + "&userId=${raw(user?.email)}",
                 type: "GET",
                 success: function(result){
                     if(result.success){
                        document.getElementById("message").innerHTML = "Success"
                        document.getElementById("message").hidden = false
                        document.getElementById("enableMFA").checked = true;
                    }
                     else{
                         document.getElementById("message").innerHTML = result.error
                         document.getElementById("message").hidden = false
                     }
                }});
             }
         });

         $("#hide").click(function(e) {
             document.getElementById("code").value = ""
             document.getElementById("message").innerHTML = ""
             document.getElementById("message").hidden = true
             document.getElementById("mfa").hidden = true
         });

    });
</asset:script>
</html>
