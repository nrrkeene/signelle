package be.keene.signelle.net;

import be.keene.signelle.SignelleException;
import be.keene.signelle.actions.AbstractAction;
import be.keene.signelle.chat.SignelleModel;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A concurrent processor for HttpRequests.
 */
public class HttpRequestProcessor {
    private static Logger logger = Logger.getLogger("Signelle.Puller");
    
    public static String VERSION = "HTTP/1.1";
    public static String STATUS_200 = "200 OK";
    
    final ExecutorService executorService;
    final Set<Future> unfinishedRequests = new HashSet<>();
    
    
    public HttpRequestProcessor() {
        // Here we want to use a proper thread pool with multiple threads, but to do that I would have to go carefully
        // manage the model so that it could handle situations where, for instance, an add-message action executed
        // before a create-chat action. I didn't do that for this exercise, so really this needs to be one thread here.
        this.executorService = Executors.newSingleThreadExecutor();
//        this.executorService = Executors.newFixedThreadPool(10);
    }
    
    void process(HttpRequest request, SignelleModel model, SocketChannel socketChannel) {
        RequestThread requestThread = new RequestThread(request, model, socketChannel);
        Future future = this.executorService.submit(requestThread);
        this.unfinishedRequests.add(future);
    }
    
    class RequestThread extends Thread {
        private final HttpRequest httpRequest;
        private final SignelleModel model;
        private final SocketChannel socketChannel;
    
        public RequestThread(HttpRequest httpRequest, SignelleModel model, SocketChannel socketChannel) {
            this.httpRequest = httpRequest;
            this.model = model;
            this.socketChannel = socketChannel;
        }
        
        public void run() {
            AbstractAction action = AbstractAction.from(httpRequest);
            if (action == null) {
                logger.warn("Dropping message due to error " + httpRequest.toString());
                return;
            }
    
            try {
                String jsonResponse = action.act(model);
                if (jsonResponse == null) {
                    jsonResponse = "\"Error 34\"";
                }
                String[] headers = new String[] { "Content-Type: application/JSON", "Content-Length: " + jsonResponse.length(), "Connection: Closed"};
                HttpResponse httpResponse = new HttpResponse(VERSION, STATUS_200, headers, jsonResponse);
                ByteBuffer byteBuffer = httpResponse.getByteBuffer();
                while(byteBuffer.hasRemaining()) {
                    socketChannel.write(byteBuffer);
                }

//                System.out.println("-------------- Signelle Processed Request ----------");
//                System.out.println(httpRequest.getRequestText());
//                System.out.println("----------------------------------------------------");
//                System.out.println(httpResponse.getByteBufferText());
            } catch (SignelleException e) {
                logger.error("Cannot process because of Signelle error " + httpRequest, e);
            } catch (IOException e) {
                logger.error("Cannot process because of I/O error" + httpRequest, e);
            } catch(Exception e) {
                logger.error("Cannot process request: " + e.getMessage(), e);
            } finally {
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    logger.error("Cannot close socket", e);
                }
            }
        }
    }
}
