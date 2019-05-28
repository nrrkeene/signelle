package be.keene.signelle;

public class SignelleException extends Exception {
    public SignelleException(Exception e) {
        super(e);
    }
    
    public SignelleException(String s) {
        super(s);
    }
}
