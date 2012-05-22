package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.IOException;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.StreamInbound;

/**
 * WebSocket specific connection for Tomcat
 * 
 * @author remcozigterman
 */
class StreamInboundConnection extends AbstractConnection
{
	/** WebSocket connection */
	private StreamInbound socket;

	/**
	 * @return the socket
	 */
	public StreamInbound getSocket()
	{
		return socket;
	}

	/**
	 * @param socket
	 *            the socket to set
	 */
	public void setSocket(StreamInbound socket)
	{
		this.socket = socket;
	}

	/**
	 * @see nl.topicuszorg.wicket.jquerysocket.servlet.AbstractConnection#send(java.lang.String)
	 */
	@Override
	public void send(String data) throws IOException
	{
		socket.getWsOutbound().writeTextMessage(CharBuffer.wrap(data));
	}
}
