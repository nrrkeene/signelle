package be.keene.signelle;

import be.keene.signelle.chat.SignelleModel;
import be.keene.signelle.net.HttpPuller;
import org.apache.log4j.Logger;

/**
 * Signelle, a chat app server
 * by Nicholas Keene 2019
 */
public class Signelle {
    private static Logger logger = Logger.getLogger("Signelle");
    
    private static String host = "127.0.0.1";
    private static int port = 1080;
    
    public static void main(String[] args) throws Exception {
        if(logger.isInfoEnabled()) logger.info("Welcome to Signelle");
    
        SignelleModel model = new SignelleModel();
        HttpPuller httpPuller = new HttpPuller(host, port, model);
        
        new Thread(httpPuller).start();
    
        if(logger.isInfoEnabled()) logger.info("Exiting Signelle normally");
    }
}