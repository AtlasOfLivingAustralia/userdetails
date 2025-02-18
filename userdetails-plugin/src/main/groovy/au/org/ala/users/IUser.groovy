package au.org.ala.users

import java.sql.Timestamp

/**
 * Read only view of a User object that can be implemented by various backends
 * @param <T> The type of the id field
 */
interface IUser<T extends Serializable> {

    T getId()

    String getUserId()

    String getFirstName()
    String getLastName()

    String getUserName()
    String getEmail()

    Date getDateCreated()
    Date getLastUpdated()

    Timestamp getLastLogin()

    Boolean getActivated()
    Boolean getLocked()

    String getTempAuthKey()
    String getDisplayName()

    /**
     * Associated roles property.  This is named differently to the original GORM implementations fields due to the
     * way GORM discovers associated properties.
     * @return
     */
    Set<? extends IUserRole<? extends IUser<? extends Serializable>, ? extends IRole>> getRoles()

    /**
     * Associated additional attributes property.  This is named differently to the original GORM implementations fields due to the
     * way GORM discovers associated properties.
     * @return
     */
    Set<? extends IUserProperty<? extends IUser<? extends Serializable>>> getAdditionalAttributes()

    def propsAsMap()

    default String toString(){
        return "${firstName} ${lastName} <${email}>"
    }
}
