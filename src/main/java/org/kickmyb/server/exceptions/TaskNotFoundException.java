package org.kickmyb.server.exceptions;

public class TaskNotFoundException extends RuntimeException {

    public  TaskNotFoundException(String error){
        super(error);
    }
}
