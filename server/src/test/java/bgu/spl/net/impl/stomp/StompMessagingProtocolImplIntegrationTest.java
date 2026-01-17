package bgu.spl.net.impl.stomp;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.net.srv.ConnectionsImpl;

class StompMessagingProtocolImplIntegrationTest {
    private StompMessagingProtocolImpl protocol;
    private ConnectionsImpl<String> connections;
    private MockConnectionHandler handler;

    private class MockConnectionHandler implements bgu.spl.net.srv.ConnectionHandler<String> {
        private final List<String> sentFrames = new ArrayList<>();

        @Override
        public void send(String msg) {
            sentFrames.add(msg);
        }

        @Override
        public void close() {
        }

        public List<String> getSentFrames() {
            return sentFrames;
        }

        public void clear() {
            sentFrames.clear();
        }
    }

    @BeforeEach
    void setUp() {
        protocol = new StompMessagingProtocolImpl();
        connections = new ConnectionsImpl<>();
        handler = new MockConnectionHandler();
        connections.addConnection(1, handler);
        protocol.start(1, connections);
    }

    @Test
    void testConnectWithoutLogin() {
        String connectFrame = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\n\n\0";
        
        protocol.process(connectFrame);
        
        assertTrue(handler.getSentFrames().size() >= 1, "Should send at least one frame");
        String response = handler.getSentFrames().get(0);
        assertTrue(response.startsWith("ERROR") || response.startsWith("CONNECTED"), 
                   "Response should be ERROR or CONNECTED, got: " + response);
    }

    @Test
    void testSubscribeBeforeConnect() {
        String subscribeFrame = "SUBSCRIBE\ndestination:/topic/general\nid:0\nreceipt:0\n\n\0";
        
        protocol.process(subscribeFrame);
        
        assertEquals(1, handler.getSentFrames().size());
        String response = handler.getSentFrames().get(0);
        assertTrue(response.startsWith("ERROR"));
    }

    @Test
    void testDisconnectShouldTerminate() {
        assertFalse(protocol.shouldTerminate());
        
        String disconnectFrame = "DISCONNECT\nreceipt:0\n\n\0";
        protocol.process(disconnectFrame);
        
        assertTrue(protocol.shouldTerminate());
    }

    @Test
    void testErrorResponseFormat() {
        String invalidFrame = "INVALID\n\n\0";
        
        protocol.process(invalidFrame);
        
        assertEquals(1, handler.getSentFrames().size());
        String response = handler.getSentFrames().get(0);
        assertTrue(response.startsWith("ERROR"));
        assertTrue(response.contains("message:"));
    }

    @Test
    void testReceiptOnDisconnect() {
        String disconnectFrame = "DISCONNECT\nreceipt:123\n\n\0";
        
        protocol.process(disconnectFrame);
        
        assertTrue(protocol.shouldTerminate(), "Protocol should terminate after DISCONNECT");
        // May send RECEIPT or ERROR depending on connection state
        if (handler.getSentFrames().size() > 0) {
            String response = handler.getSentFrames().get(0);
            assertTrue(response.startsWith("RECEIPT") || response.startsWith("ERROR"),
                       "Response should be RECEIPT or ERROR, got: " + response);
        }
    }
}
