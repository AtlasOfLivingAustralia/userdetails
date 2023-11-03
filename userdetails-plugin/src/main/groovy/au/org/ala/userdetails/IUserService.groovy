/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.users.IRole
import au.org.ala.users.IUser
import au.org.ala.users.IUserProperty
import au.org.ala.users.IUserRole
import grails.web.servlet.mvc.GrailsParameterMap

interface IUserService<U extends IUser<? extends Serializable>, P extends IUserProperty<U>, R extends IRole, UR extends IUserRole<U, R>> {

    //

    U newUser(GrailsParameterMap params)
    R newRole(GrailsParameterMap params)
//    UserPropertyRecord newProperty(GrailsParameterMap params)
//    UserRoleRecord newRole(GrailsParameterMap params)


    //    *********** User related services *************

    /**
     * Update user method intended for user self service updates.  Things like
     * activation and locking should not be available through this method.
     * This method should also notify a user of changes they made to their account.
     *
     * @param userId The user id
     * @param params The request params with the updates
     * @param locale The user's locale
     * @return true if the update succeeded
     */
    boolean updateUser(String userId, GrailsParameterMap params, Locale locale)

    /**
     * Admin user update, allows additional settings such as locked, activated, etc
     * to be set.
     *
     * No user notification is required from this method.
     *
     * @param userId The user id
     * @param params The request params with the updates
     * @param locale The user's locale
     * @return true if the update succeeded
     */
    boolean adminUpdateUser(String userId, GrailsParameterMap params, Locale locale)

    boolean disableUser(U user)

    boolean enableUser(U user)

    boolean isActive(String email)

    boolean isLocked(String email)

    boolean isEmailInUse(String newEmail)

    boolean activateAccount(U user, GrailsParameterMap params)

    /***
     * This method can be used to search users
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     * @param params
     * @return
     */
    PagedResult<U> listUsers(GrailsParameterMap params)

    Collection<U> listUsers()

    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody)

    U registerUser(GrailsParameterMap params) throws Exception

    void updateProperties(U user, GrailsParameterMap params)

    void deleteUser(U user)

    U getUserById(String userId)

    U getUserByEmail(String email)

    /**
     * This service method returns the UserRecord object for the current user.
     */
    U getCurrentUser()

    Collection<U> findUsersForExport(List usersInRoles, includeInactive)

    /**
     * Calculate the number of active users (not locked and is activated), as well as the number
     * of active users 1 year ago (for comparison).
     *
     * @return Map jsonMap
     */
    Map getUsersCounts(Locale locale)

    List<String[]> countByProfileAttribute(String s, Date date, Locale locale)

    List<String[]> emailList(Date startDate, Date endDate)

    /***
     * This method can be used to get users by username
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     * @param params
     * @param resultStreamer
     */
    void findScrollableUsersByUserName(GrailsParameterMap params, ResultStreamer resultStreamer)

    /***
     * This method can be used to get users by role and ids
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     * @param params
     * @param resultStreamer
     */
    void findScrollableUsersByIdsAndRole(GrailsParameterMap params, ResultStreamer resultStreamer)

    def getUserDetailsFromIdList(List idList)

    U findByUserNameOrEmail(GrailsParameterMap params)

    List<String[]> listNamesAndEmails()

    List<String[]> listIdsAndNames()

    List<String[]> listUserDetails()

    //    *********** Role services *************

    Collection<R> listRoles()

    /**
     * Retrieve a list of roles, paged by the params in the argument.
     *
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     *
     * @param params The parameters that may be used in the search
     * @return A result with the list of roles and either the total count or the next page token.
     */
    PagedResult<R> listRoles(GrailsParameterMap params)

    /**
     * Add a role with `rolename` to user identified by `userid`
     *
     * @param userId The users id
     * @param roleName The name of the role to add
     * @return True if the operation succeeded or false otherwise
     */
    boolean addUserRole(String userId, String roleName)

    /**
     * Remove a role with `rolename` from user identified by `userid`
     *
     * @param userId The users id
     * @param roleName The name of the role to add
     * @return True if the operation succeeded or false otherwise
     */
    boolean removeUserRole(String userId, String roleName)

    /**
     * Add a list of roles to the system.
     *
     * @param roleRecords The list of RoleRecords to add
     */
    void addRoles(Collection<R> roleRecords)

    /**
     * Add a single role to the system.
     *
     * @param roleRecord The RoleRecord to add
     */

    R addRole(R roleRecord)

    /**
     * Find a list of users for a given role.
     *
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     *
     * @param role The role name to get the list of users for
     * @param params The paging parameters for the request
     */
    PagedResult<UR> findUserRoles(String role, GrailsParameterMap params)

    //    *********** account related services *************

    void clearTempAuthKey(U user)

    def sendAccountActivation(U user)

    //    *********** MFA services *************

    String getSecretForMfa()

    boolean verifyUserCode(String userCode)

    void enableMfa(String userId, boolean enable)

//    *********** Property related services *************

    P addOrUpdateProperty(U userRecord, String name, String value)

    void removeUserProperty(U userRecord, ArrayList<String> attributes)

    List<P> searchProperty(U userRecord, String attribute)
}