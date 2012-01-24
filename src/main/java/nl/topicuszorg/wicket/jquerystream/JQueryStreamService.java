/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerystream.servlet.StreamServlet;

import org.apache.wicket.protocol.http.WebApplication;

/**
 * Internal class to pass messages to the correct destination
 * @author Sven Rienstra
 *
 */
public final class JQueryStreamService
{

	/** Push target */
	private static IStreamMessageDestination destination;

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
	protected static void sendMessage(String clientid, String javascript)
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
	 * Add disconnect event listener
	 * @param eventListener
	 */
	protected static void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener)
	{
		getDestination().addDisconnectEventListener(clientid, eventListener);
	}

	/**
	 * Find destination
	 * @return
	 */
	private static IStreamMessageDestination getDestination()
	{
		// At this moment our only implementation of a destination is the servlet
		if (destination == null)
		{
			destination = (IStreamMessageDestination) WebApplication.get().getServletContext()
				.getAttribute(StreamServlet.STREAM_SERVLET_ATTRIBUTE);
			if (destination == null)
			{
				throw new IllegalStateException("Stream servlet not found");
			}
		}

		return destination;
	}
}
