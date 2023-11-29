package au.org.ala.userdetails.gorm
/**
 * API key implementation
 */
class ApiKey implements Serializable {
    static belongsTo = [user: User]

    String apiKey // not encoded
    String apiSecret // encoded
    Date dateCreated
    Date lastUpdated

    static mapping = {
        table 'api_key'
        user column:  'userid'
    }
    static constraints = {
        apiKey nullable: false, blank: false
        apiSecret nullable: true
    }
}
