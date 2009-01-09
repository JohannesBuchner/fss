package com.jakeapp.core.services;

import java.util.List;

import com.jakeapp.jake.ics.ICService;

/**
 * Session of work that is established whenever any Frontend
 * accesses the core. The session contains references to all
 * Services needed by the frontend.
 * @author christopher
 */
public class FrontendSession {
	public FrontendSession() {
		//TODO create and store ics-Service-Implementations
	}
	
    public IProjectsManagingService getMetaProjectService() throws  IllegalStateException {
        return null; // TODO
    }

    public List<ICService> getICServices() throws  IllegalStateException {
        return null; // TODO
    }
}
