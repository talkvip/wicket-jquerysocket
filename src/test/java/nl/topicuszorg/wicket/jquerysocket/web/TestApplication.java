package nl.topicuszorg.wicket.jquerysocket.web;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponseDecorator;
import org.apache.wicket.protocol.http.WebApplication;
import org.odlabs.wiquery.core.WiQueryDecoratingHeaderResponse;
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
		settings.setAutoImportJQueryResource(false);
		setMetaData(WiQueryInitializer.WIQUERY_INSTANCE_KEY, settings);

		setHeaderResponseDecorator(new IHeaderResponseDecorator()
		{
			/** */
			@Override
			public IHeaderResponse decorate(IHeaderResponse response)
			{
				return new WiQueryDecoratingHeaderResponse(response);
			}
		});
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
