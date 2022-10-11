import com.saveourtool.save.utils.AuthenticationDetails
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication

fun Authentication.userId() = (this.details as AuthenticationDetails).id

fun Authentication.userName() = this.extractUserNameAndIdentitySource().first

fun Authentication.identitySource() = this.extractUserNameAndIdentitySource().second

fun Authentication.extractUserNameAndIdentitySource(): Pair<String, String> {
    val identitySource = (this.details as AuthenticationDetails).identitySource
    if (identitySource == null || !this.name.startsWith("$identitySource:")) {
        throw BadCredentialsException(this.name)
    }
    val name = this.name.drop(identitySource.length + 1)
    return name to identitySource
}