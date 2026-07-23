package au.org.ala.userdetails

import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model.CreateApiKeyRequest
import software.amazon.awssdk.services.apigateway.model.CreateUsagePlanKeyRequest
import software.amazon.awssdk.services.apigateway.model.GetApiKeysRequest
import software.amazon.awssdk.services.apigateway.model.GetApiKeysResponse

class AWSApikeyService implements IApikeyService {

    ApiGatewayClient apiGatewayIdp
    IUserService userService

    AWSApikeyService(ApiGatewayClient apiGatewayIdp, IUserService userService) {
        this.apiGatewayIdp = apiGatewayIdp
        this.userService = userService
    }

    @Override
    List<String> generateApikey(String usagePlanId) {
        if (!usagePlanId) {
            throw new IllegalArgumentException("No usage plan id to generate api key")
        }

        def currentUser = userService.currentUser

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .enabled(true)
                .customerId(currentUser.userId)
                .name("API key for user " + currentUser.userId)
                .build() as CreateApiKeyRequest
        def response = apiGatewayIdp.createApiKey(request)

        if (response.sdkHttpResponse().statusCode() == 201) {
            //add api key to usage plan
            CreateUsagePlanKeyRequest usagePlanKeyRequest = CreateUsagePlanKeyRequest.builder()
                    .keyId(response.id())
                    .keyType("API_KEY")
                    .usagePlanId(usagePlanId)
                    .build() as CreateUsagePlanKeyRequest
            apiGatewayIdp.createUsagePlanKey(usagePlanKeyRequest)

            return getApikeys(currentUser.userId)
        } else {
            throw new RuntimeException("Could not generate api key")
        }
    }

    @Override
    List<String> getApikeys(String userId) {
        GetApiKeysRequest getApiKeysRequest = GetApiKeysRequest.builder()
                .customerId(userId)
                .includeValues(true)
                .build() as GetApiKeysRequest
        GetApiKeysResponse response = apiGatewayIdp.getApiKeys(getApiKeysRequest)
        if (response.sdkHttpResponse().statusCode() == 200) {
            return response.items()*.value()
        } else {
            throw new RuntimeException("Error retrieving apikeys")
        }
    }
}
