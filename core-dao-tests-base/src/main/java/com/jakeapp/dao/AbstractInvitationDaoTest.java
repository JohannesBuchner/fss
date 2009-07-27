package com.jakeapp.core.dao;

import com.jakeapp.core.domain.Invitation;
import com.jakeapp.core.domain.Project;
import com.jakeapp.core.domain.ProtocolType;
import com.jakeapp.core.domain.User;
import com.jakeapp.core.domain.exceptions.InvalidProjectException;
import static junit.framework.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Test to test the correct behaviour of the HibernateInvitationDao
 */
@ContextConfiguration // global
public abstract class AbstractInvitationDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private IInvitationDao invitationDao;

	@Autowired
	private HibernateTemplate hibernateTemplate;


	@Before
	public void setUp() {
		// Add your code here

		hibernateTemplate.getSessionFactory().getCurrentSession().beginTransaction();
	}

	@After
	public void tearDown() {
		// Add your code here
		hibernateTemplate.getSessionFactory().getCurrentSession().getTransaction().commit();
	}

	@Test
	@Transactional
	public void testCreate() throws InvalidProjectException {
		// Add your code here

		Project project = new Project("project", UUID.fromString("a398423b-e2f1-409b-b9f2-5ebbd7fe2367"), null, null);
		User invitor = new User(ProtocolType.XMPP, "jaketest1@localhost");

		Invitation invite = new Invitation(project, invitor);

		List<Invitation> result = invitationDao.getAll();

		assertNotNull(result);
		assertEquals(result.size(), 0);


		Invitation invitationResult = 	invitationDao.create(invite);

		result = invitationDao.getAll();

		assertNotNull(result);
		assertEquals(result.size(), 1);
		assertTrue(result.contains(invite));
		assertEquals(invitationResult, invite);
		assertTrue(result.contains(invitationResult));

	}

	@Test
	public void testGetAll() {
		// Add your code here
	}

	@Test
	public void testAccept() {
		// Add your code here
	}

	@Test
	public void testReject() {
		// Add your code here
	}
}