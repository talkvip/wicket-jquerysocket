/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

import nl.topicuszorg.wicket.jquerystream.servlet.StreamServlet;

import org.apache.wicket.Application;
import org.apache.wicket.IInitializer;
import org.apache.wicket.protocol.http.WebApplication;

/**
 * @author sven
 *
 */
public class ServletInitializer implements IInitializer
{
	/**
	 * @see org.apache.wicket.IInitializer#init(org.apache.wicket.Application)
	 */
	@Override
	public void init(Application application)
	{

		if (application instanceof WebApplication)
		{
			JQueryStreamService.setDestination((IStreamMessageDestination) ((WebApplication) application)
				.getServletContext().getAttribute(StreamServlet.STREAM_SERVLET_ATTRIBUTE));
		}
	}

	/**
	 * @see org.apache.wicket.IInitializer#destroy(org.apache.wicket.Application)
	 */
	@Override
	public void destroy(Application application)
	{
		// DoNothing
	}
}
