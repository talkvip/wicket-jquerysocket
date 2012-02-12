/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket;

import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerysocket.servlet.StreamServlet;

import org.apache.wicket.protocol.http.WebApplication;

/**
 * Internal class to pass messages to the correct destination
 * 
 * @author Sven Rienstra
 */
public final class JQuerySocketService
{

	/** Push target */
	private static IStreamMessageDestination destination;

	/**
	 * Util class
	 */
	private JQuerySocketService()
	{
	}

	/**
	 * Send message
	 * 
	 * @param clientid
	 * @param javascript
	 */
	protected static void sendMessage(String clientid, String javascript)
	{
		JSONObject json = new JSONObject();
		json.put("type", "message");
		json.put("data", javascript);

		getDestination().sendMessage(clientid, json);
	}

	/**
	 * Get servlet url
	 * 
	 * @return
	 */
	protected static String getServletUrl()
	{
		return WebApplication.get().getServletContext().getContextPath() + "/streamservlet";
	}

	/**
	 * Add disconnect event listener
	 * 
	 * @param eventListener
	 */
	protected static void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener)
	{
		getDestination().addDisconnectEventListener(clientid, eventListener);
	}

	/**
	 * set the destination
	 * 
	 * @param nwDestination
	 */
	protected static void setDestination(IStreamMessageDestination nwDestination)
	{
		destination = nwDestination;
	}

	/**
	 * Find destination
	 * 
	 * @return
	 */
	private static IStreamMessageDestination getDestination()
	{
		// At this moment our only implementation of a destination is the servlet
		if (destination == null)
		{
			if (!WebApplication.exists())
			{
				throw new IllegalStateException("No application bound to thread and Stream servlet not yet set");
			}

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
