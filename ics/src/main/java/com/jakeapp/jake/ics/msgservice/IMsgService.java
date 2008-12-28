package com.jakeapp.jake.ics.msgservice;

import com.jakeapp.jake.ics.UserId;
import com.jakeapp.jake.ics.exceptions.NetworkException;
import com.jakeapp.jake.ics.exceptions.NoSuchUseridException;
import com.jakeapp.jake.ics.exceptions.NotLoggedInException;
import com.jakeapp.jake.ics.exceptions.OtherUserOfflineException;
import com.jakeapp.jake.ics.exceptions.TimeoutException;



/**
 * 
 * The task of the InterClient Communication Service (ICService) is to provide 
 * a communication layer based on the network for communication between users 
 * based on messages and objects.
 * 
 * <p>userid:   A way of identifying the user within the used network protocol.
 *           example: user@host</p>
 * <p>network service: The implementation of IICService use some sort of 
 *           Interclient Communication protocol. We reference to this underlying
 *           system as network service.
 *           examples: XMPP, TCP-Sockets, ...</p> 
 * 
 * @author johannes
 **/

public interface IMsgService {

	/**
	 * Sends a object to another user.
	 * @param to_userid        Userid to send to 
	 * @param objectidentifier name of the content so the receiver knows what 
	 *                         it is receiving
	 * @param content          Full object content as String
	 * @return                 wether the object could be sent. Does not 
	 *                         guarantee the object has been retrieved.
	 * @throws NetworkException  if the network connection is down
	 * @throws TimeoutException if a timeout occured while transmitting the object
	 * @throws NotLoggedInException if the user is not logged in
	 */
	public Boolean sendObject(UserId to_userid, String objectidentifier, 
			byte[] content) 
		throws NetworkException, NotLoggedInException, TimeoutException,
			NoSuchUseridException, OtherUserOfflineException;
	
	/**
	 * Registers a callback for the event that a object is received.
	 * If sendMessage() uses sendObject(), this is also called for messages. 
	 * 
	 * @param rl    object to be called
	 */
	public void registerReceiveObjectListener(IObjectReceiveListener rl);
	
	/**
	 * Sends a message to another user.
	 * May call sendObject with the objectidentifier "message".
	 * @param to_userid        Userid to send to
	 * @param content          Full message content as String 
	 * @return                 wether the message could be sent. Does not 
	 *                         guarantee the object has been retrieved.
	 * @throws NetworkException if the network connection is down
	 * @throws NotLoggedInException if the user is not logged in
	 * @throws TimeoutException if a timeout occured
	 */
	public Boolean sendMessage(UserId to_userid, String content)
		throws NetworkException, NotLoggedInException, TimeoutException, 
			NoSuchUseridException, OtherUserOfflineException;
	
	/**
	 * Registers a callback for the event that a message is received.
	 * 
	 * @param receiveListener    object to be called
	 */
	public void registerReceiveMessageListener(IMessageReceiveListener receiveListener);
	

}