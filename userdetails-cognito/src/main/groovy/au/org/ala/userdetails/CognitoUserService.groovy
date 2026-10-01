package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.users.RoleRecord
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.users.UserRoleRecord
import au.org.ala.web.AuthService
import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.tokens.TokenService
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AddCustomAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserMfaPreferenceRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AssociateSoftwareTokenRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeDataType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.SchemaAttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.SoftwareTokenMfaSettingsType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifyUserAttributeRequest
import com.nimbusds.oauth2.sdk.token.AccessToken
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenRequest
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.NotImplementedException
import org.springframework.beans.factory.annotation.Value

import java.util.stream.Stream

@Slf4j
class CognitoUserService implements IUserService<UserRecord, UserPropertyRecord, RoleRecord, UserRoleRecord> {

    static mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set

    static customAttrs = [ 'organisation', 'city', 'state', 'country' ] as Set

    EmailService emailService
    TokenService tokenService
    LocationService locationService

    CognitoIdentityProviderClient cognitoIdp
    String poolId
    JwtProperties jwtProperties
    List<String> socialLoginGroups
    AuthService authService
    boolean useGatewayAPI

    @Value('${attributes.affiliations.enabled:false}')
    boolean affiliationsEnabled = false
    public static final String TEMP_AUTH_KEY = 'tempAuthKey'

    @Override
    UserRecord newUser(GrailsParameterMap params) {
        UserRecord newUser = new UserRecord()
        newUser.setProperties(params)
        return newUser
    }

    @Override
    RoleRecord newRole(GrailsParameterMap params) {
        return params ? new RoleRecord(role: params.role, description: params.description) : new RoleRecord()
    }

    @Override
    boolean updateUser(String userId, GrailsParameterMap params, Locale locale) {

        UserRecord user = getUserById(userId)
        def isUserLocked = user.locked
        def isUserActivated = user.activated

        def emailRecipients = [ user.email ]
        if (params.email != user.email) {
            emailRecipients << params.email
        }

        try {
            baseUpdateUser(user, params, userId)

            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            return true

        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }

        return false
    }

    private void baseUpdateUser(UserRecord user, GrailsParameterMap params, String userId) {
        user.setProperties(params)

        Collection<AttributeType> userAttributes = new ArrayList<>()

        userAttributes.add(AttributeType.builder().name('email').value(user.email).build())
//            userAttributes.add(AttributeType.builder().name('userName').value(user.userName).build())
//        userAttributes.add(AttributeType.builder().name('userid').value(record.id).build())
        userAttributes.add(AttributeType.builder().name('given_name').value(user.firstName).build())
        userAttributes.add(AttributeType.builder().name('family_name').value(user.lastName).build())

        params.findAll { customAttrs.contains(it.key) }
                .each { userAttributes.add(AttributeType.builder().name("custom:${it.key}").value(it.value as String).build()) }

        if (affiliationsEnabled && params.get('affiliation')) {
            userAttributes.add(AttributeType.builder().name("custom:affiliation").value(params.get('affiliation', '')).build())
        }

        AdminUpdateUserAttributesRequest request =
                AdminUpdateUserAttributesRequest.builder()
                        .userPoolId(poolId)
                        .username(userId)
                        .userAttributes(userAttributes)
                        .build() as AdminUpdateUserAttributesRequest

        cognitoIdp.adminUpdateUserAttributes(request)
    }

    @Override
    boolean adminUpdateUser(String userId, GrailsParameterMap params, Locale locale) {

        UserRecord user = getUserById(userId)
        def isUserLocked = user.locked
        def isUserActivated = user.activated

        try {
            baseUpdateUser(user, params, userId)
            //enable or disable user
            if(params.locked && !isUserLocked){
                disableUser(user)
            }
            else if(!params.locked && isUserLocked) {
                enableUser(user)
            }

            //activate account if not
            if(params.activated && !isUserActivated){
                activateAccount(user, params)
            }

            return true

        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }

        return false
    }

    @Override
    boolean disableUser(UserRecord user) {
        def response = cognitoIdp.adminDisableUser(AdminDisableUserRequest.builder()
                .username(user.email)
                .userPoolId(poolId)
                .build() as AdminDisableUserRequest)
        return isSuccessful(response)
    }

