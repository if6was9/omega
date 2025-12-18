package omega;

public class OmegaException extends RuntimeException {

  public OmegaException(String message, Throwable cause) {
    super(message, cause);
  }

  public OmegaException(String message) {
    super(message);
  }

  public OmegaException(Throwable cause) {
    super(cause);
  }
}
