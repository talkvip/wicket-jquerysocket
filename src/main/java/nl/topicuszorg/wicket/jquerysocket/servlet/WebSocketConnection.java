package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket;

/**
 * WebSocket specific connection
 * 
 * @author Dries Schulten
 */
class WebSocketConnection extends AbstractConnection
{
	/** WebSocket connection */
	private WebSocket.Connection socket;

	/**
	 * @return the socket
	 */
	public WebSocket.Connection getSocket()
	{
		return socket;
	}

	/**
	 * @param socket
	 *            the socket to set
	 */
	public void setSocket(WebSocket.Connection socket)
	{
		this.socket = socket;
	}

	/**
	 * @see nl.topicuszorg.wicket.jquerysocket.servlet.AbstractConnection#send(java.lang.String)
	 */
	@Override
	public void send(String data) throws IOException
	{
		socket.sendMessage(data);
	}
}
