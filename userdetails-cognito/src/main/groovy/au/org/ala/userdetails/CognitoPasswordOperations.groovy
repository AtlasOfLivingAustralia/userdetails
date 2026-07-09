package au.org.ala.userdetails

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.IUser
import au.org.ala.web.OidcClientProperties
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminResetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

@Slf4j
class CognitoPasswordOperations implements IPasswordOperations {

    CognitoIdentityProviderClient cognitoIdp
    String poolId
    OidcClientProperties oidcClientProperties

    @Override
    boolean resetPassword(IUser<?> user, String newPassword, boolean isPermanent, String confirmationCode) {
        if(!user || !newPassword) {
            return false
        }

        try {
            if (confirmationCode == null) {
                def request = AdminSetUserPasswordRequest.builder()
                        .username(user.email)
                        .userPoolId(poolId)
                        .password(newPassword)
                        .permanent(isPermanent)
                        .build() as AdminSetUserPasswordRequest

                def response = cognitoIdp.adminSetUserPassword(request)
                return response.sdkHttpResponse().statusCode() == 200
            } else {
                def request = ConfirmForgotPasswordRequest.builder()
                        .username(user.email)
                        .password(newPassword)
                        .confirmationCode(confirmationCode)
                        .clientId(oidcClientProperties.getClientId())
                        .secretHash(calculateSecretHash(oidcClientProperties.getClientId(), oidcClientProperties.getSecret(), user.email))
                        .build() as ConfirmForgotPasswordRequest
                def response = cognitoIdp.confirmForgotPassword(request)
                return response.sdkHttpResponse().statusCode() == 200
            }
        } catch(Exception e) {
            return false
        }
    }

    @Override
    void resetAndSendTemporaryPassword(IUser<?> user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        def request = AdminResetUserPasswordRequest.builder()
                .username(user.email)
                .userPoolId(poolId)
                .build() as AdminResetUserPasswordRequest

        cognitoIdp.adminResetUserPassword(request)
    }

    @Override
    boolean checkUserPassword(IUser<?> user, String password) {
        def clientId = oidcClientProperties.getClientId()
        def secret = oidcClientProperties.getSecret()
        try {
            def authResult = cognitoIdp.adminInitiateAuth(AdminInitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .clientId(clientId)
                    .userPoolId(poolId)
                    .authParameters([
                            USERNAME   : user.userName,
                            PASSWORD   : password,
                            SECRET_HASH: calculateSecretHash(clientId, secret, user.userName)
                    ])
                    .build() as AdminInitiateAuthRequest
            )
            return authResult.authenticationResult() != null
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
