package be.keene.signelle.net;

import org.apache.log4j.Logger;

public class HttpRequest {
    private static Logger logger = Logger.getLogger("Signelle.Puller");
    
    public TYPE getType() {
        return type;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String[] getHeaders() {
        return headers;
    }
    
    public String getPayload() {
        return payload;
    }
    
    private final TYPE type;
    private final String resource;
    private final String version;
    private final String[] headers;
    private final String payload;
    
    private final String requestText;
    
    public enum TYPE {
        POST,
        GET
    }
    
    public HttpRequest(TYPE type, String resource, String version, String[] headers, String payload, String requestText) {
        this.type = type;
        this.resource = resource;
        this.version = version;
        this.headers = headers;
        this.payload = payload;
        this.requestText = requestText;
    }
    
    /**
     * Returns a valid HttpRequest or null if there is an error. Logs exceptions, never throws them.
     *
     * @param requestText
     * @return
     */
    static HttpRequest from(String requestText) {
        try {
            if (requestText == null || requestText.isEmpty()) {
                if (logger.isTraceEnabled()) logger.trace("Cannot create message from empty request");
                return null;
            }
    
            String[] lines = requestText.split("\n");
            String[] firstSegments = lines[0].split("\\s");
            if(firstSegments.length != 3) {
                if (logger.isTraceEnabled()) logger.trace("Can't handle malformed header line: " + lines[0]);
                return null;
            }
            
            TYPE type;
            if(firstSegments[0].equalsIgnoreCase("GET")) {
                type = TYPE.GET;
            } else if(firstSegments[0].equalsIgnoreCase("POST")) {
                type = TYPE.POST;
            } else {
                if (logger.isTraceEnabled()) logger.trace("Can only handle GET and POST, not " + firstSegments[0]);
                return null;
            }
            String resource = firstSegments[1];
            String version = firstSegments[2];
            String[] headers = new String[lines.length - 3];
            System.arraycopy(lines, 1, headers, 0, lines.length - 3);
            String payload = lines[lines.length - 1];
            
            HttpRequest result = new HttpRequest(type, resource, version, headers, payload, requestText);
            return result;
        } catch (Exception e) {
            logger.error(e,e );
            return null;
        }
    }
    
    public String getRequestText() {
        return requestText;
    }
}
