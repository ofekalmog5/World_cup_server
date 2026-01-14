package bgu.spl.net.impl.stomp;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.LoginStatus;
import bgu.spl.net.impl.data.User;
import bgu.spl.net.srv.Connections;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<String>{
    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate = false;
    private final Map<String, Consumer<String>> commandHandlers;
    private static final java.util.concurrent.atomic.AtomicInteger messageIdCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private final Map<Integer, String> activeSubscriptions = new HashMap<>();
    private final Database db = Database.getInstance();
    private User currentUser;
    public StompMessagingProtocolImpl() {
        commandHandlers = new HashMap<>();
        initializeHandlers();
        currentUser = null;
    }
    
    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }
    
    @Override
    public void process(String message) {
        String cleanedMessage = message.replaceAll("^\\s+", ""); 
        if (cleanedMessage.isEmpty()) return;
        String command = cleanedMessage.split("\\n")[0].trim();
    Consumer<String> handler = commandHandlers.get(command);
        if (handler != null) {
            handler.accept(message);
        } else {
            connections.send(connectionId, "ERROR\nmessage: Unknown Command\n\nThe command " + command + " is not implemented.\u0000");
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private void initializeHandlers() {
        commandHandlers.put("CONNECT", this::handleConnect);
        commandHandlers.put("SEND", this::handleSend);
        commandHandlers.put("SUBSCRIBE", this::handleSubscribe);
        commandHandlers.put("UNSUBSCRIBE", this::handleUnsubscribe);
        commandHandlers.put("DISCONNECT", this::handleDisconnect);
    }

    private void handleConnect(String message) {
        if (this.currentUser != null) {
            sendError("Already Logged In", "This connection is already associated with a user", parseHeaders(message));
            return;
        }

        Map<String, String> headers = parseHeaders(message);

        String login = headers.get("login");
        String passcode = headers.get("passcode");
        String acceptVersion = headers.get("accept-version");

        if (login == null || passcode == null || acceptVersion == null) {
            sendError("Malformed Frame", "Missing required headers (login, passcode, accept-version).", headers);
            return;
        }

        LoginStatus status = db.login(connectionId, login, passcode);
        switch (status) {
            case CLIENT_ALREADY_CONNECTED:
                sendError("Client already connected", "Connection already associated with a user.", headers);
                return;
            case ALREADY_LOGGED_IN:
                sendError("User already logged in", "User " + login + " is already logged in.", headers);
                return;
            case WRONG_PASSWORD:
                sendError("Wrong password", "Password does not match.", headers);
                return;
            default:
                break;
        }

        this.currentUser = db.getUserByConnectionId(connectionId);
        if (this.currentUser == null) {
            sendError("Login failed", "User state not found after login", headers);
            return;
        }
        String response = "CONNECTED\n" +
                          "version:1.2\n" +
                          "\n" + 
                          "\u0000"; 
        
        connections.send(connectionId, response);
        sendReceiptIfRequested(headers);
    }

    private Map<String, String> parseHeaders(String message) {
        Map<String, String> headers = new HashMap<>();
        String[] lines = message.split("\n");
        
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) break; 

            int splitIndex = line.indexOf(':');
            if (splitIndex != -1) {
                String key = line.substring(0, splitIndex).trim();
                String value = line.substring(splitIndex + 1).trim();
                headers.put(key, value);
            }
        }
        return headers;
    }

    private void sendError(String messageHeader, String errorBody,Map<String, String> headers) {
    String receiptId;
    if (headers != null) {
        receiptId = headers.get("receipt");
    } else {
        receiptId = null;
    }
    String errorFrame = "ERROR\n" +
                        "message:" + messageHeader + "\n";

    if (receiptId != null) {
        errorFrame += "receipt-id:" + receiptId + "\n";
    }
    errorFrame += "\n" + 
                  errorBody + "\n" +
                  "\u0000";

    connections.send(connectionId, errorFrame);
    connections.disconnect(connectionId); 
    shouldTerminate = true;
    }

        private void handleSend(String message) {
        Map<String, String> headers = parseHeaders(message);
    
      if (this.currentUser == null) {
        sendError("Not Logged In", "You must send a CONNECT frame first", headers);
        return;
        }
    String destination = headers.get("destination");

    if (destination == null) {
        sendError("Malformed Frame", "Missing destination header", headers);
        return;
    }
    if (!activeSubscriptions.containsValue(destination)) {
        sendError("Not Subscribed", "You cannot send messages to a channel you are not subscribed to", headers);
        return;
    }
    int msgId = messageIdCounter.getAndIncrement();

    int bodyStartIndex = message.indexOf("\n\n");
    int nullCharIndex = message.indexOf('\u0000');
    if (nullCharIndex == -1) {
        nullCharIndex = message.length();
    }

    String body;
    if (bodyStartIndex != -1) {
        body = message.substring(bodyStartIndex + 2, nullCharIndex);
    } else {
        body = "";
    }

    String messageFrame = "MESSAGE\n" +
                          "message-id:" + msgId + "\n" +
                          "destination:" + destination + "\n" +
                          "\n" +
                          body + "\n" +
                          "\u0000";

    connections.send(destination, messageFrame);
    sendReceiptIfRequested(headers);
    }
    private void handleSubscribe(String message) {
        Map<String, String> headers = parseHeaders(message);
        if (this.currentUser == null) {
        sendError("Not Logged In", "You must send a CONNECT frame first", headers);
        return;
        }
        String destination = headers.get("destination");
        String idStr = headers.get("id");
        
        if (destination == null || idStr == null) {
            sendError("Malformed Frame", "Missing required headers (destination, id).", headers);
            return;
        }
        
        int subscriptionId;
        try {
            subscriptionId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sendError("Malformed Frame", "id header must be a number.", headers);
            return;
        }

        connections.subscribe(destination, connectionId, subscriptionId);
        
        activeSubscriptions.put(subscriptionId, destination);
        sendReceiptIfRequested(headers);

    }
    private void handleUnsubscribe(String message) {
        Map<String, String> headers = parseHeaders(message);
         if (this.currentUser == null) {
        sendError("Not Logged In", "You must send a CONNECT frame first", headers);
        return;
        }
        String idStr = headers.get("id");

        if (idStr == null) {
            sendError("Malformed Frame", "Missing required header: id", headers);
            return;
        }

        int subscriptionId;
        try {
            subscriptionId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sendError("Malformed Frame", "id header must be a number"   , headers);
            return;
        }

        String channel = activeSubscriptions.get(subscriptionId);

        if (channel != null) {
            connections.unsubscribe(channel, connectionId);

            activeSubscriptions.remove(subscriptionId);
        } else {
            sendError("Not Subscribed", "No active subscription with id: " + subscriptionId, headers);
        }
        sendReceiptIfRequested(headers);
    }
    private void handleDisconnect(String message) {
        Map<String, String> headers = parseHeaders(message);
        if (this.currentUser == null) {
            sendError("Not Logged In", "You must send a CONNECT frame first", headers);
            return;
        }
        unsubscribeAll();
        sendReceiptIfRequested(headers);
        db.logout(connectionId);
        connections.disconnect(connectionId);
        shouldTerminate = true;
        currentUser = null;
    }

    private void unsubscribeAll() {
        for (Map.Entry<Integer, String> entry : activeSubscriptions.entrySet()) {
            connections.unsubscribe(entry.getValue(), connectionId);
        }
        activeSubscriptions.clear();
    }
    private void sendReceiptIfRequested(Map<String, String> headers) {
    String receiptId = headers.get("receipt");
    if (receiptId != null) {
        String receiptFrame = "RECEIPT\n" +
                              "receipt-id:" + receiptId + "\n" +
                              "\n" + "\u0000";
        connections.send(connectionId, receiptFrame);
    }
}
}
