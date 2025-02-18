package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.ApplicationRecord
import au.org.ala.userdetails.ApplicationType
import au.org.ala.userdetails.IApplicationService
import au.org.ala.userdetails.PatternUtils
import au.org.ala.userdetails.secrets.RandomStringGenerator
import com.mongodb.ClientSessionOptions
import com.mongodb.client.MongoClient
import org.apache.commons.lang3.StringUtils
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonIgnore
import org.bson.codecs.pojo.annotations.BsonProperty

import java.util.regex.Pattern

import static com.mongodb.client.model.Filters.*


class GormApplicationService implements IApplicationService {

    MongoClient mongoClient
    String mongoDbName
    String mongoCollectionName

    RandomStringGenerator randomStringGenerator

    List<String> supportedIdentityProviders
    List<String> authFlows
    List<String> clientScopes
    List<String> galahCallbackURLs
    List<String> tokensCallbackURLs

    private ClientSessionOptions getClientSessionOptions() {
        ClientSessionOptions.builder().causallyConsistent(true).build()
    }

    @Override
    List<String> listClientIdsForUser(String userId) {
        listApplicationsForUser(userId).collect { it.clientId }
    }

    @Override
    ApplicationRecord generateClient(String userId, ApplicationRecord applicationRecord) {
        def service = new Cas66Service()

        service.properties.owner = [ 'values': [userId], '_class': 'org.apereo.cas.services.DefaultRegisteredServiceProperty']

        // TODO ensure this is unique
        service.clientId = randomStringGenerator.getNewString() // applicationRecord.clientId
        // TODO check user consent screen works
        service.bypassApprovalPrompt = false

        applyApplicationRecordToService(applicationRecord, service)

//        service.supportedIdentityProviders = new ArrayList<>(supportedIdentityProviders) // TODO
//        request.explicitAuthFlows = new ArrayList<>(authFlows)
//        request.allowedOAuthFlowsUserPoolClient = true

        service.scopes = new ArrayList<>(clientScopes)
        service.evaluationOrder = 999999 // evaluate after admin defined

        mongoClient.startSession(clientSessionOptions).withCloseable { session ->

            def collection = mongoClient.getDatabase(mongoDbName).getCollection(mongoCollectionName, Cas66Service)

            String clientId
            boolean matched
            do {
                clientId = randomStringGenerator.getNewString()
                matched = collection.countDocuments(session, eq('clientId', clientId)) > 0
            } while (matched) // XXX there's a race condition here but since CAS doesn't apply a unique constraint on clientId there's not much we can do.

            service.clientId = clientId
            service.id = (long)service.hashCode() // XXX This is what CAS does

            def insertResult  = collection.insertOne(session, service)
            if (insertResult.insertedId == null) {
                throw new RuntimeException("Couldn't create client")
            }
        }
        
        return service.toApplicationRecord(galahCallbackURLs, tokensCallbackURLs)
    }

    void updateClient(String userId, ApplicationRecord applicationRecord) {
        def clientId = applicationRecord.clientId

        def existing = getExistingClient(userId, clientId)
        if (!existing) {
            throw new IllegalStateException("${clientId} doesn't exist")
        }

        applyApplicationRecordToService(applicationRecord, existing, false)

        mongoClient.startSession(clientSessionOptions).withCloseable { session ->
            def collection = mongoClient.getDatabase(mongoDbName).getCollection(mongoCollectionName, Cas66Service)
            def updateResult  = collection.replaceOne(session, and(eq('clientId', clientId), eq('properties.owner', userId)), existing)
            if (updateResult.modifiedCount != 1l) {
                throw new RuntimeException("Couldn't update ${clientId}")
            }
        }

        return //existing.toApplicationRecord(galahCallbackURLs, tokensCallbackURLs)
    }

    @Override
    ApplicationRecord findClientByClientId(String userId, String clientId) {
        return getExistingClient(userId, clientId).toApplicationRecord(galahCallbackURLs, tokensCallbackURLs)
    }

    @Override
    boolean deleteApplication(String userId, String clientId) {
        def existing = getExistingClient(userId, clientId)
        if (!existing) {
            throw new IllegalStateException("${clientId} doesn't exist")
        }

        mongoClient.startSession(clientSessionOptions).withCloseable { session ->
            def collection = mongoClient.getDatabase(mongoDbName).getCollection(mongoCollectionName, Cas66Service)
            def deleteResult  = collection.findOneAndDelete(session, and(eq('clientId', clientId), eq('properties.owner', userId)))
            if (!deleteResult) {
                throw new RuntimeException("Couldn't delete ${clientId}")
            }
        }
        return true
    }

