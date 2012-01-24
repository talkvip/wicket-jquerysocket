/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

import net.sf.json.JSON;

/**
 * Interface for destination
 * @author Sven Rienstra
 *
 */
public interface IStreamMessageDestination
{
	/**
	 * Send message to client
	 * @param json The json data
	 * @param clientid Optional id of the target client, if null the message will be send to all the clients
	 */
	void sendMessage(String clientid, JSON json);

	/**
	 * Add disconnect listener
	 * @param eventListener
	 * @param clientid
	 */
	void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener);
}
