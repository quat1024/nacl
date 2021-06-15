package agency.highlysuspect.libs.nacl.v1;

public interface ConfigExt {
	/**
	 * Perform extra validation on the values in this config file.
	 * @throws ConfigParseException if the config file is not well-formed.
	 */
	default void validate() throws ConfigParseException {
		
	}
	
	/**
	 * Try to upgrade this config file to the latest version.
	 * You may want to specify a "config_version" field somewhere in your file.
	 */
	default void upgrade() {
		
	}
	
	/**
	 * This is called when the config file is all done parsing.
	 * If you need to create any Java-derived values from values in the config file,
	 * this is a good place to do it.
	 */
	default void finish() {
		
	}
}
