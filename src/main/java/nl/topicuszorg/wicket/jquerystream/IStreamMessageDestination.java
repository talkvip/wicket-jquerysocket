/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

import net.sf.json.JSON;

/**
 * @author sven
 *
 */
public interface StreamMessageDestination
{
	/**
	 * Stuur message
	 * @param json json data
	 * @param clientid Id van de client waar het bericht heen moet, optioneel
	 */
	void sendMessage(String clientid, JSON json);

	/**
	 * Voeg disconnect event listener toe
	 * @param eventListener
	 * @param clientid
	 */
	void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener);
}
