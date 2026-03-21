package api.myitmo.utils;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final Integer errorCode;
    private final String errorMessage;

    public ApiException(Integer errorCode, String errorMessage) {
        super("API error " + errorCode + ": " + errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.errorMessage = message;
    }
}
