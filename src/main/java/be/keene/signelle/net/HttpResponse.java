package be.keene.signelle.net;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class HttpResponse {
    private final String version;
    private final String status;
    private final String[] headers;
    private final String payload;
    
    public HttpResponse(String version, String status, String[] headers, String payload) {
        this.version = version;
        this.status = status;
        this.headers = headers;
        this.payload = payload;
    }
    
    public ByteBuffer getByteBuffer() {
        String responseText = getByteBufferText();
        ByteBuffer result = ByteBuffer.wrap(responseText.getBytes(Charset.defaultCharset()));
        
        return result;
    }
    
    public String getByteBufferText() {
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append(version).append(" ").append(status).append("\n");
        for(String header: headers) responseBuilder.append(header.trim()).append(("\n"));
        responseBuilder.append("\n");
        responseBuilder.append(payload);
        String result = responseBuilder.toString();
        return result;
    }
}
