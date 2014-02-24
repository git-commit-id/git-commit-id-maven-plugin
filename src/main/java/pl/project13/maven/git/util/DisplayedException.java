package pl.project13.maven.git.util;

/**
 *
 * @author Krzysztof Suszyński <krzysztof.suszynski@wavesoftware.pl>
 */
public class DisplayedException extends Exception {

  /**
   * Constructor with a cause throwable
   *
   * @param message a message of a exception
   * @param cause a cause of exception
   */
  public DisplayedException(String message, Throwable cause) {
    super(message, cause);
  }

}
