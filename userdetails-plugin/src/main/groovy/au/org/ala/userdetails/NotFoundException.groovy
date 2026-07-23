package au.org.ala.userdetails

class NotFoundException extends RuntimeException {
    NotFoundException(String message) {
        super(message, null, false, false)
    }
}
