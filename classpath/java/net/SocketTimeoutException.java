package java.net;

import java.io.IOException;

public class SocketTimeoutException extends IOException {
    public SocketTimeoutException(String message) {
        super(message);
    }

    public SocketTimeoutException() {}
}
