package ma.gestionlocative.locataires.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CinDejaUtiliseException extends RuntimeException {
    public CinDejaUtiliseException(String message) {
        super(message);
    }
}
