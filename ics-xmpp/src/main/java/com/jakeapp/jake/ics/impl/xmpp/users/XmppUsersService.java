package com.jakeapp.jake.ics.impl.xmpp.users;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

import com.jakeapp.jake.ics.UserId;
import com.jakeapp.jake.ics.exceptions.NoSuchUseridException;
import com.jakeapp.jake.ics.exceptions.NotLoggedInException;
import com.jakeapp.jake.ics.impl.xmpp.XmppConnectionData;
import com.jakeapp.jake.ics.impl.xmpp.XmppUserId;
import com.jakeapp.jake.ics.status.IOnlineStatusListener;
import com.jakeapp.jake.ics.status.IUsersService;


public class XmppUsersService implements IUsersService {

	public static final Logger log = Logger.getLogger(XmppUsersService.class);

	public XmppConnectionData con;

	public Set<IOnlineStatusListener> onlinereceivers = new HashSet<IOnlineStatusListener>();

	public String groupname;

	public XmppUsersService(XmppConnectionData connection) {
		this.con = connection;
	}

	@Override
	public void registerOnlineStatusListener(IOnlineStatusListener osl) {
		this.onlinereceivers.add(osl);
	}

	public void addUser(UserId user, String name) throws NoSuchUseridException,
			NotLoggedInException, IOException {
		assertLoggedIn();
		RosterEntry re = getGroup().getEntry(getXmppId(user));
		if (re == null) {
			String[] groups = { this.groupname };
			try {
				getRoster().createEntry(getXmppId(user), name, groups);
			} catch (XMPPException e) {
				throw new IOException(e);
			}
		}
	}

	private String getXmppId(UserId user) throws NoSuchUseridException {
		if (!(new XmppUserId(user).isOfCorrectUseridFormat()))
			throw new NoSuchUseridException();
		return user.getUserId();
	}

	private void assertLoggedIn() throws NotLoggedInException {
		if (!this.con.getService().getStatusService().isLoggedIn())
			throw new NotLoggedInException();
	}

	public void removeUser(UserId user) throws NotLoggedInException,
			NoSuchUseridException, IOException {
		assertLoggedIn();
		RosterEntry re = getGroup().getEntry(getXmppId(user));

		if (re == null)
			return; // we silently ignore double deletes

		try {
			getGroup().removeEntry(re);
		} catch (XMPPException e) {
			throw new IOException(e);
		}
	}

	public Roster getRoster() throws NotLoggedInException {
		assertLoggedIn();
		return this.con.getConnection().getRoster();
	}

	public Iterable<UserId> getUsers() throws NotLoggedInException {
		assertLoggedIn();

		Set<UserId> users = new HashSet<UserId>();

		for (RosterEntry re : getGroup().getEntries()) {
			users.add(new XmppUserId(re.getUser()));
		}
		return users;
	}

	public RosterGroup getGroup() throws NotLoggedInException {
		assertLoggedIn();
		RosterGroup rg = getRoster().getGroup(this.groupname);
		if (rg == null) {
			rg = getRoster().createGroup(this.groupname);
		}
		return getRoster().getGroup(this.groupname);
	}

	private boolean isFriend(String xmppid) throws NotLoggedInException {
		assertLoggedIn();
		if (getGroup().getEntry(xmppid) != null)
			return true;
		else
			return false;
	}

	public class DiscoveryThread implements Runnable {

		private final Logger log = Logger.getLogger(DiscoveryThread.class);

		String xmppid;

		DiscoveryThread(String who) {
			super();
			this.xmppid = who;
		}

		public void run() {
			Thread.currentThread().setName("Discovering " + this.xmppid);
			this.log.debug(Thread.currentThread() + " starting ... ");

			this.log.info("trying to discover capabilities for user ...");
			int tries = 2;
			while (tries > 0
					&& XmppUsersService.this.con.getService()
							.getStatusService().isLoggedIn())
				try {
					if (isCapable(this.xmppid)) {
						if (isFriend(this.xmppid)) {
							this.log.info("It is a friend!");
							notifyAboutPresenceChange(this.xmppid);
						}
					}
					break;
				} catch (IOException e) {
					this.log.error("discovering capabilities failed!", e);
					tries--;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// not important, don't care, best effort
					}
				} catch (NotLoggedInException e) {
					this.log.debug("We got logged out somehow", e);
				}
			this.log.debug(Thread.currentThread() + " done");
		}
	}

	public void notifyAboutPresenceChange(String xmppid) {
		for (IOnlineStatusListener osl : this.onlinereceivers) {
			osl.onlineStatusChanged(xmppid);
		}
	}
	
	private boolean isCapable(String xmppid) throws IOException {
		ServiceDiscoveryManager discoManager = ServiceDiscoveryManager
				.getInstanceFor(this.con.getConnection());
		DiscoverInfo discoInfo;
		try {
			discoInfo = discoManager.discoverInfo(xmppid);
		} catch (XMPPException e) {
			log.debug("Something weird happened", e);
			throw new IOException(e);
		}
		if (discoInfo.containsFeature(this.con.getNamespace())) {
			log.info("user came online with our feature");
			return true;
		} else {
			log.info("user came online withOUT our feature");
			return false;
		}
	}

	public boolean isCapable(UserId userid) throws IOException, NotLoggedInException, NoSuchUseridException {
		assertLoggedIn();
		return isCapable(getXmppId(userid));
	}

	public void requestOnlineNotification(UserId userid)
			throws NotLoggedInException {
		Presence presence = getRoster().getPresence(userid.getUserId());
		String xmppid = userid.getUserId();
		log.debug("presenceChanged: " + xmppid + " - " + presence);
		if (presence.isAvailable()) {
			new Thread(new DiscoveryThread(xmppid)).start();
		} else {
			notifyAboutPresenceChange(xmppid);
		}
		new Thread(new DiscoveryThread(xmppid)).start();
	}

	@Override
	public boolean isFriend(UserId userid) throws NotLoggedInException, NoSuchUseridException {
		assertLoggedIn();
		return getGroup().contains(getXmppId(userid));
	}
}