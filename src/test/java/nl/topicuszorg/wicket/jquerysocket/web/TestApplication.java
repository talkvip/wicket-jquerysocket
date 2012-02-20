package nl.topicuszorg.wicket.jquerysocket.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

/**
 * @author schulten
 */
public class TestApplication extends WebApplication
{
	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends Page> getHomePage()
	{
		return TestPage.class;
	}
}
