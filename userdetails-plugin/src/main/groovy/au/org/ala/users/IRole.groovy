package au.org.ala.users

/**
 * Read only view of a role object
 */
interface IRole {

    String getRole()
    String getDescription()

    default String toString() {
        return role
    }
}
