import org.junit.Test;
import org.starrel.submitee.auth.InternalAccountRealm;

public class Tests {

    @Test
    public void hashPassword() {
        String hash;
        System.out.println(hash = InternalAccountRealm.hashPassword("woshizhu123"));
        assert InternalAccountRealm.verifyPassword("woshizhu123", hash);
        assert !InternalAccountRealm.verifyPassword("woshizhu1234", hash);
    }
}
