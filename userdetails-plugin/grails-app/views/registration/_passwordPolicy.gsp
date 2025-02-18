<h3><g:message code="account.password.policy.title"/></h3>

<p>
    <g:message code="account.password.policy.requirements.length"
               args="[passwordPolicy.minLength]"/>
</p>

<g:if test="${passwordPolicy.enabled && passwordPolicy.charGroupMinRequired > 0}">
    <p>
        <g:message code="account.password.policy.requirements.complexity.intro"
                   args="[passwordPolicy.charGroupMinRequired, passwordPolicy.charGroupCount]"/>
    </p>
    <ul>
        <g:if test="${passwordPolicy.charGroupMinUpperCase > 0}">
            <li>
                <g:message code="account.password.policy.requirements.complexity.upper"/>
            </li>
        </g:if>
        <g:if test="${passwordPolicy.charGroupMinLowerCase > 0}">
            <li>
                <g:message code="account.password.policy.requirements.complexity.lower"/>
            </li>
        </g:if>
        <g:if test="${passwordPolicy.charGroupMinUpperOrLowerCase > 0}">
            <li>
                <g:message code="account.password.policy.requirements.complexity.upperOrLower"/>
            </li>
        </g:if>
        <g:if test="${passwordPolicy.charGroupMinDigit > 0}">
            <li>
                <g:message code="account.password.policy.requirements.complexity.number"/>
            </li>
        </g:if>
        <g:if test="${passwordPolicy.charGroupMinSpecial > 0}">
            <li>
                <g:message code="account.password.policy.requirements.complexity.special"/>
            </li>
        </g:if>
    </ul>
</g:if>

<g:if test="${passwordPolicy.excludeCommonPasswords}">
    <p>
        <g:message code="account.password.policy.requirements.complexity.common"/>
    </p>
</g:if>
<g:if test="${passwordPolicy.excludeUsername}">
    <p>
        <g:message code="account.password.policy.requirements.complexity.username"/>
    </p>
</g:if>
<g:if test="${passwordPolicy.excludeUsQwertyKeyboardSequence}">
    <p>
        <g:message code="account.password.policy.requirements.complexity.knownsequence"/>
    </p>
</g:if>
