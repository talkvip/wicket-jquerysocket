/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerystream.servlet.StreamServlet;

import org.apache.wicket.protocol.http.WebApplication;

/**
 * @author sven
 *
 */
public final class JQueryStreamService
{

	/** Push target */
	private static StreamMessageDestination destination;

	/**
	 * Util class
	 */
	private JQueryStreamService()
	{
	}

	/**
	 * Send message
	 * @param clientid
	 * @param javascript
	 */
	public static void sendMessage(String clientid, String javascript)
	{
		JSONObject json = new JSONObject();
		json.put("javascript", javascript);

		getDestination().sendMessage(clientid, json);
	}

	/**
	 * Get servlet url
	 * @return
	 */
	protected static String getServletUrl()
	{
		return WebApplication.get().getServletContext().getContextPath() + "/streamservlet";
	}

	/**
	 * Voeg disconnect event listener toe
	 * @param eventListener
	 */
	protected static void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener)
	{
		getDestination().addDisconnectEventListener(clientid, eventListener);
	}

	/**
	 * Haal destination op
	 * @return
	 */
	private static StreamMessageDestination getDestination()
	{
		// Enige manier die we momenteel kennen is via een servlet
		if (destination == null)
		{
			destination = (StreamMessageDestination) WebApplication.get().getServletContext()
				.getAttribute(StreamServlet.STREAM_SERVLET_ATTRIBUTE);
			if (destination == null)
			{
				throw new IllegalStateException("Stream servlet not found");
			}
		}

		return destination;
	}
}
