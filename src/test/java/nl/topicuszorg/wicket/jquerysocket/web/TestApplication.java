package nl.topicuszorg.wicket.jquerysocket.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.odlabs.wiquery.core.WiQueryInitializer;
import org.odlabs.wiquery.core.WiQuerySettings;

/**
 * @author schulten
 */
public class TestApplication extends WebApplication
{
	/**
	 * @see org.apache.wicket.protocol.http.WebApplication#init()
	 */
	@Override
	protected void init()
	{
		super.init();

		WiQuerySettings settings = new WiQuerySettings();
		settings.setEnableWiqueryResourceManagement(false);
		setMetaData(WiQueryInitializer.WIQUERY_INSTANCE_KEY, settings);
	}

	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends Page> getHomePage()
	{
		return TestPage.class;
	}
}
