package au.org.ala.users

/**
 * Read only view of a user to role mapping.
 */
interface IUserRole<U extends IUser<? extends Serializable>, R extends IRole> {

    /**
     * Associated user property.  This is named differently to the original GORM implementations fields due to the
     * way GORM discovers associated properties.
     * @return
     */
    U getOwner()
    /**
     * Associated user property.  This is named differently to the original GORM implementations fields due to the
     * way GORM discovers associated properties.
     * @return
     */
    R getRoleObject()

    default String toString() {
        return roleObject.role
    }
}
