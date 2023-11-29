package au.org.ala.userdetails

interface IApikeyService {

    /***
     * This method is used to generate an api key for a given aws apigateway usage plan
     * @param usagePlanId
     * @return
     */
    List<String> generateApikey(String usagePlanId)

    /***
     * This method is used to get registered api keys of a user
     * @param userId
     * @return
     */
    List<String> getApikeys(String userId)

}