    private void applyApplicationRecordToService(ApplicationRecord applicationRecord, Cas66Service service, boolean generateSecret = true) {
        service.name = applicationRecord.name
        service.description = "A placeholder description"

        def callbacks = new ArrayList<>(applicationRecord.callbacks.findAll{it != ""})
        if(applicationRecord.needTokenAppAsCallback) {
            callbacks.addAll(tokensCallbackURLs)
        }
        // CAS interprets service IDs as a regex but to prevent confusion, user
        // registered applications will be treated as exact matches
        service.serviceId = callbacks.collect { it.trim() }.findAll().collect(Pattern.&quote).join('|')

        if (applicationRecord.type == ApplicationType.M2M) {
            service.applicationType = 'native' // This field doesn't appear to be used in CAS but we fill it in anyway, just in case
            if (generateSecret) {
                service.clientSecret = randomStringGenerator.getNewString()
            } else if (!service.clientSecret) {
                service.clientSecret = randomStringGenerator.getNewString()
            }
            service.supportedGrantTypes = ["client_credentials"] as Set
        } else {
            if (applicationRecord.type == ApplicationType.PUBLIC) {
                service.applicationType = 'web'
                if (generateSecret) {
                    service.clientSecret = ''
                } else if (service.clientSecret) {
                    service.clientSecret = ''
                }
            } else {
                service.applicationType = 'native'
                if (generateSecret) {
                    service.clientSecret = randomStringGenerator.getNewString()
                } else if (!service.clientSecret) {
                    service.clientSecret = randomStringGenerator.getNewString()
                }
            }
            service.supportedGrantTypes = ["code", "refresh_token"] as Set
        }
    }

    Cas66Service getExistingClient(String userId, String clientId) {
        return mongoClient.startSession(clientSessionOptions).withCloseable { session ->
            def collection = mongoClient.getDatabase(mongoDbName).getCollection(mongoCollectionName, Cas66Service)
            def results = collection.find(session, and(eq('clientId', clientId), eq('properties.owner.values', userId)), Cas66Service)
            results.first()
        }
    }

    @Override
    List<ApplicationRecord> listApplicationsForUser(String userId) {
        return mongoClient.startSession(clientSessionOptions).withCloseable { session ->
            def collection = mongoClient.getDatabase(mongoDbName).getCollection(mongoCollectionName, Cas66Service)
            def results = collection.find(session, eq('properties.owner.values', userId), Cas66Service)
            results.collect { it.toApplicationRecord(galahCallbackURLs, tokensCallbackURLs) }
        }
    }
}

interface CasOidcService {
    ApplicationRecord toApplicationRecord(List<String> galahCallbackURLs, List<String> tokensCallbackURLs)

    void setName(String name);
    void setDescription(String name);
    void setServiceId(String serviceId);
    void setApplicationType(String applicationType)
    void setClientSecret(String clientSecret)
    void setClientId(String clientId)
    void setSupportedGrantTypes(Set<String> supportedGrantTypes)
}

/**
 * POJO that models the Mongo Document for a CAS 6.6 service definition.
 * Default values are provided for the various settings that are not
 * service specific.
 */
class Cas66Service implements CasOidcService {

