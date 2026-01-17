package bgu.spl.net.impl.stomp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.ConnectionsImpl;

class StompProtocolTest {
    static class FakeHandler implements ConnectionHandler<String> {
        final List<String> sent = new ArrayList<>();
        @Override
        public void send(String msg) { sent.add(msg); }
        @Override
        public void close() throws IOException { }
    }

    private ConnectionsImpl<String> connections;
    private StompMessagingProtocolImpl protocol1;
    private StompMessagingProtocolImpl protocol2;
    private FakeHandler handler1;
    private FakeHandler handler2;

    @BeforeEach
    void setup() {
        connections = new ConnectionsImpl<>();
        protocol1 = new StompMessagingProtocolImpl();
        protocol2 = new StompMessagingProtocolImpl();
        handler1 = new FakeHandler();
        handler2 = new FakeHandler();
        connections.addConnection(1, handler1);
        connections.addConnection(2, handler2);
        protocol1.start(1, connections);
        protocol2.start(2, connections);
    }

    @Test
    void testConnectSuccessAndReceipt() {
        String connect = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:meni\npasscode:films\nreceipt:10\n\n\u0000";
        protocol1.process(connect);
        assertTrue(handler1.sent.get(0).startsWith("CONNECTED\nversion:1.2"));
        assertEquals("RECEIPT\nreceipt-id:10\n\n\u0000", handler1.sent.get(1));
        assertFalse(protocol1.shouldTerminate());
    }

    @Test
    void testDuplicateLoginError() {
        // First connection logs in
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:meni\npasscode:films\n\n\u0000");
        // Second tries same user
        protocol2.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:meni\npasscode:films\n\n\u0000");
        String err = handler2.sent.get(0);
        assertTrue(err.startsWith("ERROR\nmessage:User already logged in"));
        assertTrue(protocol2.shouldTerminate());
    }

    @Test
    void testWrongPasswordError() {
        // First connection creates user with passcode films
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:meni\npasscode:films\n\n\u0000");
        // Second tries same user with wrong passcode
        protocol2.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:meni\npasscode:wrong\n\n\u0000");
        String err = handler2.sent.get(0);
        assertTrue(err.startsWith("ERROR\nmessage:Wrong password"));
        assertTrue(protocol2.shouldTerminate());
    }

    @Test
    void testConnectMissingHeadersError() {
        // Missing accept-version
        protocol1.process("CONNECT\nhost:stomp.cs.bgu.ac.il\nlogin:u\npasscode:p\n\n\u0000");
        String err = handler1.sent.get(0);
        assertTrue(err.startsWith("ERROR\nmessage:Malformed Frame"));
        assertTrue(protocol1.shouldTerminate());
    }

    @Test
    void testSubscribeAndSendMessageDelivery() {
        // Login both
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:alice\npasscode:pw\n\n\u0000");
        protocol2.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:bob\npasscode:pw\n\n\u0000");
        // Subscribe handler2 to /usa_mexico with id 17
        protocol2.process("SUBSCRIBE\ndestination:/usa_mexico\nid:17\n\n\u0000");
        // Sender must be subscribed as per assignment; subscribe protocol1 too
        protocol1.process("SUBSCRIBE\ndestination:/usa_mexico\nid:5\n\n\u0000");
        // Send message from protocol1
        String body = "user: alice\nteam a: usa\nteam b: mexico\nevent name: kickoff\n" +
                      "time: 0\n\n" +
                      "description:\nAnd we’re off!";
        protocol1.process("SEND\ndestination:/usa_mexico\n\n" + body + "\n\u0000");
        // Expect handler2 to receive MESSAGE with subscription:17 and destination:/usa_mexico
        String delivered = handler2.sent.stream().filter(s -> s.startsWith("MESSAGE\n")).findFirst().orElse("");
        assertTrue(delivered.contains("destination:/usa_mexico\n"));
        assertTrue(delivered.contains("subscription:17\n"));
        assertTrue(delivered.contains("\n" + body + "\n\u0000"));
    }

    @Test
    void testSubscribeReceiptReturned() {
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:alice\npasscode:pw\n\n\u0000");
        protocol1.process("SUBSCRIBE\ndestination:/topic\nid:1\nreceipt:55\n\n\u0000");
        // Last message to handler1 should be RECEIPT with id 55
        String last = handler1.sent.get(handler1.sent.size()-1);
        assertEquals("RECEIPT\nreceipt-id:55\n\n\u0000", last);
    }

    @Test
    void testUnsubscribeStopsDelivery() {
        protocol2.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:bob\npasscode:pw\n\n\u0000");
        protocol2.process("SUBSCRIBE\ndestination:/game\nid:17\n\n\u0000");
        protocol2.process("UNSUBSCRIBE\nid:17\n\n\u0000");
        // Sender subscribes and sends
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:alice\npasscode:pw\n\n\u0000");
        protocol1.process("SUBSCRIBE\ndestination:/game\nid:5\n\n\u0000");
        protocol1.process("SEND\ndestination:/game\n\nhi\n\u0000");
        // No MESSAGE should be delivered to handler2
        assertTrue(handler2.sent.stream().noneMatch(s -> s.startsWith("MESSAGE\n")));
    }

    @Test
    void testUnsubscribeNonExistingIdError() {
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:alice\npasscode:pw\n\n\u0000");
        // Unsubscribe without prior subscribe
        protocol1.process("UNSUBSCRIBE\nid:999\n\n\u0000");
        String err = handler1.sent.get(0);
        assertTrue(err.startsWith("ERROR\nmessage:Not Subscribed"));
        assertTrue(protocol1.shouldTerminate());
    }

    @Test
    void testSendWithoutSenderSubscriptionError() {
        // Receiver subscribes but sender does not
        protocol2.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:bob\npasscode:pw\n\n\u0000");
        protocol2.process("SUBSCRIBE\ndestination:/game\nid:17\n\n\u0000");
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:alice\npasscode:pw\n\n\u0000");
        protocol1.process("SEND\ndestination:/game\nreceipt:66\n\nhello\n\u0000");
        // Sender should receive ERROR and terminate; no MESSAGE to receiver
        String err = handler1.sent.get(1); // CONNECTED + then ERROR
        assertTrue(err.startsWith("ERROR\nmessage:Not Subscribed"));
        assertTrue(protocol1.shouldTerminate());
        assertTrue(handler2.sent.stream().noneMatch(s -> s.startsWith("MESSAGE\n")));
    }

    @Test
    void testDisconnectReceiptAndTerminate() {
        protocol1.process("CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:alice\npasscode:pw\n\n\u0000");
        protocol1.process("DISCONNECT\nreceipt:77\n\n\u0000");
        // Expect RECEIPT then termination
        assertEquals("RECEIPT\nreceipt-id:77\n\n\u0000", handler1.sent.get(handler1.sent.size()-1));
        assertTrue(protocol1.shouldTerminate());
    }
}
