package com.jakeapp.core.dao;

import com.jakeapp.core.domain.Invitation;
import com.jakeapp.core.domain.Project;
import com.jakeapp.core.domain.User;
import com.jakeapp.core.domain.exceptions.InvalidProjectException;

import java.util.List;

/**
 * This is the DAO-Interface to manage invitations.
 */
public interface IInvitationDao {

	/**
	 * Stores a new Invitation into the Database
	 * @param invitation The Invitation to be stored
	 * @return the stored Invitation
	 * @throws InvalidProjectException if invalid data is passed
	 */
	public Invitation create(Invitation invitation) throws InvalidProjectException;


	/**
	 * Returns a list containing all unanswered Invitations
	 * @return a (empty) List<Invitation>
	 */
	public List<Invitation> getAll();
	
	/**
	 * Returns a list containing all unanswered <code>Invitation</code>s for the specified <code>User</code>
	 * @param user the <code>User</code> to get <code>Invitation</code>s for
	 * @return a (empty) List<Invitation>
	 */
	public List<Invitation> getAll(User user);

	/**
	 * Accepts an invitation: Transforms the invitation into a Project and stores it into the database.
	 * Then return the stored project and remove the Invitation from the database.
	 * LogEntries and other <i>per project</i> things cannot be done within this method, as they require
	 * the project specific database.
	 * @param invitation The Invitation to accept
	 * @return the Project corresponding to this Invitation
	 */
	public Project accept(Invitation invitation);


	/**
	 * Removes the Invitation from the databse.
	 * @param invitation The Invitation to be removed.
	 */
	public void reject(Invitation invitation);


}