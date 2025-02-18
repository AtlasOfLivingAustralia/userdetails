package au.org.ala.userdetails

import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.CreateApiKeyRequest
import com.amazonaws.services.apigateway.model.CreateUsagePlanKeyRequest
import com.amazonaws.services.apigateway.model.GetApiKeysRequest
import com.amazonaws.services.apigateway.model.GetApiKeysResult

class AWSApikeyService implements IApikeyService {

    AmazonApiGateway apiGatewayIdp
    IUserService userService

    AWSApikeyService(AmazonApiGateway apiGatewayIdp, IUserService userService) {
        this.apiGatewayIdp = apiGatewayIdp
        this.userService = userService
    }

    @Override
    List<String> generateApikey(String usagePlanId) {
        if (!usagePlanId) {
            throw new IllegalArgumentException("No usage plan id to generate api key")
        }

        def currentUser = userService.currentUser

        CreateApiKeyRequest request = new CreateApiKeyRequest()
        request.enabled = true
        request.customerId = currentUser.userId
        request.name = "API key for user " + currentUser.userId
        def response = apiGatewayIdp.createApiKey(request)

        if (response.getSdkHttpMetadata().httpStatusCode == 201) {
            //add api key to usage plan
            CreateUsagePlanKeyRequest usagePlanKeyRequest = new CreateUsagePlanKeyRequest()
            usagePlanKeyRequest.keyId = response.id
            usagePlanKeyRequest.keyType = "API_KEY"
            usagePlanKeyRequest.usagePlanId = usagePlanId
            apiGatewayIdp.createUsagePlanKey(usagePlanKeyRequest)

            return getApikeys(currentUser.userId)
        } else {
            throw new RuntimeException("Could not generate api key")
        }
    }

    @Override
    List<String> getApikeys(String userId) {
        GetApiKeysRequest getApiKeysRequest = new GetApiKeysRequest().withCustomerId(userId).withIncludeValues(true)
        GetApiKeysResult response = apiGatewayIdp.getApiKeys(getApiKeysRequest)
        if (response.getSdkHttpMetadata().httpStatusCode == 200) {
            return response.items*.value
        } else {
            throw new RuntimeException("Error retrieving apikeys")
        }
    }
}
