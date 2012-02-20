package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.util.Date;

import javax.servlet.AsyncContext;

import nl.topicuszorg.wicket.jquerysocket.DisconnectEventListener;

/**
 * Connection object, wraps a {@link AsyncContext} and maintains some extra information about the connection
 * 
 * @author Dries Schulten
 */
public class Connection
{
	/** The id of the client */
	private String clientId;

	/** {@link AsyncContext} */
	private AsyncContext asyncContext;

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
	 * @return the asyncContext
	 */
	public AsyncContext getAsyncContext()
	{
		return asyncContext;
	}

	/**
	 * @param asyncContext
	 *            the asyncContext to set
	 */
	public void setAsyncContext(AsyncContext asyncContext)
	{
		this.asyncContext = asyncContext;
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
}