    @Override
    boolean enableUser(UserRecord user) {
        def response = cognitoIdp.adminEnableUser(AdminEnableUserRequest.builder()
                .username(user.email)
                .userPoolId(poolId)
                .build() as AdminEnableUserRequest)
        return isSuccessful(response)
    }

    @Override
    boolean isActive(String email) {
        def user = getUserByEmail(email)
        return user?.getActivated()
    }

    @Override
    boolean isLocked(String email) {
        def user = getUserByEmail(email)
        return user?.getLocked()
    }

    @Override
    boolean isEmailInUse(String email) {

        if (useGatewayAPI) {
            //using gateway API which consolidate both users from cognito pool and CAS
            def user = authService.getUserForEmailAddress(email)
            return user != null
        }
        else {
            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(poolId)
                    .filter("email=\"${email}\"")
                    .build() as ListUsersRequest

            ListUsersResponse response = cognitoIdp.listUsers(request)
            return response.users()
        }
    }

    @Override
    boolean activateAccount(UserRecord user, GrailsParameterMap params) {
        def result = false
        if(user.locked) {
            result = enableUser(user)
        }
        if(!user.activated) {
            def request = AdminConfirmSignUpRequest.builder()
                    .username(user.userName)
                    .userPoolId(poolId)
                    .build()
            def response = cognitoIdp.adminConfirmSignUp(request as AdminConfirmSignUpRequest)
            result = isSuccessful(response)
        }
        return result
    }

    @Override
    PagedResult<UserRecord> listUsers(GrailsParameterMap params) {
        //max value for pagination in cognito is 60
        def max = Math.min(params.int('max', 20), 60)
        def nextPageToken = null

        Stream<UserType> users

        if (params.q) {

            ListUsersResponse emailResults = cognitoIdp.listUsers { builder ->
                builder.userPoolId(poolId)
                        .paginationToken(params.token ?: null)
                        .limit(max)
                        .filter("email ^= \"${params.q}\"")
            }

            ListUsersResponse givenNameResults = cognitoIdp.listUsers { builder ->
                builder.userPoolId(poolId)
                        .paginationToken(params.token ?: null)
                        .limit(max)
                        .filter("given_name ^= \"${params.q}\"")
            }

            ListUsersResponse familyNameResults = cognitoIdp.listUsers { builder ->
                builder.userPoolId(poolId)
                        .paginationToken(params.token ?: null)
                        .limit(max)
                        .filter("family_name ^= \"${params.q}\"")
            }

            users = Stream.concat(
                    emailResults.users().stream(),
                    Stream.concat(givenNameResults.users().stream(), familyNameResults.users().stream()))
                    .distinct()

        } else {

            ListUsersResponse results = cognitoIdp.listUsers { builder ->
                builder.userPoolId(poolId)
                        .paginationToken(params.token ?: null)
                        .limit(max)
            }

            users = results.users().stream()
            nextPageToken = results.paginationToken()
        }

        def list =  users.map { userType ->
            cognitoUserTypeToUserRecord(userType, true)
        }.toList()

        return new PagedResult<UserRecord>(list: list, count: null, nextPageToken: nextPageToken)
    }

    private UserRecord cognitoUserTypeToUserRecord(UserType userType, boolean findRoles = false) {
        def (Map<String, String> attributes, List<UserPropertyRecord> userProperties) =
            cognitoAttrsToUserPropertyRecords(userType.attributes(), cognitoIdp.adminGetUser(AdminGetUserRequest.builder()
                    .username(userType.username())
                    .userPoolId(poolId)
                    .build() as AdminGetUserRequest)?.userMFASettingList())

        def user = new UserRecord(
                id: userType.username(),
                dateCreated: Date.from(userType.userCreateDate()), lastUpdated: Date.from(userType.userLastModifiedDate()),
                activated: userType.userStatus() != UserStatusType.UNCONFIRMED, locked: !userType.enabled(),
                firstName: attributes['given_name'], lastName: attributes['family_name'],
                email: attributes['email'], userName: attributes['email'],
                userRoles: [],
                userProperties: userProperties)
        if (findRoles) {
            user.userRoles = rolesForUser(userType.username()).collect { new UserRoleRecord(user: user, role: it) }
        }
        return user
    }

