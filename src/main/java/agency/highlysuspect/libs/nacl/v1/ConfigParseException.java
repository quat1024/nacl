package agency.highlysuspect.libs.nacl.v1;

public class ConfigParseException extends RuntimeException {
	public ConfigParseException(String message) {
		super(message);
	}
	
	public ConfigParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
