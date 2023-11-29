package au.org.ala.userdetails

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.IUser
import au.org.ala.users.UserRecord
import au.org.ala.web.OidcClientProperties
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest
import com.amazonaws.services.cognitoidp.model.AdminResetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AuthFlowType
import com.amazonaws.services.cognitoidp.model.ConfirmForgotPasswordRequest
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

@Slf4j
class CognitoPasswordOperations implements IPasswordOperations {

    AWSCognitoIdentityProvider cognitoIdp
    String poolId
    OidcClientProperties oidcClientProperties

    @Override
    boolean resetPassword(IUser<?> user, String newPassword, boolean isPermanent, String confirmationCode) {
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
                request.clientId = oidcClientProperties.getClientId()
                request.secretHash = calculateSecretHash(oidcClientProperties.getClientId(), oidcClientProperties.getSecret(), user.email)
                def response = cognitoIdp.confirmForgotPassword(request)
                return response.getSdkHttpMetadata().httpStatusCode == 200
            }
        } catch(Exception e) {
            return false
        }
    }

    @Override
    void resetAndSendTemporaryPassword(IUser<?> user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        def request = new AdminResetUserPasswordRequest()
        request.username = user.email
        request.userPoolId = poolId

        cognitoIdp.adminResetUserPassword(request)
    }

    @Override
    boolean checkUserPassword(IUser<?> user, String password) {
        def clientId = oidcClientProperties.getClientId()
        def secret = oidcClientProperties.getSecret()
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
    String getResetPasswordUrl(IUser<?> user) {
        return null
    }

    @Override
    String getPasswordResetView() {
        return "passwordReset"
    }

}
