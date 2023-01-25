package au.org.ala.userdetails

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.UserRecord
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest
import com.amazonaws.services.cognitoidp.model.AdminResetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AuthFlowType
import com.amazonaws.services.cognitoidp.model.ChangePasswordRequest
import com.amazonaws.services.cognitoidp.model.ChangePasswordResult
import com.amazonaws.services.cognitoidp.model.ConfirmForgotPasswordRequest
import com.nimbusds.oauth2.sdk.token.AccessToken
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

@Slf4j
class CognitoPasswordOperations implements IPasswordOperations {

    GrailsApplication grailsApplication

    TokenService tokenService
    AWSCognitoIdentityProvider cognitoIdp
    String poolId

    @Override
    boolean resetPassword(UserRecord user, String newPassword, boolean isPermanent, String confirmationCode) {
        if(!user || !newPassword) {
            return false
        }

        try {
            if (confirmationCode == null) {
                def request = new AdminSetUserPasswordRequest()
                request.username = user.email
                request.userPoolId = poolId
                request.password = newPassword
                request.permanent = isPermanent

                def response = cognitoIdp.adminSetUserPassword(request)
                return response.getSdkHttpMetadata().httpStatusCode == 200
            } else {
                def request = new ConfirmForgotPasswordRequest().withUsername(user.email)
                request.password = newPassword
                request.confirmationCode = confirmationCode
                request.clientId = grailsApplication.config.getProperty('security.oidc.client-id')
                request.secretHash = calculateSecretHash(grailsApplication.config.getProperty('security.oidc.client-id'),
                        grailsApplication.config.getProperty('security.oidc.secret'), user.email)
                def response = cognitoIdp.confirmForgotPassword(request)
                return response.getSdkHttpMetadata().httpStatusCode == 200
            }
        } catch(Exception e) {
            return false
        }
    }

    @Override
    boolean updatePassword(UserRecord user, String currentPassword, String newPassword) {

        if (!user || !currentPassword || !newPassword) {
            return false
        }

        AccessToken accessToken = tokenService.getAuthToken(true)

        if (accessToken == null){
            return null
        }

        try {

            ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest()
                    .withAccessToken(accessToken as String)
                    .withPreviousPassword(currentPassword)
                    .withProposedPassword(newPassword)


            ChangePasswordResult changePasswordResult = cognitoIdp.changePassword(changePasswordRequest)
            return changePasswordResult.sdkHttpMetadata.httpStatusCode == 200

        } catch (Exception e) {

            return false
        }
    }

    @Override
    void resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        def request = new AdminResetUserPasswordRequest()
        request.username = user.email
        request.userPoolId = poolId

        cognitoIdp.adminResetUserPassword(request)
    }

    @Override
    boolean checkUserPassword(UserRecord user, String password) {
        // TODO this is untested
        def clientId = grailsApplication.config.getProperty('security.oidc.client-id')
        def secret = grailsApplication.config.getProperty('security.oidc.secret')
        try {
            def authResult = cognitoIdp.adminInitiateAuth(new AdminInitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .withClientId(clientId)
                    .withUserPoolId(poolId)
                    .withAuthParameters([
                            USERNAME   : user.userName,
                            PASSWORD   : password,
                            SECRET_HASH: calculateSecretHash(clientId, secret, user.userName)
                    ])
            )
            // TODO Test this and sign out?
            return authResult.authenticationResult != null
        } catch (e) {
            log.debug("Exception caught while checking user password", e)
            return false
        }

    }

    static String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        try {
            byte[] rawHmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, userPoolClientSecret).hmac("$userName$userPoolClientId")
            return Base64.getEncoder().encodeToString(rawHmac)
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating ")
        }
    }

    @Override
    String getResetPasswordUrl(UserRecord user) {
        return null
    }

    @Override
    String getPasswordResetView() {
        return "passwordResetCognito"
    }

}
