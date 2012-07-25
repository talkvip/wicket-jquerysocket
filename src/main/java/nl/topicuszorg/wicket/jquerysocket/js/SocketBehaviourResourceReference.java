package nl.topicuszorg.wicket.jquerysocket.js;

import java.util.Map;

import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * @author Dries Schulten
 */
public class SocketBehaviourResourceReference extends PackageTextTemplate
{
	/** Default */
	private static final long serialVersionUID = 1L;

	/** Singleton instance */
	private static SocketBehaviourResourceReference INSTANCE = new SocketBehaviourResourceReference();

	/**
	 * Construct
	 */
	private SocketBehaviourResourceReference()
	{
		super(SocketBehaviourResourceReference.class, "JQuerySocketBehavior.js");
	}

	/**
	 * Render the template using the provided variables
	 * 
	 * @param vars
	 *            provided variables
	 * @return template renderd as string
	 */
	public static String render(Map<String, Object> vars)
	{
		return INSTANCE.asString(vars);
	}
}
