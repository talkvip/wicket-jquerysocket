/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket;

import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerysocket.servlet.StreamServlet;

import org.apache.wicket.protocol.http.WebApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class to pass messages to the correct destination
 * 
 * @author Sven Rienstra
 */
public final class JQuerySocketService
{

	/** Push target */
	private static IStreamMessageDestination destination;

	/** default logger */
	public static final Logger LOG = LoggerFactory.getLogger(JQuerySocketService.class);

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
	public static void sendMessage(String clientid, String javascript)
	{
		if (javascript != null)
		{
			JSONObject json = new JSONObject();
			json.put("type", "message");
			json.put("data", javascript);

			getDestination().sendMessage(clientid, json);
		}
		else
		{
			LOG.warn("Ignore message to client " + clientid + " because there is no data");
		}
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
