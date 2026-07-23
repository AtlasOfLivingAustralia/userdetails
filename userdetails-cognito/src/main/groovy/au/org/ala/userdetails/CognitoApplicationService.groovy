package au.org.ala.userdetails

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType
import software.amazon.awssdk.services.cognitoidentityprovider.model.OAuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import groovy.util.logging.Slf4j

@Slf4j
class CognitoApplicationService implements IApplicationService {

    IUserService userService
    CognitoIdentityProviderClient cognitoIdp
    String poolId

//    Config config
    List<String> supportedIdentityProviders
    List<ExplicitAuthFlowsType> authFlows
    List<String> clientScopes
    List<String> galahCallbackURLs
    List<String> tokensCallbackURLs

    DynamoDbClient dynamoDbClient
    String dynamoDBTable
    String dynamoDBPK
    String dynamoDBSK

    List<ApplicationRecord> listApplicationsForUser(String userId) {
        def qr = QueryRequest.builder()
                .tableName(dynamoDBTable)
                .keyConditionExpression("$dynamoDBPK = :userId")
                .expressionAttributeValues([":userId": AttributeValue.builder().s(userId).build()])
                .build() as QueryRequest
        def result = dynamoDbClient.query(qr)

        if (result.sdkHttpResponse().statusCode() == 200) {
            result.items().collect { itemToApplication(it) }
        } else {
            throw new RuntimeException("Could not list clients for user $userId")
        }
    }

    private ApplicationRecord itemToApplication(item) {
        def clientId = item.get(dynamoDBSK).s()

        def client = cognitoIdp.describeUserPoolClient(
                DescribeUserPoolClientRequest.builder()
                        .userPoolId(poolId)
                        .clientId(clientId)
                        .build() as DescribeUserPoolClientRequest
        )
        userPoolClientToApplication(client.userPoolClient())
    }

    private ApplicationRecord userPoolClientToApplication(UserPoolClientType userPoolClient) {
        def name = userPoolClient.clientName()
        def clientId = userPoolClient.clientId()
        def secret = userPoolClient.clientSecret()
        def callbackUrls = userPoolClient.callbackURLs()
        def allowedFlows = userPoolClient.allowedOAuthFlows()
        userPoolClient.logoutURLs()
        userPoolClient.defaultRedirectURI()

        def type
        if (allowedFlows.contains(OAuthFlowType.CLIENT_CREDENTIALS)) {
            type = ApplicationType.M2M
        } else if (allowedFlows.contains(OAuthFlowType.CODE)) {
            if (userPoolClient.clientSecret()) {
                type = ApplicationType.CONFIDENTIAL
            } else {
                type = ApplicationType.PUBLIC
            }
        } else {
            type = ApplicationType.UNKNOWN
        }

        return new ApplicationRecord(
                name: name,
                clientId: clientId,
                secret: secret,
                callbacks: callbackUrls,
                type: type,
                needTokenAppAsCallback: callbackUrls?.containsAll(tokensCallbackURLs)
        )
    }

    List<String> listClientIdsForUser(String userId) {
        listApplicationsForUser(userId).collect { it.clientId }
    }

    private def addClientIdForUser(String userId, String clientId) {
        def putResponse = dynamoDbClient.putItem(
                PutItemRequest.builder()
                        .tableName(dynamoDBTable)
                        .item([(dynamoDBPK): AttributeValue.builder().s(userId).build(), (dynamoDBSK): AttributeValue.builder().s(clientId).build()])
                        .build() as PutItemRequest)
        if (putResponse.sdkHttpResponse().statusCode() != 200) {
            throw new RuntimeException("Couldn't add mapping for $clientId to $userId")
        }
    }

    private def deleteClientIdForUser(String userId, String clientId) {
        def deleteResponse = dynamoDbClient.deleteItem(
                DeleteItemRequest.builder()
                        .tableName(dynamoDBTable)
                        .key([(dynamoDBPK): AttributeValue.builder().s(userId).build(), (dynamoDBSK): AttributeValue.builder().s(clientId).build()])
                        .build() as DeleteItemRequest)
        if (deleteResponse.sdkHttpResponse().statusCode() != 200) {
            throw new RuntimeException("Couldn't delete mapping for $clientId to $userId")
        }
    }

    private def getClientByUserIdAndClientId(String userId, String clientId) {
        def result = dynamoDbClient.getItem(
                GetItemRequest.builder()
                        .tableName(dynamoDBTable)
                        .key([(dynamoDBPK): AttributeValue.builder().s(userId).build(), (dynamoDBSK): AttributeValue.builder().s(clientId).build()])
                        .build() as GetItemRequest)
        return result.item()
    }

    private def isUserOwnsClientId(String userId, String clientId) {
        return getClientByUserIdAndClientId(userId, clientId) != null
    }

