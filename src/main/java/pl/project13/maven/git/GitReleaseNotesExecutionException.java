package pl.project13.maven.git;

/**
 * Created by pankaj on 10/21/16.
 */
public class GitReleaseNotesExecutionException extends Exception {
    public GitReleaseNotesExecutionException() {
        super();
    }

    public GitReleaseNotesExecutionException(String message) {
        super(message);
    }

    public GitReleaseNotesExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitReleaseNotesExecutionException(Throwable cause) {
        super(cause);
    }

    public GitReleaseNotesExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
