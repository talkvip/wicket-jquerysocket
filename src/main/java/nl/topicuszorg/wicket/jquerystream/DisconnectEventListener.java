/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

/**
 * If you want to be informed when a client disconnects, implement this eventlistener.
 * 
 * 
 * @author Sven Rienstra
 * @see {@link JQueryStreamBehavior#JQueryStreamBehavior(DisconnectEventListener)}
 */
public interface DisconnectEventListener
{
	/** On disconnect */
	void onDisconnect();
}
