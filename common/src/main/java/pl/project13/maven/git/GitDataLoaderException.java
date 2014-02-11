package pl.project13.maven.git;

/**
 * To wrap exceptions when informations from the repository are loaded.
 * 
 * @author jbellmann
 *
 */
public class GitDataLoaderException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public GitDataLoaderException(String message){
		super(message);
	}
	
	public GitDataLoaderException(String message, Throwable cause){
		super(message, cause);
	}

	public GitDataLoaderException(Throwable cause){
		super(cause);
	}
}
