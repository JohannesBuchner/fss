package com.jakeapp.core.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.jakeapp.core.Injected;
import com.jakeapp.core.dao.IServiceCredentialsDao;
import com.jakeapp.core.dao.exceptions.NoSuchServiceCredentialsException;
import com.jakeapp.core.domain.ProtocolType;
import com.jakeapp.core.domain.ServiceCredentials;
import com.jakeapp.core.domain.UserId;
import com.jakeapp.core.domain.exceptions.InvalidCredentialsException;
import com.jakeapp.core.services.exceptions.ProtocolNotSupportedException;

/**
 * Manager that creates and stores MsgServices
 */
@Transactional
public class MsgServiceManager {

	private static Logger log = Logger.getLogger(MsgServiceManager.class);

	/**
	 * Key is the Credentials UUID
	 */
	private Map<String, MsgService<UserId>> msgServices = new HashMap<String, MsgService<UserId>>();

	@Injected
	private IServiceCredentialsDao serviceCredentialsDao;

	@Injected
	private ICSManager icsManager;

	public void setICSManager(ICSManager icsManager) {
		this.icsManager = icsManager;
	}

	private IServiceCredentialsDao getServiceCredentialsDao() {
		return this.serviceCredentialsDao;
	}

	public void setServiceCredentialsDao(IServiceCredentialsDao serviceCredentialsDao) {
		this.serviceCredentialsDao = serviceCredentialsDao;
	}


	/**
	 * Creates a new MessageService if one for the specified credentials does
	 * not exist yet.
	 * 
	 * @param credentials
	 *            The ServiceCredentials to create a MsgService for.
	 * @throws ProtocolNotSupportedException
	 *             if the protocol specified in the credentials is not
	 *             supported.
	 */
	private MsgService<UserId> create(ServiceCredentials credentials)
			throws ProtocolNotSupportedException {
		log.info("Creating MsgService for " + credentials);
		MsgService<UserId> msgService;

		if (credentials == null) {
			throw new InvalidCredentialsException("Credentials cannot be null!");
		}
		if (credentials.getUuid() == null) {
			log.warn("no UUID set in credentials. fixing ...");
			credentials.setUuid(UUID.randomUUID().toString());
		}

		MsgService.setServiceCredentialsDao(getServiceCredentialsDao());

		log.debug("creating MsgService with crendentials: " + credentials);
		log.debug("UserId="
				+ credentials.getUserId()
				+ " pwl = "
				+ ((credentials.getPlainTextPassword() == null) ? "null" : ""
						+ credentials.getPlainTextPassword().length()));

		if (credentials.getProtocol() != null
				&& credentials.getProtocol().equals(ProtocolType.XMPP)) {
			log
					.debug("Creating new XMPPMsgService for userId "
							+ credentials.getUserId());
			msgService = new XMPPMsgService();
			msgService.setIcsManager(this.icsManager);
			msgService.setServiceCredentials(credentials);
			this.msgServices.put(credentials.getUuid(), msgService);
			msgService.setUserId(createUserforMsgService(credentials));

			log.debug("resulting MsgService is " + msgService);
			return msgService;
		} else {
			log.warn("Currently unsupported protocol given");
			throw new ProtocolNotSupportedException();
		}
	}

	/**
	 * Every MsgService has a UserId connected. This creates the UserId from the
	 * ServiceCredentals.
	 * 
	 * @param credentials
	 *            ServiceCredentials that are used to create the UserId
	 * @return
	 * @throws ProtocolNotSupportedException
	 */
	private UserId createUserforMsgService(ServiceCredentials credentials)
			throws ProtocolNotSupportedException {

		// switch through the supported protocols and create the user
		switch (credentials.getProtocol()) {
			case XMPP:
				UUID res;

				if (credentials.getUuid() != null)
					res = UUID.fromString(credentials.getUuid());
				else
					res = UUID.randomUUID();
				credentials.setUuid(res);

				return new UserId(ProtocolType.XMPP, credentials.getUserId());

			default:
				throw new ProtocolNotSupportedException("Backend not yet implemented");
		}
	}

