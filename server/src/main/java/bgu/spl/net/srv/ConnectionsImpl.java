package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.net.impl.data.Database;

public class ConnectionsImpl<T> implements Connections<T> {
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> activeConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Subscriber>> channelSubscribers = new ConcurrentHashMap<>();
    private static class Subscriber {
    private final int connectionId; 
    private final int subscriptionId; 

        public Subscriber(int connectionId, int subscriptionId) {
            this.connectionId = connectionId;
            this.subscriptionId = subscriptionId;
        }
    }
    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeConnections.get(connectionId);
        
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        
        return false;
    }

    @Override
    public void send(String channel, T msg) {
        ConcurrentLinkedQueue<Subscriber> subscribers = channelSubscribers.get(channel);
    if (subscribers != null) {
        for (Subscriber sub : subscribers) {
            String frame = (String) msg;
            int firstNewLine = frame.indexOf("\n");
            String command = frame.substring(0, firstNewLine);
            String rest = frame.substring(firstNewLine);
            String modifiedFrame = command + "\nsubscription:" + sub.subscriptionId + rest;
            send(sub.connectionId, (T) modifiedFrame);
        }
    }
    }

    @Override
    public void disconnect(int connectionId) {
        activeConnections.remove(connectionId);

        for (ConcurrentLinkedQueue<Subscriber> subscribers : channelSubscribers.values()) {
            subscribers.removeIf(sub -> sub.connectionId == connectionId);
        }
        Database.getInstance().logout(connectionId);
    }

    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        activeConnections.put(connectionId, handler);
    }
    @Override
    public void subscribe(String channel, int connectionId, int subscriptionId) {
        channelSubscribers.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>())
                          .add(new Subscriber(connectionId, subscriptionId));
    }
    @Override
    public void unsubscribe(String channel, int connectionId) {
        ConcurrentLinkedQueue<Subscriber> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.removeIf(sub -> sub.connectionId == connectionId);
        }
    }

}