    /**
     *   {
     _id: Long("1658775974578"),
     jwksCacheDuration: Long("0"),
     tokenEndpointAuthenticationMethod: 'client_secret_basic',
     signIdToken: true,
     encryptIdToken: false,
     applicationType: 'web',
     subjectType: 'PUBLIC',
     dynamicallyRegistered: false,
     scopes: [ 'openid', 'email', 'profile', 'ala', 'roles' ],
     clientSecret: 'PcAMr9gqogyyGCWW70SH3JctxvdC9qi3tbAN',
     clientId: 'fwnDb4hXUtERBJTjd9lwQMADobSemiQocIeL',
     bypassApprovalPrompt: true,
     generateRefreshToken: true,
     renewRefreshToken: false,
     jwtAccessToken: true,
     codeExpirationPolicy: {
     numberOfUses: Long("0"),
     _class: 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthCodeExpirationPolicy'
     },
     accessTokenExpirationPolicy: {
     _class: 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthAccessTokenExpirationPolicy'
     },
     refreshTokenExpirationPolicy: {
     _class: 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthRefreshTokenExpirationPolicy'
     },
     deviceTokenExpirationPolicy: {
     _class: 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthDeviceTokenExpirationPolicy'
     },
     supportedGrantTypes: [],
     supportedResponseTypes: [],
     serviceId: 'http://test.org/.*',
     name: 'Some test OIDC RP',
     description: 'Test test test',
     expirationPolicy: {
     deleteWhenExpired: false,
     notifyWhenDeleted: false,
     notifyWhenExpired: false,
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceExpirationPolicy'
     },
     acceptableUsagePolicy: {
     enabled: true,
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceAcceptableUsagePolicy'
     },
     proxyPolicy: {
     _class: 'org.apereo.cas.services.RefuseRegisteredServiceProxyPolicy'
     },
     proxyTicketExpirationPolicy: {
     numberOfUses: Long("0"),
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceProxyTicketExpirationPolicy'
     },
     serviceTicketExpirationPolicy: {
     numberOfUses: Long("0"),
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceServiceTicketExpirationPolicy'
     },
     webflowInterruptPolicy: {
     enabled: true,
     forceExecution: 'UNDEFINED',
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceWebflowInterruptPolicy'
     },
     evaluationOrder: 7,
     usernameAttributeProvider: {
     canonicalizationMode: 'NONE',
     encryptUsername: false,
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceUsernameProvider'
     },
     logoutType: 'BACK_CHANNEL',
     environments: [],
     attributeReleasePolicy: {
     allowedAttributes: [],
     principalAttributesRepository: {
     mergingStrategy: 'MULTIVALUED',
     attributeRepositoryIds: [],
     ignoreResolvedAttributes: false,
     _class: 'org.apereo.cas.authentication.principal.DefaultPrincipalAttributesRepository'
     },
     consentPolicy: {
     status: 'UNDEFINED',
     order: 0,
     _class: 'org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy'
     },
     authorizedToReleaseCredentialPassword: false,
     authorizedToReleaseProxyGrantingTicket: false,
     excludeDefaultAttributes: false,
     authorizedToReleaseAuthenticationAttributes: true,
     order: 0,
     _class: 'org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy'
     },
     multifactorPolicy: {
     multifactorAuthenticationProviders: [],
     failureMode: 'UNDEFINED',
     bypassEnabled: false,
     forceExecution: false,
     bypassTrustedDeviceEnabled: false,
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceMultifactorPolicy'
     },
     matchingStrategy: {
     _class: 'org.apereo.cas.services.FullRegexRegisteredServiceMatchingStrategy'
     },
     accessStrategy: {
     order: 0,
     enabled: true,
     ssoEnabled: true,
     delegatedAuthenticationPolicy: {
     allowedProviders: [],
     permitUndefined: true,
     exclusive: false,
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceDelegatedAuthenticationPolicy'
     },
     requireAllAttributes: true,
     requiredAttributes: {},
     rejectedAttributes: {},
     caseInsensitive: false,
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy'
     },
     authenticationPolicy: {
     requiredAuthenticationHandlers: [],
     excludedAuthenticationHandlers: [],
     criteria: {
     tryAll: false,
     _class: 'org.apereo.cas.services.AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria'
     },
     _class: 'org.apereo.cas.services.DefaultRegisteredServiceAuthenticationPolicy'
     },
     properties: {},
     contacts: [],
     _class: 'org.apereo.cas.services.OidcRegisteredService'
     }
     */

    /**
     * CAS service defintion discriminator
     */
    @BsonProperty("_class")
    String cls = 'org.apereo.cas.services.OidcRegisteredService'

    @BsonId()
//    @BsonRepresentation(BsonType.INT64)
    @BsonProperty("_id")
    Long id

    /**
     * CAS service fields relevant to OIDC services
     */
    @BsonProperty("name")
    String name
    String description

    String serviceId
    int evaluationOrder

    Long jwksCacheDuration = 0
    
    String tokenEndpointAuthenticationMethod = 'client_secret_basic'

    boolean signIdToken = true
    boolean encryptIdToken = false
    String applicationType = 'web'
    String subjectType = 'PUBLIC' // TODO Should this be PAIRWISE for 3rd party apps?
    boolean dynamicallyRegistered = false
    Set<String> scopes = [ 'openid', 'offline_access' ]
    String clientSecret
    String clientId
    boolean bypassApprovalPrompt = false
    boolean generateRefreshToken = false
    boolean renewRefreshToken = false
    boolean jwtAccessToken = true

