package com.jakeapp.core.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.jakeapp.core.Injected;
import com.jakeapp.core.dao.IServiceCredentialsDao;
import com.jakeapp.core.domain.ServiceCredentials;
import com.jakeapp.core.domain.UserId;
import com.jakeapp.core.domain.exceptions.FrontendNotLoggedInException;
import com.jakeapp.core.domain.exceptions.InvalidCredentialsException;
import com.jakeapp.core.domain.exceptions.NoSuchMsgServiceException;
import com.jakeapp.core.services.exceptions.ProtocolNotSupportedException;
import com.jakeapp.core.services.futures.CreateAccountFuture;
import com.jakeapp.core.synchronization.IFriendlySyncService;
import com.jakeapp.core.util.availablelater.AvailableLaterObject;
import com.jakeapp.jake.ics.exceptions.NetworkException;
import com.jakeapp.jake.ics.exceptions.TimeoutException;

/**
 * Implementation of the FrontendServiceInterface
 */
public class FrontendServiceImpl implements IFrontendService {

	private static Logger log = Logger.getLogger(FrontendServiceImpl.class);

	@Injected
	private IProjectsManagingService projectsManagingService;

	@Injected
	private IProjectInvitationListener internalInvitationListener;
	
	@Injected
	private MsgServiceFactory msgServiceFactory;

	private Map<String, FrontendSession> sessions = new HashMap<String, FrontendSession>();
	
	@Injected
	private IServiceCredentialsDao serviceCredentialsDao;

	@Injected
	private IFriendlySyncService sync;
	
	/**
	 * Constructor
	 *
	 * @param projectsManagingService
	 * @param msgServiceFactory
	 * @param sync
	 */
	@Injected
	public FrontendServiceImpl(IProjectsManagingService projectsManagingService,
										MsgServiceFactory msgServiceFactory, IFriendlySyncService sync,
										IServiceCredentialsDao serviceCredentialsDao,
										IProjectInvitationListener invitationListener
		) {
		this.setProjectsManagingService(projectsManagingService);
		this.msgServiceFactory = msgServiceFactory;
		this.sync = sync;
		this.serviceCredentialsDao = serviceCredentialsDao;
		this.internalInvitationListener = invitationListener;
	}

	private IProjectsManagingService getProjectsManagingService() {
		return projectsManagingService;
	}

	private IServiceCredentialsDao getServiceCredentialsDao() {
		return this.serviceCredentialsDao;
	}

	private void setProjectsManagingService(
			  IProjectsManagingService projectsManagingService) {
		this.projectsManagingService = projectsManagingService;
	}

	/**
	 * Checks frontend-credentials and throws exceptions if they are not
	 * correct.
	 *
	 * @param credentials The credentials to be checked
	 * @throws com.jakeapp.core.domain.exceptions.InvalidCredentialsException
	 *
	 * @throws IllegalArgumentException
	 * @see #authenticate(Map)
	 */
	private void checkCredentials(Map<String, String> credentials)
			  throws IllegalArgumentException, InvalidCredentialsException {

		if (credentials == null)
			throw new IllegalArgumentException();

		if (!credentials.isEmpty())
			throw new InvalidCredentialsException(
					"You are doing it wrong (don't set any credentials for core login)");
		// TODO do further checking for later versions
	}

	private void setSessions(Map<String, FrontendSession> sessions) {
		this.sessions = sessions;
	}

	private Map<String, FrontendSession> getSessions() {
		return sessions;
	}

	private void addSession(String sessid, FrontendSession session) {
		this.getSessions().put(sessid, session);
	}

	private boolean removeSession(String sessid) {
		FrontendSession fes;

		fes = this.getSessions().remove(sessid);

		return fes != null;
	}

	/**
	 * @param sessionId The id associated with the session after it was created
	 * @throws IllegalArgumentException if <code>sessionId</code> was null
	 * @throws com.jakeapp.core.domain.exceptions.FrontendNotLoggedInException
	 *                                  if no Session associated with <code>sessionId</code> exists.
	 */
	private FrontendSession getSession(String sessionId) throws IllegalArgumentException,
			  FrontendNotLoggedInException {
		checkSession(sessionId);
		return this.getSessions().get(sessionId);
	}

	/**
	 * @return A sessionid for a new session
	 */
	private String makeSessionID() {
		return UUID.randomUUID().toString();
	}


	@Override
	public String authenticate(Map<String, String> credentials)
			  throws IllegalArgumentException, InvalidCredentialsException {
		String sessid;

		this.checkCredentials(credentials);

		// create new session
		sessid = makeSessionID();
		this.addSession(sessid, new FrontendSession());

		return sessid;
	}

