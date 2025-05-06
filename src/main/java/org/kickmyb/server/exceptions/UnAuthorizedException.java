package org.kickmyb.server.exceptions;

public class UnAuthorizedException extends RuntimeException {

    public UnAuthorizedException(String error){
        super(error);
    }
}
