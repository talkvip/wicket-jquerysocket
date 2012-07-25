package nl.topicuszorg.wicket.jquerysocket.js;

import org.apache.wicket.resource.JQueryPluginResourceReference;

/**
 * Resource reference to the jquery stream javascript files
 * 
 * @author Dries Schulten
 */
public class SocketResourceReference extends JQueryPluginResourceReference
{
	/** Default */
	private static final long serialVersionUID = 1L;

	/** */
	private static final SocketResourceReference INSTANCE = new SocketResourceReference();

	/**
	 * Construct
	 */
	private SocketResourceReference()
	{
		super(SocketResourceReference.class, "jquery.socket.js");
	}

	/**
	 * @return {@link SocketResourceReference} instance
	 */
	public static SocketResourceReference get()
	{
		return INSTANCE;
	}
}
