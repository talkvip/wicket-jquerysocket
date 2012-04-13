package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;

/**
 * Connection implementation for a streamed connection
 * 
 * @author Dries Schulten
 */
class StreamConnection extends AbstractConnection
{
	/** {@link AsyncContext} */
	private AsyncContext asyncContext;

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
	 * @see nl.topicuszorg.wicket.jquerysocket.servlet.AbstractConnection#send(String)
	 */
	@Override
	public synchronized void send(String data) throws IOException
	{
		PrintWriter writer = asyncContext.getResponse().getWriter();

		writer.print("data: ");
		writer.print(data);
		writer.print("\n\n");
		writer.flush();
	}
}
