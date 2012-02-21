package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.IOException;
import java.util.Date;

import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerysocket.DisconnectEventListener;

/**
 * Base connection object
 * 
 * @author Dries Schulten
 */
abstract class AbstractConnection
{
	/** The id of the client */
	private String clientId;

	/** Optional {@link DisconnectEventListener} */
	private DisconnectEventListener disconnectEventListener;

	/** When was the last connection from the client made? */
	private Date lastSeen;

	/**
	 * @return the clientId
	 */
	public String getClientId()
	{
		return clientId;
	}

	/**
	 * @param clientId
	 *            the clientId to set
	 */
	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	/**
	 * @return the disconnectEventListener
	 */
	public DisconnectEventListener getDisconnectEventListener()
	{
		return disconnectEventListener;
	}

	/**
	 * @param disconnectEventListener
	 *            the disconnectEventListener to set
	 */
	public void setDisconnectEventListener(DisconnectEventListener disconnectEventListener)
	{
		this.disconnectEventListener = disconnectEventListener;
	}

	/**
	 * @return the lastSeen
	 */
	public Date getLastSeen()
	{
		return lastSeen;
	}

	/**
	 * @param lastSeen
	 *            the lastSeen to set
	 */
	public void setLastSeen(Date lastSeen)
	{
		this.lastSeen = lastSeen;
	}

	/**
	 * Send a jQuery socket specific status update to the client
	 * 
	 * @param status
	 *            the status to send
	 * @throws IOException
	 */
	public void sendStatus(String status) throws IOException
	{
		JSONObject json = new JSONObject();

		json.put("socket", clientId);
		json.put("type", status);

		send(json.toString());
	}

	/**
	 * Send message
	 * 
	 * @param message
	 *            the {@link Message} to send
	 * @throws IOException
	 */
	public void sendMessage(Message message) throws IOException
	{
		send(message.getJson().toString());
	}

	/**
	 * Send 'data' to the client
	 * 
	 * @param data
	 *            the data to send
	 * @throws IOException
	 */
	public abstract void send(String data) throws IOException;
}