    @Override
    ApplicationRecord generateClient(String userId, ApplicationRecord applicationRecord) {
        def requestBuilder = CreateUserPoolClientRequest.builder().userPoolId(poolId)
        requestBuilder.clientName(applicationRecord.name)
        // TODO enable user consent
        if (applicationRecord.type == ApplicationType.M2M) {
            requestBuilder.generateSecret(true)
            requestBuilder.allowedOAuthFlows([OAuthFlowType.CLIENT_CREDENTIALS])
        } else {
            requestBuilder.generateSecret(applicationRecord.type == ApplicationType.CONFIDENTIAL) //do not need secret for public clients
            requestBuilder.allowedOAuthFlows([OAuthFlowType.CODE])
        }
        requestBuilder.supportedIdentityProviders(new ArrayList<>(supportedIdentityProviders))
        requestBuilder.preventUserExistenceErrors("ENABLED")
        requestBuilder.explicitAuthFlows(new ArrayList<>(authFlows))
        requestBuilder.allowedOAuthFlowsUserPoolClient(true)

        def scopes = new ArrayList<>(clientScopes)

        if (scopes && applicationRecord.type != ApplicationType.M2M) {
            requestBuilder.allowedOAuthScopes(scopes)
        }
        if(applicationRecord.type == ApplicationType.M2M) {
            requestBuilder.allowedOAuthScopes(["ala/attrs"])
        }

        def callbackUrls = new ArrayList<>(applicationRecord.callbacks.findAll{it != ""})
        if (applicationRecord.type == ApplicationType.M2M) {
            callbackUrls = null
        }
        else if(applicationRecord.needTokenAppAsCallback) {
            callbackUrls.addAll(tokensCallbackURLs)
        }
        if (callbackUrls) {
            requestBuilder.callbackURLs(callbackUrls)
        }

        try {
            CreateUserPoolClientResponse response = cognitoIdp.createUserPoolClient(requestBuilder.build() as CreateUserPoolClientRequest)

            if (isSuccessful(response)) {
                def clientId = response.userPoolClient().clientId()
                addClientIdForUser(userId, clientId)
                return userPoolClientToApplication(response.userPoolClient())
            } else {
                throw new RuntimeException("Could not generate client")
            }
        }
        catch (Exception e) {
            log.error(e.getMessage(), e)
            throw new RuntimeException("Could not create client")
        }
    }

    @Override
    void updateClient(String userId, ApplicationRecord applicationRecord) {
        if (!isUserOwnsClientId(userId, applicationRecord.clientId)) {
            throw new IllegalArgumentException("${applicationRecord.clientId} not found")
        }
        def requestBuilder = UpdateUserPoolClientRequest.builder()
                .userPoolId(poolId)
                .clientId(applicationRecord.clientId)
                .clientName(applicationRecord.name)
        requestBuilder.supportedIdentityProviders(new ArrayList<>(supportedIdentityProviders))
        requestBuilder.preventUserExistenceErrors("ENABLED")
        requestBuilder.explicitAuthFlows(new ArrayList<>(authFlows))
        requestBuilder.allowedOAuthFlowsUserPoolClient(true)

        if (applicationRecord.type == ApplicationType.M2M) {
            requestBuilder.allowedOAuthFlows([OAuthFlowType.CLIENT_CREDENTIALS])
        } else {
            requestBuilder.allowedOAuthFlows([OAuthFlowType.CODE])
        }

        def scopes = new ArrayList<>(clientScopes)

        if (scopes && applicationRecord.type != ApplicationType.M2M) {
            requestBuilder.allowedOAuthScopes(scopes)
        }
        if(applicationRecord.type == ApplicationType.M2M) {
            requestBuilder.allowedOAuthScopes(["ala/attrs"])
        }

        def callbackUrls = new ArrayList<>(applicationRecord.callbacks.findAll{it != ""})
        if (applicationRecord.type == ApplicationType.M2M) {
            callbackUrls = null
        }
        else if(applicationRecord.needTokenAppAsCallback) {
            callbackUrls.addAll(tokensCallbackURLs)
        }
        if (callbackUrls) {
            requestBuilder.callbackURLs(callbackUrls)
        }

        try {
            def response = cognitoIdp.updateUserPoolClient(requestBuilder.build() as UpdateUserPoolClientRequest)
            if (!isSuccessful(response)) {
                throw new RuntimeException("Could not update client $applicationRecord.clientId")
            }
        }
        catch (Exception e) {
            log.error(e.getMessage(), e)
            throw new RuntimeException("Could not update client")
        }
    }

    @Override
    ApplicationRecord findClientByClientId(String userId, String clientId) {
        return itemToApplication(getClientByUserIdAndClientId(userId, clientId))
    }

    private static boolean isSuccessful(def result) {
        def code = result.sdkHttpResponse().statusCode()
        return code >= 200 && code < 300
    }

    @Override
    boolean deleteApplication(String userId, String clientId){
        if (!isUserOwnsClientId(userId, clientId)) {
            throw new IllegalArgumentException("${clientId} not found")
        }
        def request = DeleteUserPoolClientRequest.builder()
                .userPoolId(poolId)
                .clientId(clientId)
                .build() as DeleteUserPoolClientRequest

        def response = cognitoIdp.deleteUserPoolClient(request)
        if (!isSuccessful(response)) {
            throw new RuntimeException("Could not delete client $clientId")
        }
        else{
            deleteClientIdForUser(userId, clientId)
            return true
        }
    }
}