    Map<String, ?> codeExpirationPolicy = [
            'numberOfUsers': Long.valueOf(0),
            '_class': 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthCodeExpirationPolicy'
    ]
    Map<String, ?> accessTokenExpirationPolicy = ['_class': 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthAccessTokenExpirationPolicy']
    Map<String, ?> refreshTokenExpirationPolicy = ['_class': 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthRefreshTokenExpirationPolicy']
    Map<String, ?> deviceTokenExpirationPolicy = ['_class': 'org.apereo.cas.support.oauth.services.DefaultRegisteredServiceOAuthDeviceTokenExpirationPolicy']
    Set<String> supportedGrantTypes = []// TODO
    Set<String> supportedResponseTypes = [] // TODO

    /**
     * Generic CAS service fields with default values
     */
    Map<String, ?> expirationPolicy = [
        deleteWhenExpired: false,
        notifyWhenDeleted: false,
        notifyWhenExpired: false,
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceExpirationPolicy'
    ]

    Map<String, ?> acceptableUsagePolicy = [
        enabled: true,
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceAcceptableUsagePolicy'
    ]
    Map<String, ?> proxyPolicy = [
        _class: 'org.apereo.cas.services.RefuseRegisteredServiceProxyPolicy'
    ]
    Map<String, ?> proxyTicketExpirationPolicy = [
        numberOfUses: Long.valueOf(0),
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceProxyTicketExpirationPolicy'
    ]
    Map<String, ?> serviceTicketExpirationPolicy = [
        numberOfUses: Long.valueOf(0),
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceServiceTicketExpirationPolicy'
    ]
    Map<String, ?> webflowInterruptPolicy = [
        enabled: true,
        forceExecution: 'UNDEFINED',
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceWebflowInterruptPolicy'
    ]
    Map<String, ?> usernameAttributeProvider = [ // TODO Enable this for 3rd parties?
        canonicalizationMode: 'NONE',
        encryptUsername: false,
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceUsernameProvider'
    ]
    String logoutType = 'BACK_CHANNEL'
    List<String> environments = []
    Map<String, ?> attributeReleasePolicy = [
        allowedAttributes: [],
        principalAttributesRepository: [
            mergingStrategy: 'MULTIVALUED',
            attributeRepositoryIds: [],
            ignoreResolvedAttributes: false,
            _class: 'org.apereo.cas.authentication.principal.DefaultPrincipalAttributesRepository'
        ],
        consentPolicy: [
            status: 'UNDEFINED',
            order: 0,
            _class: 'org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy'
        ],
        authorizedToReleaseCredentialPassword: false,
        authorizedToReleaseProxyGrantingTicket: false,
        excludeDefaultAttributes: false,
        authorizedToReleaseAuthenticationAttributes: true,
        order: 0,
        _class: 'org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy'
    ]
    Map<String, ?> multifactorPolicy = [
        multifactorAuthenticationProviders: [],
        failureMode: 'UNDEFINED',
        bypassEnabled: false,
        forceExecution: false,
        bypassTrustedDeviceEnabled: false,
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceMultifactorPolicy'
    ]
    Map<String, ?> matchingStrategy = [
        _class: 'org.apereo.cas.services.FullRegexRegisteredServiceMatchingStrategy'
    ]
    Map<String, ?> accessStrategy = [
        order: 0,
        enabled: true,
        ssoEnabled: true,
        delegatedAuthenticationPolicy: [
            allowedProviders: [],
            permitUndefined: true,
            exclusive: false,
            _class: 'org.apereo.cas.services.DefaultRegisteredServiceDelegatedAuthenticationPolicy'
        ],
        requireAllAttributes: true,
        requiredAttributes: [:],
        rejectedAttributes: [:],
        caseInsensitive: false,
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy'
    ]
    Map<String, ?> authenticationPolicy = [
        requiredAuthenticationHandlers: [],
        excludedAuthenticationHandlers: [],
        criteria: [
            tryAll: false,
            _class: 'org.apereo.cas.services.AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria'
        ],
        _class: 'org.apereo.cas.services.DefaultRegisteredServiceAuthenticationPolicy'
    ]
    Map<String, ?> properties = [:]
    List<String> contacts = [] // TODO add this for consent screen

    @Override
    @BsonIgnore
    ApplicationRecord toApplicationRecord(List<String> galahCallbackURLs, List<String> tokensCallbackURLs) {
        def callbacks = StringUtils.split(this.serviceId, '|').collect {PatternUtils.unquotePattern(it) }.toList()

        def type
        if (supportedGrantTypes.contains('client_credentials')) {
            type = ApplicationType.M2M
        } else if (supportedGrantTypes.contains('code')) {
            if (clientSecret) {
                type = ApplicationType.CONFIDENTIAL
            } else {
                type = ApplicationType.PUBLIC
            }

        } else {
            type = ApplicationType.UNKNOWN
        }

        return new ApplicationRecord(
                name: this.name,
                callbacks: callbacks,
                clientId: this.clientId,
                secret: this.clientSecret,
                type: type,
                needTokenAppAsCallback: callbacks?.containsAll(tokensCallbackURLs)
        )
    }
}
