package bgu.spl.net.impl.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Database login/logout with Python SQL server.
 * Requires sql_server.py to be running on 127.0.0.1:7778
 */
class DatabaseLoginTest {
    private static Process sqlServerProcess;
    private Database db;

    @BeforeAll
    static void startSqlServer() throws Exception {
        // Start Python SQL server
        String pythonScript = "../data/sql_server.py";
        File scriptFile = new File(pythonScript);
        
        if (!scriptFile.exists()) {
            throw new FileNotFoundException("SQL server script not found at: " + scriptFile.getAbsolutePath());
        }

        // Use MSYS2 UCRT64 Python which has sqlite3 working on this machine.
        ProcessBuilder pb = new ProcessBuilder("C:/msys64/ucrt64/bin/python.exe", scriptFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        sqlServerProcess = pb.start();
        
        // Give server time to start
        Thread.sleep(2000);
        
        // Verify server started
        if (!sqlServerProcess.isAlive()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(sqlServerProcess.getInputStream()));
            String line;
            System.err.println("SQL server failed to start:");
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
            throw new RuntimeException("SQL server failed to start");
        }
    }

    @AfterAll
    static void stopSqlServer() {
        if (sqlServerProcess != null && sqlServerProcess.isAlive()) {
            sqlServerProcess.destroy();
            try {
                sqlServerProcess.waitFor();
            } catch (InterruptedException e) {
                sqlServerProcess.destroyForcibly();
            }
        }
    }

    @BeforeEach
    void setUp() {
        db = Database.getInstance();
    }

    @Test
    void testRegisterNewUser() {
        // login also creates a new user when not exists
        LoginStatus status = db.login(1, "testuser1", "password123");
        assertEquals(LoginStatus.ADDED_NEW_USER, status, "Should add and log in new user");
        
        // Verify user is in database
        User user = db.getUserByName("testuser1");
        assertNotNull(user);
        assertEquals("testuser1", user.name);
        assertEquals("password123", user.password);
        assertTrue(user.isLoggedIn());
    }

    @Test
    void testRegisterDuplicateUser() {
        db.login(2, "duplicate", "pass1"); // first time creates
        LoginStatus status = db.login(3, "duplicate", "pass2");
        // Existing user already logged in -> ALREADY_LOGGED_IN
        assertEquals(LoginStatus.ALREADY_LOGGED_IN, status, "Duplicate login while logged in should be rejected");
    }

    @Test
    void testLoginValidCredentials() {
        // First login creates the user
        db.login(4, "validuser", "validpass");
        db.logout(4);
        
        LoginStatus status = db.login(5, "validuser", "validpass");
        assertEquals(LoginStatus.LOGGED_IN_SUCCESSFULLY, status);
        
        User user = db.getUserByName("validuser");
        assertTrue(user.isLoggedIn());
        assertEquals(5, user.getConnectionId());
    }

    @Test
    void testLoginInvalidPassword() {
        db.login(6, "user2", "correctpass");
        db.logout(6);
        
        LoginStatus status = db.login(7, "user2", "wrongpass");
        assertEquals(LoginStatus.WRONG_PASSWORD, status);
        
        User user = db.getUserByName("user2");
        assertFalse(user.isLoggedIn());
    }

    @Test
    void testLoginNonexistentUser() {
        LoginStatus status = db.login(8, "ghost", "pass");
        assertEquals(LoginStatus.ADDED_NEW_USER, status);
    }

    @Test
    void testLoginAlreadyLoggedIn() {
        db.login(9, "user3", "pass3");
        
        LoginStatus status = db.login(10, "user3", "pass3");
        assertEquals(LoginStatus.ALREADY_LOGGED_IN, status);
    }

    @Test
    void testLogoutByConnectionId() {
        db.login(11, "user4", "pass4");
        
        User user = db.getUserByConnectionId(11);
        assertTrue(user.isLoggedIn());
        
        db.logout(11);
        assertFalse(user.isLoggedIn());
        
        // Connection ID should be removed from map
        assertNull(db.getUserByConnectionId(11));
    }

    @Test
    void testLogoutNotLoggedIn() {
        db.logout(999); // Should not throw exception
    }

    @Test
    void testGetUserByConnectionId() {
        db.login(12, "user5", "pass5");
        
        User user = db.getUserByConnectionId(12);
        assertNotNull(user);
        assertEquals("user5", user.name);
        
        db.logout(12);
        assertNull(db.getUserByConnectionId(12));
    }

    @Test
    void testMultipleUsersSimultaneous() {
        LoginStatus aliceStatus = db.login(13, "alice", "alicepass");
        LoginStatus bobStatus = db.login(14, "bob", "bobpass");
        assertEquals(LoginStatus.ADDED_NEW_USER, aliceStatus);
        assertEquals(LoginStatus.ADDED_NEW_USER, bobStatus);
        
        User alice = db.getUserByConnectionId(13);
        User bob = db.getUserByConnectionId(14);
        
        assertNotNull(alice);
        assertNotNull(bob);
        assertTrue(alice.isLoggedIn());
        assertTrue(bob.isLoggedIn());
        
        db.logout(13);
        assertFalse(alice.isLoggedIn());
        assertTrue(bob.isLoggedIn());
    }
}
