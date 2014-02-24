package pl.project13.maven.git.util;

/**
 *
 * @author Krzysztof Suszyński <krzysztof.suszynski@wavesoftware.pl>
 */
public class EidRuntimeException extends RuntimeException {

  private final String eid;

  private final String originalMessage;

  /**
   * Constructor with message
   *
   * @param eid exception ID
   * @param message a message of a exception
   */
  public EidRuntimeException(String eid, String message) {
    super(String.format("bug:%s:%s", eid, message));
    this.eid = eid;
    originalMessage = message;
  }

  /**
   * Constructor with message and cause throwable
   *
   * @param eid exception ID
   * @param message a message of a exception
   * @param cause a cause of exception
   */
  public EidRuntimeException(String eid, String message, Throwable cause) {
    super(String.format("bug:%s:%s => %s", eid, message, cause.getLocalizedMessage()), cause);
    this.eid = eid;
    originalMessage = message;
  }

  /**
   * Constructor with a cause throwable
   *
   * @param eid exception ID
   * @param cause a cause of exception
   */
  public EidRuntimeException(String eid, Throwable cause) {
    super(String.format("bug:%s => %s", eid, cause.getLocalizedMessage()));
    this.eid = eid;
    originalMessage = null;
  }

  /**
   * Gets a EID (Exception ID)
   *
   * @return a exception ID
   */
  public String getEid() {
    return eid;
  }

  /**
   * Gets a original message of a exception
   *
   * @return a original message
   */
  public String getOriginalMessage() {
    return originalMessage;
  }

}
