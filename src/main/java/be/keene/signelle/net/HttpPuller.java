package be.keene.signelle.net;


import be.keene.signelle.SignelleException;
import be.keene.signelle.chat.SignelleModel;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * The puller grabs messages off of the wire.
 */
public class HttpPuller implements Runnable {
    private static Logger logger = Logger.getLogger("Signelle.Puller");
    private final int BYTE_BUFFER_SIZE = 4096;
    
    private final String host;
    private final int port;
    private final SignelleModel model;
    private SignellePullerProcessor processor;
    private final HttpRequestProcessor requestProcessor;
    
    public HttpPuller(String host, int port, SignelleModel model) {
        this.host = host;
        this.port = port;
        this.model = model;
        
        this.requestProcessor = new HttpRequestProcessor();
    }
    
    @Override
    public void run() {
        try {
            this.processor = new SignellePullerProcessor();
            new Thread(this.processor).start();
        } catch (SignelleException e) {
            logger.error("HTTP Request Processor failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * The actual processor thread.
     */
    private class SignellePullerProcessor implements Runnable {
        private final Selector selector;
        private final ServerSocketChannel serverSocketChannel;
        private boolean shouldStop = false;
        
        SignellePullerProcessor() throws SignelleException {
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(host, port));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, serverSocketChannel.validOps(), null);
            } catch (IOException e) {
                logger.error("Cannot create processor because bad IO: " + e.getMessage(), e);
                throw new SignelleException(e);
            } catch (Exception e) {
                logger.error("Cannot create processor because of exception: " + e.getMessage(), e);
                throw new SignelleException(e);
            }
        }
        
        public void run() {
            if(logger.isTraceEnabled()) logger.trace("Running Processor");
            
            try {
                while (!this.shouldStop) {
                    readHttpRequests();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        logger.error("Processor interrupted while waiting for more messages; stopping processor");
                        this.shouldStop = true;
                    }
                }
            } catch(Throwable t) {
                logger.error("Processor fatal exception", t);
                this.shouldStop = true;
            }
            
            if(logger.isInfoEnabled()) logger.info("Exiting Signelle");
        }
    
        /**
         * Pulls bytes off the wire and emits the requests to listeners.
         *
         * @throws SignelleException
         */
        private void readHttpRequests() throws SignelleException {
            try {
                this.selector.select();
                Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectedKey = keyIterator.next();
                    
                    // accept connections
                    if (selectedKey.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        if(logger.isTraceEnabled()) logger.trace("Processor accepted connection to: " + socketChannel.getLocalAddress());
                    }
                    
                    // read accepted connections
                    else if (selectedKey.isReadable()) {
                        
                        // pull all the bytes, create a request object, and pass the request to a processor
                        SocketChannel socketChannel = (SocketChannel) selectedKey.channel();
                        
                        ByteBuffer byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
                        StringBuilder entireRequest = new StringBuilder();
                        int bytesRead;
                        do {
                            bytesRead = socketChannel.read(byteBuffer);
                            if(bytesRead > 0) {
                                entireRequest.append(new String(byteBuffer.array(), 0, bytesRead));
                            }
                        } while(bytesRead > 0);
                        socketChannel.shutdownInput();
                        
                        String rawRequest = entireRequest.toString();
                        HttpRequest httpRequest = HttpRequest.from(rawRequest);
                        if(httpRequest == null) {
                            if(logger.isTraceEnabled()) logger.trace("No httpRequest from bytes: " + rawRequest);
                            continue;
                        }
                        requestProcessor.process(httpRequest, model, socketChannel);
                    }
                    
                    else {
                        if(logger.isTraceEnabled()) logger.trace("Weird key");
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                logger.error("Processor could not handle messages: " + e.getMessage());
                throw new SignelleException(e);
            }
        }
    }
}
