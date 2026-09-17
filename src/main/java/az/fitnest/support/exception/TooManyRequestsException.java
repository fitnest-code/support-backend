package az.fitnest.support.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends BaseException {

    private static final long serialVersionUID = 1L;

    public TooManyRequestsException(String message) {
        super(message, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS);
    }
}