    @Override
    Collection<UserRecord> listUsers() {
        ListUsersRequest request = ListUsersRequest.builder()
                .userPoolId(poolId)
                .build() as ListUsersRequest
        ListUsersResponse results = cognitoIdp.listUsers(request)

        return results.users().stream().map { userType ->
            cognitoUserTypeToUserRecord(userType, true)
        } as Collection<UserRecord>
    }

    @Override
    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {
        throw new NotImplementedException()
    }

    @Override
    UserRecord registerUser(GrailsParameterMap params) throws Exception {

        if(!params.email || isEmailInUse(params.email)){
            return null
        }

        def request = AdminCreateUserRequest.builder()
                .username(UUID.randomUUID().toString())
                .userPoolId(poolId)
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)


        Collection<AttributeType> userAttributes = new ArrayList<>()

        userAttributes.add(AttributeType.builder()
                .name('email')
                .value(params.email)
                .build())
        userAttributes.add(AttributeType.builder()
                .name('given_name')
                .value(params.firstName)
                .build())
        userAttributes.add(AttributeType.builder()
                .name('family_name')
                .value(params.lastName)
                .build())
        userAttributes.add(
                AttributeType.builder()
                        .name('email_verified')
                        .value('true')
                        .build()
        )

        params.findAll {customAttrs.contains(it.key) }
                .each {userAttributes.add(AttributeType.builder().name("custom:${it.key}").value(it.value as String).build()) }

        if (affiliationsEnabled && params.get('affiliation')) {
            userAttributes.add(AttributeType.builder().name("custom:affiliation").value(params.get('affiliation', '')).build())
        }
        request.userAttributes(userAttributes)

        def userResponse = cognitoIdp.adminCreateUser(request.build() as AdminCreateUserRequest)

