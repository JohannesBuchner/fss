package com.jakeapp.jake.fss;

import java.io.File;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.runner.RunWith;

import com.googlecode.junit.ext.PrerequisiteAwareClassRunner;

@RunWith(PrerequisiteAwareClassRunner.class)
public class FSServiceTestCase extends FSTestCase {

	static Logger log = Logger.getLogger(FSServiceTestCase.class);

	protected FSService fss = null;

	protected void wipeRoot() {
		File f = fss.getRootPath();
		Assert.assertTrue(f.exists() && f.isDirectory());
		Assert.assertTrue(recursiveDelete(f));
		f.mkdirs();
		Assert.assertTrue(f.exists() && f.isDirectory() && f.list().length == 0);
	}

	@Override
	// @Prerequisite(checker = DesktopSupportedChecker.class) - does not work
	public void setUp() throws Exception {
		super.setUp();

		HashValue.DIGEST = "SHA-512";
		HashValue.N_BITS = 512;

		if ((new DesktopSupportedChecker()).satisfy()) {
			fss = new FSService();
			fss.setRootPath(new ProjectDir(mytempdir));
			Assert.assertEquals("rootpath", mytempdir, fss.getRootPath());
		}
	}
}
