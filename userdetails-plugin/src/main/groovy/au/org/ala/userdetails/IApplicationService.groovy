package au.org.ala.userdetails

interface IApplicationService {

    /**
     * List all client ids attached to a given user
     * @param userId The user id
     * @return The list of client ids for the user
     */
    List<String> listClientIdsForUser(String userId)

    /***
     * This method is used to generate an oauth client
     * @param userId
     * @return
     */
    ApplicationRecord generateClient(String userId, ApplicationRecord applicationRecord)

    List<ApplicationRecord> listApplicationsForUser(String s)

    void updateClient(String userId, ApplicationRecord applicationRecord)

    ApplicationRecord findClientByClientId(String userId, String clientId)

    boolean deleteApplication(String userId, String clientId)
}