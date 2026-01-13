package bgu.spl.net.impl.stomp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.ConnectionsImpl;

/**
 * Thread safety tests for STOMP protocol implementation
 */
class StompMultithreadingTest {
    
    static class FakeHandler implements ConnectionHandler<String> {
        final List<String> sent = new CopyOnWriteArrayList<>();
        @Override
        public void send(String msg) { sent.add(msg); }
        @Override
        public void close() throws IOException { }
    }

    private ConnectionsImpl<String> connections;
    private ExecutorService executor;

    @BeforeEach
    void setup() {
        connections = new ConnectionsImpl<>();
        executor = Executors.newFixedThreadPool(10);
    }

    @Test
    void testConcurrentLoginSameUser() throws InterruptedException {
        // Test that only one connection can login with the same username
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        
        List<StompMessagingProtocolImpl> protocols = new ArrayList<>();
        List<FakeHandler> handlers = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl();
            FakeHandler handler = new FakeHandler();
            connections.addConnection(i, handler);
            protocol.start(i, connections);
            protocols.add(protocol);
            handlers.add(handler);
        }

        // All threads try to login as the same user simultaneously
        for (int i = 0; i < numThreads; i++) {
            final int connId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal
                    String connect = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:testuser\npasscode:pass123\n\n\u0000";
                    protocols.get(connId).process(connect);
                    
                    // Check if this connection got CONNECTED or ERROR
                    List<String> messages = handlers.get(connId).sent;
                    for (String msg : messages) {
                        if (msg.startsWith("CONNECTED")) successCount.incrementAndGet();
                        else if (msg.startsWith("ERROR")) errorCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Test timed out");

        // Exactly one should succeed, rest should get errors
        assertEquals(1, successCount.get(), "Exactly one login should succeed");
        assertEquals(numThreads - 1, errorCount.get(), "All others should get errors");
    }

    @Test
    void testConcurrentSubscriptions() throws InterruptedException {
        // Test multiple clients subscribing to the same channel simultaneously
        int numClients = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numClients);

        List<StompMessagingProtocolImpl> protocols = new ArrayList<>();
        List<FakeHandler> handlers = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl();
            FakeHandler handler = new FakeHandler();
            connections.addConnection(i, handler);
            protocol.start(i, connections);
            protocols.add(protocol);
            handlers.add(handler);

            // Login first
            String connect = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:user" + i + "\npasscode:pass\n\n\u0000";
            protocol.process(connect);
        }

        // All subscribe to same channel
        for (int i = 0; i < numClients; i++) {
            final int connId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String subscribe = "SUBSCRIBE\ndestination:/testchannel\nid:" + connId + "\nreceipt:" + connId + "\n\n\u0000";
                    protocols.get(connId).process(subscribe);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        // Send a message to the channel - all subscribed clients should receive it
        protocols.get(0).process("SEND\ndestination:/testchannel\n\nTest message\u0000");

        // Give some time for message delivery
        Thread.sleep(100);

        // All clients should have received the MESSAGE frame
        int receivedCount = 0;
        for (FakeHandler handler : handlers) {
            for (String msg : handler.sent) {
                if (msg.startsWith("MESSAGE")) {
                    receivedCount++;
                    break;
                }
            }
        }
        assertEquals(numClients, receivedCount, "All subscribed clients should receive the message");
    }

    @Test
    void testConcurrentSendToSameChannel() throws InterruptedException {
        // Test multiple clients sending messages to the same channel simultaneously
        int numSenders = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numSenders);

        List<StompMessagingProtocolImpl> protocols = new ArrayList<>();
        List<FakeHandler> handlers = new ArrayList<>();

        // Setup all clients with login and subscription
        for (int i = 0; i < numSenders; i++) {
            StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl();
            FakeHandler handler = new FakeHandler();
            connections.addConnection(i, handler);
            protocol.start(i, connections);
            protocols.add(protocol);
            handlers.add(handler);

            String connect = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:sender" + i + "\npasscode:pass\n\n\u0000";
            protocol.process(connect);
            
            String subscribe = "SUBSCRIBE\ndestination:/racechannel\nid:" + i + "\n\n\u0000";
            protocol.process(subscribe);
        }

        // All send messages simultaneously
        for (int i = 0; i < numSenders; i++) {
            final int connId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String send = "SEND\ndestination:/racechannel\n\nMessage from sender " + connId + "\u0000";
                    protocols.get(connId).process(send);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        Thread.sleep(200); // Wait for message delivery

        // Each client should have received numSenders messages
        for (FakeHandler handler : handlers) {
            int messageCount = 0;
            for (String msg : handler.sent) {
                if (msg.startsWith("MESSAGE")) {
                    messageCount++;
                }
            }
            assertEquals(numSenders, messageCount, "Each client should receive all messages");
        }
    }

    @Test
    void testConcurrentUnsubscribe() throws InterruptedException {
        // Test concurrent unsubscribe operations
        int numClients = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numClients);

        List<StompMessagingProtocolImpl> protocols = new ArrayList<>();
        List<FakeHandler> handlers = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl();
            FakeHandler handler = new FakeHandler();
            connections.addConnection(i, handler);
            protocol.start(i, connections);
            protocols.add(protocol);
            handlers.add(handler);

            String connect = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:unsub" + i + "\npasscode:pass\n\n\u0000";
            protocol.process(connect);
            
            String subscribe = "SUBSCRIBE\ndestination:/tempchannel\nid:" + i + "\n\n\u0000";
            protocol.process(subscribe);
        }

        // All unsubscribe simultaneously
        for (int i = 0; i < numClients; i++) {
            final int connId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String unsub = "UNSUBSCRIBE\nid:" + connId + "\nreceipt:" + (100 + connId) + "\n\n\u0000";
                    protocols.get(connId).process(unsub);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        Thread.sleep(100);

        // Send a message - no one should receive it
        protocols.get(0).process("SEND\ndestination:/tempchannel\n\nTest after unsub\u0000");
        Thread.sleep(100);

        // Count MESSAGE frames after unsubscribe
        int messageCount = 0;
        for (FakeHandler handler : handlers) {
            for (String msg : handler.sent) {
                if (msg.startsWith("MESSAGE") && msg.contains("Test after unsub")) {
                    messageCount++;
                }
            }
        }
        assertEquals(0, messageCount, "No messages should be delivered after unsubscribe");
    }

    @Test
    void testConcurrentDisconnect() throws InterruptedException {
        // Test that concurrent disconnects don't cause issues
        int numClients = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numClients);

        List<StompMessagingProtocolImpl> protocols = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl();
            FakeHandler handler = new FakeHandler();
            connections.addConnection(i, handler);
            protocol.start(i, connections);
            protocols.add(protocol);

            String connect = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:disc" + i + "\npasscode:pass\n\n\u0000";
            protocol.process(connect);
        }

        // All disconnect simultaneously
        for (int i = 0; i < numClients; i++) {
            final int connId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String disconnect = "DISCONNECT\nreceipt:" + connId + "\n\n\u0000";
                    protocols.get(connId).process(disconnect);
                    assertTrue(protocols.get(connId).shouldTerminate());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        // All protocols should be terminated
        for (StompMessagingProtocolImpl protocol : protocols) {
            assertTrue(protocol.shouldTerminate());
        }
    }
}
