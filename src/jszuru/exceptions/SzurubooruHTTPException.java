package jszuru.exceptions;

public class SzurubooruHTTPException extends SzurubooruException {
    String errorName;

    public SzurubooruHTTPException(String errorName, String errorDescription) {
        super(errorDescription);
        this.errorName = errorName;
    }
    public SzurubooruHTTPException(String msg){
        super(msg);
    }

    public String getErrorName(){
        return errorName;
    }
}
