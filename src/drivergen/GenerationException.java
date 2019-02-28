package drivergen;

/**
 * @author Rody Kersten
 *
 */
public class GenerationException extends Exception {

	private static final long serialVersionUID = 3478046871091712081L;
	String message;
	
	public GenerationException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
