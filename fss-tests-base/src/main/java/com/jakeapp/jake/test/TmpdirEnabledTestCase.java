package com.jakeapp.jake.test;

import java.io.File;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import com.jakeapp.jake.test.FSTestCommons;

/**
 * A unit test base class that provides a temporary directory, which is created
 * empty on start, and is deleted safely afterwords.
 */
@Ignore
public abstract class TmpdirEnabledTestCase {

	protected File tmpdir;

	@Before
	public void setup() throws Exception {
		tmpdir = FSTestCommons.provideTempDir();
	}

	@After
	public void teardown() throws Exception {
		if (tmpdir.exists())
			Assert.assertTrue(FSTestCommons.recursiveDelete(tmpdir));
		Assert.assertFalse("Cleanup done", tmpdir.exists());
	}

}
