package nl.topicuszorg.wicket.jquerystream.js;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * Resource reference to the jquery stream javascript files
 * 
 * @author Dries Schulten
 */
public class StreamResourceReference extends JavaScriptResourceReference
{
	/** Default */
	private static final long serialVersionUID = 1L;

	/** */
	private static final StreamResourceReference INSTANCE = new StreamResourceReference();

	/**
	 * Construct
	 */
	private StreamResourceReference()
	{
		super(StreamResourceReference.class, getResourceName());
	}

	/**
	 * @return {@link StreamResourceReference} instance
	 */
	public static StreamResourceReference get()
	{
		return INSTANCE;
	}

	/** In development mode use full javascript */
	private static String getResourceName()
	{
		if (Application.get().getConfigurationType() == RuntimeConfigurationType.DEVELOPMENT)
		{
			return "jquery.stream-1.2.js";
		}

		return "jquery.stream-1.2.min.js";
	}
}
