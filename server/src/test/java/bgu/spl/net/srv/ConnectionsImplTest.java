package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectionsImplTest {
    private ConnectionsImpl<String> connections;
    private List<String> sentMessages;
    private MockConnectionHandler handler1;
    private MockConnectionHandler handler2;

    // Mock connection handler to capture sent messages
    private class MockConnectionHandler implements ConnectionHandler<String> {
        private final List<String> receivedMessages = new ArrayList<>();

        @Override
        public void send(String msg) {
            receivedMessages.add(msg);
        }

        @Override
        public void close() {
            // No-op for test
        }

        public List<String> getReceivedMessages() {
            return receivedMessages;
        }

        public void clearMessages() {
            receivedMessages.clear();
        }
    }

    @BeforeEach
    void setUp() {
        connections = new ConnectionsImpl<>();
        sentMessages = new ArrayList<>();
        handler1 = new MockConnectionHandler();
        handler2 = new MockConnectionHandler();
    }

    @Test
    void testAddConnectionAndSend() {
        connections.addConnection(1, handler1);
        
        boolean result = connections.send(1, "Hello");
        
        assertTrue(result);
        assertEquals(1, handler1.getReceivedMessages().size());
        assertEquals("Hello", handler1.getReceivedMessages().get(0));
    }

    @Test
    void testSendToNonexistentConnection() {
        boolean result = connections.send(999, "Message");
        
        assertFalse(result);
    }

    @Test
    void testSubscribeAndBroadcast() {
        connections.addConnection(1, handler1);
        connections.addConnection(2, handler2);
        
        connections.subscribe("general", 1, 10);
        connections.subscribe("general", 2, 20);
        
        connections.send("general", "MESSAGE\ndestination:/topic/general\nbody:test\n\n");
        
        assertEquals(1, handler1.getReceivedMessages().size());
        assertEquals(1, handler2.getReceivedMessages().size());
        
        String msg1 = handler1.getReceivedMessages().get(0);
        String msg2 = handler2.getReceivedMessages().get(0);
        
        assertTrue(msg1.contains("subscription:10"));
        assertTrue(msg2.contains("subscription:20"));
    }

    @Test
    void testUnsubscribe() {
        connections.addConnection(1, handler1);
        connections.addConnection(2, handler2);
        
        connections.subscribe("sports", 1, 30);
        connections.subscribe("sports", 2, 40);
        
        connections.unsubscribe("sports", 1);
        
        connections.send("sports", "MESSAGE\ndestination:/topic/sports\nbody:goal\n\n");
        
        assertEquals(0, handler1.getReceivedMessages().size());
        assertEquals(1, handler2.getReceivedMessages().size());
    }

    @Test
    void testDisconnectRemovesConnection() {
        connections.addConnection(1, handler1);
        
        connections.disconnect(1);
        
        boolean result = connections.send(1, "Test");
        assertFalse(result);
    }

    @Test
    void testDisconnectRemovesSubscriptions() {
        connections.addConnection(1, handler1);
        connections.addConnection(2, handler2);
        
        connections.subscribe("news", 1, 50);
        connections.subscribe("news", 2, 60);
        
        connections.disconnect(1);
        
        connections.send("news", "MESSAGE\ndestination:/topic/news\nbody:update\n\n");
        
        assertEquals(0, handler1.getReceivedMessages().size());
        assertEquals(1, handler2.getReceivedMessages().size());
    }

    @Test
    void testMultipleChannelSubscriptions() {
        connections.addConnection(1, handler1);
        
        connections.subscribe("channel1", 1, 100);
        connections.subscribe("channel2", 1, 101);
        
        connections.send("channel1", "MESSAGE\ndestination:/topic/channel1\nbody:msg1\n\n");
        connections.send("channel2", "MESSAGE\ndestination:/topic/channel2\nbody:msg2\n\n");
        
        assertEquals(2, handler1.getReceivedMessages().size());
        assertTrue(handler1.getReceivedMessages().get(0).contains("subscription:100"));
        assertTrue(handler1.getReceivedMessages().get(1).contains("subscription:101"));
    }

    @Test
    void testBroadcastToEmptyChannel() {
        // Should not throw exception
        connections.send("emptyChannel", "MESSAGE\ndestination:/topic/empty\nbody:test\n\n");
    }
}