        if (userResponse.user()) {

            UserRecord user = cognitoUserTypeToUserRecord(userResponse.user(), true)

            //add ROLE_USER role
            addUserRole(user.userName, "ROLE_USER")

            //disable user
            disableUser(user)

            return user
        }
        return null
    }

    @Override
    void clearTempAuthKey(UserRecord user) {
        def request = AdminUpdateUserAttributesRequest.builder()
                .username(user.userName)
                .userPoolId(poolId)
                .userAttributes(AttributeType.builder().name(TEMP_AUTH_KEY).value(null).build()).build()
        cognitoIdp.adminUpdateUserAttributes(request as AdminUpdateUserAttributesRequest)
    }

    @Override
    void updateProperties(UserRecord user, GrailsParameterMap params) {
        throw new NotImplementedException()
    }

    @Override
    void deleteUser(UserRecord user) {
        def request = AdminDeleteUserRequest.builder()
                .userPoolId(poolId)
                .username(user.userName)
                .build()
        cognitoIdp.adminDeleteUser(request as AdminDeleteUserRequest)
    }

    @Override
    UserRecord getUserById(String userId) {

        def userResponse
        Map<String, String> attributes
        List<UserPropertyRecord> userProperties

        if (userId == null || userId == "") {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        try {
            userResponse = cognitoIdp.adminGetUser(AdminGetUserRequest.builder()
                    .username(userId)
                    .userPoolId(poolId)
                    .build() as AdminGetUserRequest)
            (attributes, userProperties) = cognitoAttrsToUserPropertyRecords(userResponse.userAttributes(), userResponse.userMFASettingList())

            UserRecord user = new UserRecord(
                    id: userResponse.username(),
                    dateCreated: Date.from(userResponse.userCreateDate()), lastUpdated: Date.from(userResponse.userLastModifiedDate()),
                    activated: userResponse.userStatus() != UserStatusType.UNCONFIRMED, locked: !userResponse.enabled(),
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: attributes['email'],
                    userRoles: [],
                    userProperties: userProperties
            )

            user.userRoles = rolesForUser(userResponse.username()).collect { new UserRoleRecord(role: it, user: user) }

            return user
        } catch (UserNotFoundException e) {
            return null
        }
    }

    private static List cognitoAttrsToUserPropertyRecords(List<AttributeType> userAttributes, List<String> mfaSettings) {
        Map<String, String> attributes = userAttributes.collectEntries { [(it.name()): it.value()] }
        Collection<UserPropertyRecord> userProperties = userAttributes
                .findAll { !mainAttrs.contains(it.name()) }
                .collect {
                    new UserPropertyRecord(name: it.name().startsWith('custom:') ? it.name().substring(7) : it.name(), value: it.value())
                }
        userProperties.add(new UserPropertyRecord(name: "enableMFA", value: mfaSettings?.size() > 0))
        return [attributes, userProperties]
    }

    @Override
    UserRecord getUserByEmail(String email) {
        return getUserById(email)
    }

    @Override
    UserRecord getCurrentUser() {

        try {
            AccessToken accessToken = tokenService.getAuthToken(true)

            if(accessToken == null){
                return null
            }
            GetUserResponse userResponse = cognitoIdp.getUser(GetUserRequest.builder()
                    .accessToken(accessToken.value)
                    .build() as GetUserRequest)

            def (Map<String, String> attributes, List<UserPropertyRecord> userProperties) =
                cognitoAttrsToUserPropertyRecords(userResponse.userAttributes(), userResponse.userMFASettingList())

            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(poolId)
                    .filter("username = \"${userResponse.username()}\"")
                    .build() as ListUsersRequest
            def response = cognitoIdp.listUsers(request)

            UserRecord user = new UserRecord(
                    id: userResponse.username(),
                    dateCreated: Date.from(response.users()[0].userCreateDate()), lastUpdated: Date.from(response.users()[0].userLastModifiedDate()),
                    activated: response.users()[0].userStatus() != UserStatusType.UNCONFIRMED, locked: !response.users()[0].enabled(),
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: attributes['email'],
                    userProperties: userProperties
            )
            user.userRoles = rolesForUser(userResponse.username()).collect { new UserRoleRecord(user: user, role: it) }

            return user
        }
        catch (Exception e){
            log.error(e.getMessage())
            return null
        }
    }

    @Override
    Collection<UserRecord> findUsersForExport(List usersInRoles, Object includeInactive) {
        return null
    }

    @Override
    Map getUsersCounts(Locale locale) {
        Map jsonMap = [:]
        DescribeUserPoolRequest request = DescribeUserPoolRequest.builder()
                .userPoolId(poolId)
                .build() as DescribeUserPoolRequest
        def response = cognitoIdp.describeUserPool(request)
        jsonMap.totalUsers = response.userPool().estimatedNumberOfUsers()
        log.debug "jsonMap = ${jsonMap as JSON}"
        jsonMap
    }

    @Override
    List<String[]> countByProfileAttribute(String s,  Date startDate, Date endDate, Locale locale) {
        //TODO Need to find a way to search between dates
        def token
        def counts = [:]
        def results = cognitoIdp.listUsers(ListUsersRequest.builder()
                .userPoolId(poolId)
                .build() as ListUsersRequest
        )

        while (results) {
            def users = results.users()
            token = results.paginationToken()

            users.each {
                def value = it.attributes().find {att ->  att.name() == "custom:$s" }?.value()
                counts[value ?: ''] = ((counts[value ?: '']) ?: 0) +1
            }

            results = token ? cognitoIdp.listUsers(ListUsersRequest.builder()
                    .userPoolId(poolId)
                    .paginationToken(token)
                    .build() as ListUsersRequest) : null
        }
        def affiliations = locationService.affiliationSurvey(locale)

        return counts.collect { [affiliations[it.key] ?: it.key, it.value.toString()].toArray(new String[0]) as String[]}
    }

    @Override
    List<String[]> emailList(Date startDate, Date endDate) {
        // Initialize list to hold all filtered users across paginated calls
        def users = new ArrayList<UserType>()

        // Pagination logic
        def token = null
        while (true) {
            def response
            if (token) {
                response = cognitoIdp.listUsers(ListUsersRequest.builder()
                        .userPoolId(poolId)
                        .paginationToken(token)
                        .build() as ListUsersRequest)
            } else {
                response = cognitoIdp.listUsers(ListUsersRequest.builder()
                        .userPoolId(poolId)
                        .build() as ListUsersRequest)
            }

            // Filter users based on creation or last modified date and add to filtered_users list
            users.addAll(response.getUsers().findAll {
                (it.userCreateDate.after(startDate) && it.userCreateDate.before(endDate)) ||
                (it.userLastModifiedDate.after(startDate) && it.userLastModifiedDate.before(endDate))
            })

            token = response.paginationToken()
            if (!token) {
                break
            }
        }

        return users.collect { [it.attributes().find { it.name() == 'email' }.value(), it.userCreateDate(), it.userLastModifiedDate()].toArray(new String[0]) as String[]}
    }

    @Override
    Collection<RoleRecord> listRoles() {
        ListGroupsResponse result = cognitoIdp.listGroups(
                ListGroupsRequest.builder()
                        .userPoolId(poolId)
                        .limit(60)
                        .build() as ListGroupsRequest
        )

        return result.groups().collect { groupType ->
            new RoleRecord(role: (jwtProperties.getRolePrefix() + groupType.groupName()).toUpperCase(), description: groupType.description())
        }
    }

    @Override
    PagedResult<RoleRecord> listRoles(GrailsParameterMap params) {

        ListGroupsResponse result = cognitoIdp.listGroups(ListGroupsRequest.builder()
                .userPoolId(poolId)
                .nextToken(params.token ?: null)
                .build() as ListGroupsRequest)

        def roles = result.groups().collect { groupType ->
            new RoleRecord(role: (jwtProperties.getRolePrefix() + groupType.groupName()).toUpperCase(), description: groupType.description())
        }

        return new PagedResult<RoleRecord>(list: roles, count: null, nextPageToken: result.nextToken())
    }

    private List<RoleRecord> rolesForUser(String username) {
        def groupsResult = cognitoIdp.adminListGroupsForUser(
                AdminListGroupsForUserRequest.builder()
                        .username(username)
                        .userPoolId(poolId)
                        .build() as AdminListGroupsForUserRequest
        )

        return groupsResult.groups().collect { new RoleRecord(role: (jwtProperties.getRolePrefix() + it.groupName()).toUpperCase(), description: it.description()) }
    }

    @Override
    boolean addUserRole(String userId, String roleName) {

        String cognitoRoleName = getCognitoRoleName(roleName)

        if (checkGroupExists(cognitoRoleName)) {
            def addUserToGroupResult = cognitoIdp.adminAddUserToGroup(
                    AdminAddUserToGroupRequest.builder()
                            .username(userId)
                            .groupName(cognitoRoleName)
                            .userPoolId(poolId)
                            .build() as AdminAddUserToGroupRequest
            )

            return isSuccessful(addUserToGroupResult)
        }

        return false
    }

    @Override
    boolean removeUserRole(String userId, String roleName) {

        String cognitoRoleName = getCognitoRoleName(roleName)

        if (checkGroupExists(cognitoRoleName)) {
            def removeUserFromGroupResult = cognitoIdp.adminRemoveUserFromGroup(
                    AdminRemoveUserFromGroupRequest.builder()
                            .username(userId)
                            .groupName(cognitoRoleName)
                            .userPoolId(poolId)
                            .build() as AdminRemoveUserFromGroupRequest
            )

            return isSuccessful(removeUserFromGroupResult)
        }
        return false
    }

    private GroupType getCognitoGroup(String roleName, boolean addNewRole = false) {

        String cognitoRoleName = getCognitoRoleName(roleName)

        try {
            def getGroupResult = cognitoIdp.getGroup(
                    GetGroupRequest.builder()
                            .groupName(cognitoRoleName)
                            .userPoolId(poolId)
                            .build() as GetGroupRequest
            )
            return isSuccessful(getGroupResult) ? getGroupResult.group() : null
        }
        catch (ResourceNotFoundException e){

            if (addNewRole) {
                def roleInstance = new RoleRecord(role: cognitoRoleName, description: cognitoRoleName)
                def role = addRole(roleInstance)
                if (role) {
                    def getGroupResult = cognitoIdp.getGroup(
                            GetGroupRequest.builder()
                                    .groupName(cognitoRoleName)
                                    .userPoolId(poolId)
                                    .build() as GetGroupRequest
                    )
                    return isSuccessful(getGroupResult) ? getGroupResult.group() : null
                } else {
                    return null
                }
            }
            return null
        }
    }

    private boolean checkGroupExists(String roleName) {
        def group = getCognitoGroup(roleName, false)
        return group?.groupName() == getCognitoRoleName(roleName)
    }

    @Override
    void findScrollableUsersByUserName(GrailsParameterMap params, ResultStreamer resultStreamer) {
        def token = null
        resultStreamer.init()
        try {
            do {
                if(token) { params.token = token }
                def users = listUsers(params)
                users.list.each(resultStreamer.&offer)
                token = users.nextPageToken
            } while (token)
            resultStreamer.complete()
        } catch(e) {
            log.error('error streaming results', e)
        } finally {
            resultStreamer.finalise()
        }
    }

    @Override
    void findScrollableUsersByIdsAndRole(GrailsParameterMap params, ResultStreamer resultStreamer) {

        def ids = params.list('id')

        def groupName = getCognitoRoleName(params.role)

        def token = null
        resultStreamer.init()
        try {
            do {

                def builder = ListUsersInGroupRequest.builder()
                        .userPoolId(poolId)
                        .groupName(groupName)
                if (token) {
                    builder.nextToken(token)
                }
                ListUsersInGroupRequest request = builder.build() as ListUsersInGroupRequest

                def response = cognitoIdp.listUsersInGroup(request)

                def users = response.users()
                        .findAll {(!ids) || ids?.contains(it.username()) || ids?.contains(it.attributes().find{att -> att.name() == "email"}.value())}
                        .collect { userType -> cognitoUserTypeToUserRecord(userType, true) }

                users.each(resultStreamer.&offer)

                token = response.nextToken()
            } while (token)
            resultStreamer.complete()
        } catch(e) {
            log.error('error streaming results', e)
        } finally {
            resultStreamer.finalise()
        }
    }

    @Override
    void addRoles(Collection<RoleRecord> roleRecords) {
        roleRecords.each { addRole(it) }
    }

    //    *********** Property related services *************

    @Override
    UserPropertyRecord addOrUpdateProperty(UserRecord userRecord, String name, String value) {

        DescribeUserPoolRequest request = DescribeUserPoolRequest.builder()
                .userPoolId(poolId)
                .build() as DescribeUserPoolRequest
        def response = cognitoIdp.describeUserPool(request)
        if (response.userPool().schemaAttributes().find{it.name() =='custom:' + name} == null) {

            SchemaAttributeType schemaAttribute =
                    SchemaAttributeType.builder()
                            .attributeDataType(AttributeDataType.STRING)
                            .mutable(true)
                            .name(name)
                            .build()

            AddCustomAttributesRequest addAttrRequest =
                    AddCustomAttributesRequest.builder()
                            .userPoolId(poolId)
                            .customAttributes(schemaAttribute)
                            .build() as AddCustomAttributesRequest
            def addAttResponse = cognitoIdp.addCustomAttributes(addAttrRequest)
            if (isSuccessful(addAttResponse)) {

                def updateUserResponse = addCustomUserProperty(userRecord, name, value)

                if (isSuccessful(updateUserResponse)) {
                    return new UserPropertyRecord(user: userRecord, name: name, value: value)
                } else {
                    return null
                }
            } else {
                return null
            }
        }
        else{
            def updateUserResponse = addCustomUserProperty(userRecord, name, value)

            if (isSuccessful(updateUserResponse)) {
                return new UserPropertyRecord(user: userRecord, name: name, value: value)
            } else {
                return null
            }
        }
    }

    @Override
    void removeUserProperty(UserRecord userRecord, ArrayList<String> attributes) {
        attributes.each {
            addCustomUserProperty(userRecord, it, null)
        }
    }

    @Override
    List<UserPropertyRecord> searchProperty(UserRecord userRecord, String attribute) {
        List<UserPropertyRecord> propList = []

        if(userRecord && attribute) {
            propList.addAll(userRecord.userProperties.findAll { it.name == attribute })
        }
        else if(attribute){
            def token
            def results = cognitoIdp.listUsers(ListUsersRequest.builder()
                    .userPoolId(poolId)
                    .build() as ListUsersRequest)

            while (results) {
                def users = results.getUsers()
                token = results.getPaginationToken()

                users.each {
                    def value = it.attributes.find {att ->  att.name == "custom:$attribute" }?.value
                    if(value) {
                        propList.add(new UserPropertyRecord(user: cognitoUserTypeToUserRecord(it, false), name: attribute, value: value))
                    }
                }
                results = token ? cognitoIdp.listUsers(ListUsersRequest.builder()
                        .userPoolId(poolId)
                        .paginationToken(token)
                        .build() as ListUsersRequest) : null
            }
        }
        else{
            //cannot implement this since cognito does not support custom attribute search
            throw new NotImplementedException()
        }
        return propList
    }

    @Override
    RoleRecord addRole(RoleRecord roleRecord) {
        if (!checkGroupExists(roleRecord.role)) {
            String cognitoRoleName = getCognitoRoleName(roleRecord.role)
            def createGroupResult = cognitoIdp.createGroup(
                    CreateGroupRequest.builder()
                            .groupName(cognitoRoleName)
                            .description(roleRecord.description)
                            .userPoolId(poolId)
                            .build() as CreateGroupRequest
            )
            if (createGroupResult.group()) {
                return roleRecord
            } else {
                throw new RuntimeException("Couldn't create group")
            }
        } else {
            throw new RuntimeException("${roleRecord.role} already exists!")
        }
    }

    @Override
    List<String[]> listNamesAndEmails() {
        //Deprecated apis
        throw new NotImplementedException()
    }

    @Override
    List<String[]> listIdsAndNames() {
        //Deprecated apis
        throw new NotImplementedException()
    }

    @Override
    List<String[]> listUserDetails() {
        //Deprecated apis
        throw new NotImplementedException()
    }

    @Override
    PagedResult<UserRoleRecord> findUserRoles(String role, GrailsParameterMap params) {
        //max value for pagination in cognito is 60
        def max = Math.min(params.int('max', 5), 60)
        if (role) {
            def group = getCognitoGroup(role, false)
            if (group) {
                String cognitoRoleName = getCognitoRoleName(role)
                def listUsersInGroupResult = cognitoIdp.listUsersInGroup(
                        ListUsersInGroupRequest.builder()
                                .groupName(cognitoRoleName)
                                .limit(max)
                                .nextToken(params.token ?: null)
                                .userPoolId(poolId)
                                .build() as ListUsersInGroupRequest
                )
                if (isSuccessful(listUsersInGroupResult)) {

                    def roleRecord = new RoleRecord(role: (jwtProperties.getRolePrefix() + group.groupName()).toUpperCase(), description: group.description())
                    def userRoleInstanceList = listUsersInGroupResult.users().collect {
                        new UserRoleRecord(user: cognitoUserTypeToUserRecord(it), role: roleRecord)
                    }

                    return new PagedResult<UserRoleRecord>(list: userRoleInstanceList, count: null, nextPageToken: listUsersInGroupResult.nextToken())
                }
            } else {
                log.warn("$role does not exist, can't find users for it")
                return new PagedResult<UserRoleRecord>(list: [], count: 0, nextPageToken: null)
            }
        } else {
            throw new NotImplementedException("You must supply a role for Cognito")
        }
        return null
    }

    @Override
    def sendAccountActivation(UserRecord user) {
        //this email is sent via cognito
    }

    //    *********** MFA services *************

    @Override
    String getSecretForMfa() {
        AccessToken accessToken = tokenService.getAuthToken(true)

        if (accessToken == null) {
            throw new IllegalStateException("No current user available")
        }
        AssociateSoftwareTokenRequest request = AssociateSoftwareTokenRequest.builder()
                .accessToken(accessToken.value)
                .build() as AssociateSoftwareTokenRequest
        def response = cognitoIdp.associateSoftwareToken(request)
        if (response.secretCode()) {
            return response.secretCode()
        } else {
            throw new RuntimeException()
        }
    }

    @Override
    boolean verifyUserCode(String userCode) {
        AccessToken accessToken = tokenService.getAuthToken(true)

        if (accessToken == null) {
            throw new IllegalStateException("No current user available")
        }
        VerifySoftwareTokenRequest request = VerifySoftwareTokenRequest.builder()
                .accessToken(accessToken.value)
                .userCode(userCode)
                .build() as VerifySoftwareTokenRequest
        def response= cognitoIdp.verifySoftwareToken(request)
        return response.status() == VerifySoftwareTokenResponseType.SUCCESS
    }

    @Override
    boolean verifyUserAttribute(String attribute, String code) {
        AccessToken accessToken = tokenService.getAuthToken(true)

        if (accessToken == null) {
            throw new IllegalStateException("No current user available")
        }
        VerifyUserAttributeRequest request = VerifyUserAttributeRequest.builder()
                .accessToken(accessToken.value)
                .attributeName(attribute)
                .code(code)
                .build() as VerifyUserAttributeRequest
        def response= cognitoIdp.verifyUserAttribute(request)
        return isSuccessful(response)
    }

    @Override
    void enableMfa(String userId, boolean enable) {
        AdminSetUserMfaPreferenceRequest mfaRequest = AdminSetUserMfaPreferenceRequest.builder()
                .userPoolId(poolId)
                .username(userId)
                .softwareTokenMfaSettings(
                        SoftwareTokenMfaSettingsType.builder()
                                .enabled(enable)
                                .build()
                )
                .build() as AdminSetUserMfaPreferenceRequest
        def response = cognitoIdp.adminSetUserMFAPreference(mfaRequest)
        if (!isSuccessful(response)) {
            throw new RuntimeException("Couldn't set MFA preference")
        }
    }

    private static boolean isSuccessful(def result) {
        def code = result.sdkHttpResponse().statusCode()
        return code >= 200 && code < 300
    }

    @Override
    UserRecord findByUserNameOrEmail(GrailsParameterMap params) {
        def isUserNameUUID

        try{
            UUID.fromString(params.userName)
            isUserNameUUID = true
        }
        catch (Exception ex){
            isUserNameUUID = false
        }

        if(params.userName.isLong() || isUserNameUUID){
            return getUserById(params.userName)
        }
        else {
            params.q = params.userName
            params.max = 1
            return listUsers(params)?.list[0]
        }

    }

    def getUserDetailsFromIdList(List idList){

        List<UserType> users = []

        def baseRequest = ListUsersRequest.builder()
                .userPoolId(poolId)
                .limit(1)

        idList.forEach{
            def response = cognitoIdp.listUsers(
                    baseRequest
                            .filter("username = \"${it.toString()}\"")
                            .build() as ListUsersRequest
            )
            users.addAll(response.users())
        }

        return users.stream().map { userType ->
            cognitoUserTypeToUserRecord(userType, true)
        }.toList()
    }

    def addCustomUserProperty(UserRecord user, String name, String value){
        Collection<AttributeType> userAttributes = new ArrayList<>()

        userAttributes.add(AttributeType.builder().name('custom:' + name).value(value ?: "").build())

        AdminUpdateUserAttributesRequest updateUserRequest =
                AdminUpdateUserAttributesRequest.builder()
                        .userPoolId(poolId)
                        .username(user.userName)
                        .userAttributes(userAttributes)
                        .build() as AdminUpdateUserAttributesRequest

        return cognitoIdp.adminUpdateUserAttributes(updateUserRequest)
    }

    String getCognitoRoleName(String role) {
        List socialLoginRoles = socialLoginGroups.collect { jwtProperties.getRolePrefix() + it.toUpperCase()}

        if(socialLoginRoles.contains(role)) {
            return socialLoginGroups.find{r -> role.contains(r.toUpperCase())}
        }
        return role.contains(jwtProperties.getRolePrefix()) ? role.split(jwtProperties.getRolePrefix())[1].toLowerCase() : role
    }

    private static void streamUserResults(ResultStreamer resultStreamer, List<UserRecord> results) {
        resultStreamer.init()
        try {
            results.each {
                resultStreamer.offer(it)
            }
        } finally {
            resultStreamer.finalise()
        }
        resultStreamer.complete()
    }
}
