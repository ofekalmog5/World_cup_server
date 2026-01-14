package bgu.spl.net.srv;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<StompMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private AtomicInteger idCounter = new AtomicInteger(0);
    private ConnectionsImpl<T> connections = new ConnectionsImpl<>();
    
    public BaseServer(
            int port,
            Supplier<StompMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
    }


@Override
public void serve() {
   try (ServerSocket serverSock = new ServerSocket(port)) {
    this.sock = serverSock; 
    System.out.println("Server started on port " + port);
        while (!Thread.currentThread().isInterrupted()) {
            Socket clientSock = serverSock.accept();

            StompMessagingProtocol<T> protocol = protocolFactory.get();
            int connectionId = idCounter.getAndIncrement();
            protocol.start(connectionId, connections);

            BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                    clientSock,
                    encdecFactory.get(),
                    protocol,
                    connections,
                    connectionId);

            connections.addConnection(connectionId, handler);
            
            execute(handler);
        }
    } catch (IOException ex) {
        ex.printStackTrace();
    }
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
