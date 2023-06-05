package au.org.ala.userdetails

import com.amazonaws.AmazonWebServiceResult
import com.amazonaws.ResponseMetadata
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.CreateUserPoolClientRequest
import com.amazonaws.services.cognitoidp.model.CreateUserPoolClientResult
import com.amazonaws.services.cognitoidp.model.DescribeUserPoolClientRequest
import com.amazonaws.services.cognitoidp.model.UpdateUserPoolClientRequest
import com.amazonaws.services.cognitoidp.model.UserPoolClientType
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.QueryRequest

class CognitoApplicationService implements IApplicationService {

    IUserService userService
    AWSCognitoIdentityProvider cognitoIdp
    String poolId

//    Config config
    List<String> supportedIdentityProviders
    List<String> authFlows
    List<String> clientScopes
    List<String> galahCallbackURLs

    AmazonDynamoDB dynamoDB
    String dynamoDBTable
    String dynamoDBPK
    String dynamoDBSK

    List<ApplicationRecord> listApplicationsForUser(String userId) {
        def qr = new QueryRequest()
                .withTableName(dynamoDBTable)
                .withKeyConditionExpression("$dynamoDBPK := :userId")
                .withExpressionAttributeValues([userId: new AttributeValue(userId)])
        def result = dynamoDB.query(qr)

        if (result.sdkHttpMetadata.httpStatusCode == 200) {
            result.items.collect { itemToApplication(it) }
        } else {
            throw new RuntimeException("Could not list clients for user $userId")
        }
    }

    private ApplicationRecord itemToApplication(item) {
        def clientId = item.get(dynamoDBSK).getS()

        def client = cognitoIdp.describeUserPoolClient(
                new DescribeUserPoolClientRequest()
                        .withUserPoolId(poolId)
                        .withClientId(clientId)
        )
        userPoolClientToApplication(client.userPoolClient)
    }

    private ApplicationRecord userPoolClientToApplication(UserPoolClientType userPoolClient) {
        def name = userPoolClient.clientName
        def clientId = userPoolClient.clientId
        def secret = userPoolClient.clientSecret
        def callbackUrls = userPoolClient.callbackURLs
        def allowedFlows = userPoolClient.allowedOAuthFlows
        userPoolClient.logoutURLs
        userPoolClient.defaultRedirectURI

        def type
        if (callbackUrls.containsAll(galahCallbackURLs) && callbackUrls.size() == galahCallbackURLs.size()) {
            type = ApplicationType.GALAH
        } else if (allowedFlows.contains('client_credentials')) {
            type = ApplicationType.M2M
        } else if (allowedFlows.contains('code')) {
            if (userPoolClient.clientSecret) {
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
                type: type
        )
    }

    List<String> listClientIdsForUser(String userId) {
        listApplicationsForUser(userId).collect { it.clientId }
    }

    private def addClientIdForUser(String userId, String clientId) {
        def putResponse = dynamoDB.putItem(
                new PutItemRequest(dynamoDBTable, [(dynamoDBPK): new AttributeValue(userId), (dynamoDBSK): new AttributeValue(clientId)]))
        if (putResponse.sdkHttpMetadata.httpStatusCode != 200) {
            throw new RuntimeException("Couldn't add mapping for $clientId to $userId")
        }
    }

    private def getClientByUserIdAndClientId(String userId, String clientId) {
        def result = dynamoDB.getItem(dynamoDBTable, [(dynamoDBPK): new AttributeValue(userId), (dynamoDBSK): new AttributeValue(clientId)])
        return result.item
    }

    private def isUserOwnsClientId(String userId, String clientId) {
        return getClientByUserIdAndClientId(userId, clientId) != null
    }

    @Override
    ApplicationRecord generateClient(String userId, ApplicationRecord applicationRecord) {
        CreateUserPoolClientRequest request =  new CreateUserPoolClientRequest().withUserPoolId(poolId)
        request.clientName = applicationRecord.name
        // TODO enable user consent
        if (applicationRecord.type == ApplicationType.M2M) {
            request.generateSecret = true
            request.allowedOAuthFlows = ["client_credentials"]
        } else {
            request.generateSecret = applicationRecord.type != ApplicationType.PUBLIC
            request.allowedOAuthFlows = ["code"]
        }
        request.supportedIdentityProviders = new ArrayList<>(supportedIdentityProviders)
        request.preventUserExistenceErrors = "ENABLED"
        request.explicitAuthFlows = new ArrayList<>(authFlows)
        request.allowedOAuthFlowsUserPoolClient = true

        def scopes = new ArrayList<>(clientScopes)

        if (scopes) {
            request.allowedOAuthScopes = scopes
        }

        request.callbackURLs = new ArrayList<>(applicationRecord.callbacks.findAll())
        if (applicationRecord.type == ApplicationType.GALAH) {
            request.callbackURLs.addAll(galahCallbackURLs)
        }

        CreateUserPoolClientResult response = cognitoIdp.createUserPoolClient(request)

        if (isSuccessful(response)) {
            //update user custom attribute with new clientId
//            userService.addOrUpdateProperty(userService.currentUser, "clientId", response.userPoolClient.clientId)
            def clientId = response.userPoolClient.clientId
            addClientIdForUser(userId, clientId)
            //return applicationRecord.tap { it.clientId = clientId; it.secret = response.userPoolClient.clientSecret }
            return userPoolClientToApplication(response.userPoolClient)
        } else {
            throw new RuntimeException("Could not generate client")
        }
    }

    @Override
    void updateClient(String userId, ApplicationRecord applicationRecord) {
        if (!isUserOwnsClientId(userId, applicationRecord.clientId)) {
            throw new IllegalArgumentException("${applicationRecord.clientId} not found")
        }
        def request = new UpdateUserPoolClientRequest().withUserPoolId(poolId)
        request.withClientId(applicationRecord.clientId)
        request.withClientName(applicationRecord.name)
        if (applicationRecord.type == ApplicationType.M2M) {
            request.allowedOAuthFlows = ["client_credentials"]
        } else {
            request.allowedOAuthFlows = ["code"]
        }

        request.callbackURLs = new ArrayList<>(applicationRecord.callbacks.findAll())
        if (applicationRecord.type == ApplicationType.GALAH) {
            request.callbackURLs.addAll(galahCallbackURLs)
        }

        def response = cognitoIdp.updateUserPoolClient(request)
        if (!isSuccessful(response)) {
            throw new RuntimeException("Could not update client $applicationRecord.clientId")
        }
    }

    @Override
    ApplicationRecord findClientByClientId(String userId, String clientId) {
        return itemToApplication(getClientByUserIdAndClientId(userId, clientId))
    }

    private static boolean isSuccessful(AmazonWebServiceResult<? extends ResponseMetadata> result) {
        def code = result.sdkHttpMetadata.httpStatusCode
        return code >= 200 && code < 300
    }
}