	@Override
	public boolean logout(String sessionId) throws IllegalArgumentException,
			  FrontendNotLoggedInException {
		boolean successfullyRemoved;

		if (sessionId == null)
			throw new IllegalArgumentException();

		successfullyRemoved = this.removeSession(sessionId);
		if (!successfullyRemoved)
			throw new FrontendNotLoggedInException();

		return true;
	}

	@Override
	public IProjectsManagingService getProjectsManagingService(String sessionId)
			  throws IllegalArgumentException, FrontendNotLoggedInException, IllegalStateException {
		checkSession(sessionId);
		// 3. return ProjectsManagingService
		return this.getProjectsManagingService();
	}

	private void checkSession(String sessionId) throws FrontendNotLoggedInException {


		// 1. check if session is null, if so throw IllegalArgumentException
		if (sessionId == null || sessionId.isEmpty())
			throw new IllegalArgumentException("invalid sessionid");

		// 2. check session validity
		if (!sessions.containsKey(sessionId)) {
			log.debug("sessions dont contain ssesionid " + sessionId);
			log.debug("this are the stored sessions ");
			for (String entry : sessions.keySet()) {
				log.debug(entry);
			}
			throw new FrontendNotLoggedInException("Invalid Session; Not logged in");
		}

	}

	@Override
	public List<MsgService<UserId>> getMsgServices(String sessionId)
			  throws IllegalArgumentException, FrontendNotLoggedInException, IllegalStateException {
		this.checkSession(sessionId);
		return this.getMsgServices();
	}

	private List<MsgService<UserId>> getMsgServices() throws IllegalStateException {
		return this.msgServiceFactory.getAll();
	}

	@Override
	public AvailableLaterObject<Void> createAccount(String sessionId, ServiceCredentials credentials)
			  throws FrontendNotLoggedInException, InvalidCredentialsException,
			  ProtocolNotSupportedException, NetworkException {
		checkSession(sessionId);
		MsgService svc = msgServiceFactory.createMsgService(credentials);

		return new CreateAccountFuture(svc);
	}

	@Override
	public MsgService addAccount(String sessionId, ServiceCredentials credentials)
			  throws FrontendNotLoggedInException, InvalidCredentialsException,
			  ProtocolNotSupportedException {
		checkSession(sessionId);

		return msgServiceFactory.addMsgService(credentials);
	}

	@Override
	public void removeAccount(String sessionId, MsgService msg)
					throws FrontendNotLoggedInException, NoSuchMsgServiceException {
		checkSession(sessionId);

		// TODO: implement!
		log.warn("Needs IMPLEMENTATION");
		//msgServiceFactory.removeMsgService(msg);
	}

	@Override
	public void signOut(String sessionId) throws FrontendNotLoggedInException {
		FrontendSession session;
		Iterable<MsgService> msgServices;

		this.checkSession(sessionId);
		this.getSessions().remove(sessionId);

		/* do not logout - other UIs may access the core */
	}

	@Override
	public void ping(String sessionId) throws IllegalArgumentException,
			  FrontendNotLoggedInException {
		checkSession(sessionId);

		// TODO
	}

	@Override
	public IFriendlySyncService getSyncService(String sessionId)
			  throws FrontendNotLoggedInException {
		this.checkSession(sessionId);
		return this.sync;
	}


	//	@Override
	public IFriendlySyncService getSync() {
		return this.sync;
	}

	@Override
	@Transactional
	public Collection<ServiceCredentials> getLastLogins() {
		return this.getServiceCredentialsDao().getAll();
	}

	@Override
	@Transactional
	public AvailableLaterObject<Boolean> login(final String session, final MsgService service, final String password,
			final boolean rememberPassword) {

		AvailableLaterObject<Boolean> ret = new AvailableLaterObject<Boolean>() {

			@Override
			public Boolean calculate() throws TimeoutException, NetworkException  {
				ServiceCredentials credentials;
				boolean result;
				checkSession(session);
				
				/* login */
				if (password == null) {
					result = service.login();
				} else {
					result = service.login(password, rememberPassword);
				}
				ProjectInvitationHandler invitesHandler = new ProjectInvitationHandler(null);
				invitesHandler
						.setInvitationListener(FrontendServiceImpl.this.internalInvitationListener);
				service.getMainIcs().getMsgService().registerReceiveMessageListener(invitesHandler);
				return result;
			}
			
		};
		
		return ret;
	}

}