	public List<MsgService<UserId>> getLoaded() {
		List<MsgService<UserId>> result = new ArrayList<MsgService<UserId>>();
		result.addAll(this.msgServices.values());
		log.debug("got " + result.size() + " messageservices");
		return result;
	}

	@Transactional
	public List<MsgService<UserId>> getAll() {
		log.debug("calling getAll");

		List<ServiceCredentials> credentialsList = this.serviceCredentialsDao.getAll();
		log.debug("Found " + credentialsList.size() + " Credentials in the DB");
		log.debug("Found " + this.msgServices.size() + " Credentials in the Cache");


		for (ServiceCredentials credentials : credentialsList) {
			if (!this.msgServices.containsKey(credentials.getUuid())) {
				try {
					MsgService<UserId> service = this.create(credentials);
					if (credentials.getUuid() == null) {
						throw new IllegalStateException(
								"createMsgService didn't fill uuid");
					}
					this.msgServices.put(credentials.getUuid(), service);
				} catch (ProtocolNotSupportedException e) {
					log.warn("Protocol not supported: ", e);
					log.info("ignoring unsupported entry " + credentials);
				}
			}
		}
		return getLoaded();
	}

	/**
	 * creates and adds a msgservice for the right protocol This adds the
	 * ServiceCrenentials from the MsgService into the database.
	 * 
	 * @param credentials
	 * @return the service
	 * @throws InvalidCredentialsException
	 * @throws ProtocolNotSupportedException
	 */
	private MsgService add(ServiceCredentials credentials)
			throws InvalidCredentialsException, ProtocolNotSupportedException {
		log.debug("calling addMsgService");

		if (credentials.getUuid() == null) {
			log.debug("no UUID in credentials. fixing ...");
			credentials.setUuid(UUID.randomUUID());
		}

		// persist the ServiceCredentials!
		try {
			this.getServiceCredentialsDao().read(UUID.fromString(credentials.getUuid()));
			this.getServiceCredentialsDao().update(credentials);
		} catch (NoSuchServiceCredentialsException e) {
			this.getServiceCredentialsDao().create(credentials);
		}
		getAll();
		return getOrCreate(credentials);
	}

	/**
	 * Tries to return the MsgService that is associated with
	 * ServiceCredentials. ServiceCredentials is saved in the DB,
	 * MsgServiccreateMsgServicee not. If no MsgService was created until now,
	 * we try to created it.
	 * 
	 * @param credentials
	 * @return the MsgService or null.
	 */
	public MsgService getOrCreate(ServiceCredentials credentials) {
		log.debug("Get MsgService by credentials: " + credentials);

		MsgService<UserId> msg = find(credentials);
		if (msg != null) {
			log.debug("reused already-loaded msgservice");
			return msg;
		}
		log.info("not found in cache");

		try {
			return this.add(credentials);
		} catch (Exception e) {
			log.error("Unable to create MessageService:", e);
			return null;
		}
	}


	private MsgService<UserId> find(ServiceCredentials credentials) {
		if (this.msgServices.containsKey(credentials.getUuid())) {
			return this.msgServices.get(credentials.getUuid());
		}
		List<MsgService<UserId>> list = getAll();
		if (this.msgServices.containsKey(credentials.getUuid())) {
			return this.msgServices.get(credentials.getUuid());
		}

		// find a similar one
		for (MsgService<UserId> m : list) {
			ServiceCredentials c = m.getServiceCredentials();
			if (credentials.getProtocol() == c.getProtocol()
					&& credentials.getUserId().equals(c.getUserId())) {
				credentials.setUuid(c.getUuid());
				log.debug("found a similar one");
				break;
			}
		}
		if (this.msgServices.containsKey(credentials.getUuid())) {
			log.debug("found after adjusting UUID");
			return this.msgServices.get(credentials.getUuid());
		}
		return null;
	}

	/**
	 * releases all stored MessageServices
	 */
	public void free() {
		this.msgServices.clear();
	}
}