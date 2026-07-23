package au.org.ala.users

/**
 * Read only view of a user's single additional attribute
 * @param <U>
 */
interface IUserProperty<U extends IUser<? extends Serializable>> {

    /**
     * Associated user property.  This is named differently to the original GORM implementations fields due to the
     * way GORM discovers associated properties.
     * @return
     */
    U getOwner()
    String getName()
    String getValue()

    default String toString() {
        return "${name}:${value}"
    }

}
