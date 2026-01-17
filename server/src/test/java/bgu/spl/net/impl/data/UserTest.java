package bgu.spl.net.impl.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void testUserCreation() {
        User user = new User(1, "alice", "secret123");
        
        assertEquals("alice", user.name);
        assertEquals("secret123", user.password);
        assertEquals(1, user.getConnectionId());
        assertFalse(user.isLoggedIn());
    }

    @Test
    void testLoginLogout() {
        User user = new User(1, "bob", "pass456");
        
        assertFalse(user.isLoggedIn());
        
        user.login();
        assertTrue(user.isLoggedIn());
        
        user.logout();
        assertFalse(user.isLoggedIn());
    }

    @Test
    void testSetConnectionId() {
        User user = new User(1, "charlie", "pwd789");
        
        assertEquals(1, user.getConnectionId());
        
        user.setConnectionId(42);
        assertEquals(42, user.getConnectionId());
    }

    @Test
    void testMultipleLoginLogoutCycles() {
        User user = new User(1, "dave", "test");
        
        for (int i = 0; i < 5; i++) {
            user.login();
            assertTrue(user.isLoggedIn());
            user.logout();
            assertFalse(user.isLoggedIn());
        }
    }
}
