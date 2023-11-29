package au.org.ala.users

interface IAuthorisedSystem<T> {
    T getId()
    String getHost()
    String getDescription()
}
