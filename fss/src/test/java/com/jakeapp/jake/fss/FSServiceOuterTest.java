package com.jakeapp.jake.fss;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.concurrent.Semaphore;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.googlecode.junit.ext.Prerequisite;
import com.googlecode.junit.ext.PrerequisiteAwareClassRunner;
import com.jakeapp.jake.fss.IModificationListener.ModifyActions;
import com.jakeapp.jake.fss.exceptions.CreatingSubDirectoriesFailedException;
import com.jakeapp.jake.fss.exceptions.InvalidFilenameException;
import com.jakeapp.jake.fss.exceptions.NotAFileException;
import com.jakeapp.jake.fss.exceptions.NotAReadableFileException;

@RunWith(PrerequisiteAwareClassRunner.class)
public class FSServiceOuterTest extends FSServiceTestCase {

	private static final Logger log = Logger
			.getLogger(FSServiceOuterTest.class);

	@Override
	public void setUp() throws Exception {
		super.setUp();
		wipeRoot();
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testGetFullpath() throws Exception {
		String sep = File.separator;
		File root = mytempdir;

		Assert.assertEquals("root", root.getAbsolutePath(),
				fss.getFullpath("/"));
		Assert.assertEquals(fss.getFullpath("testfile.xml"), root + sep
				+ "testfile.xml");
		Assert.assertEquals(fss.getFullpath("folder/to/testfile.xml"), root
				+ sep + "folder" + sep + "to" + sep + "testfile.xml");

		Assert.assertEquals(fss.joinPath("foldera", "folderb"), "foldera" + sep
				+ "folderb");
		Assert.assertEquals(
				fss.joinPath("foldera" + sep + "to" + sep, "folderb"),
				"foldera" + sep + "to" + sep + "folderb");

	}

	@Test(expected = InvalidFilenameException.class)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testReadFileInvalidFilename() throws Exception {
		fss.readFile(":");
	}

	@Test(expected = InvalidFilenameException.class)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testReadFileInvalidFilenameUpwards() throws Exception {
		fss.readFile("/../../test.xml");
	}

	@Test(expected = InvalidFilenameException.class)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testReadFileInvalidFilenameUpwards2() throws Exception {
		fss.readFile("/folder/../../../test.xml");
	}

	@Test(expected = FileNotFoundException.class)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testReadFileDoesNotExist() throws Exception {
		fss.readFile("/doesntexist");
	}

	@Test(expected = NotAFileException.class)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testReadFileThatIsAFolder() throws Exception {
		fss.readFile("/");
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testReadFile() throws Exception {
		String filename = "/test.out";
		String content = "This is interesting\nstuff\n\nyou know...\n\n";
		{
			FileWriter w = new FileWriter(mytempdir + File.separator + filename);
			BufferedWriter bw = new BufferedWriter(w);
			bw.write(content);
			bw.close();
			w.close();

			byte[] r = fss.readFile(filename);
			Assert.assertEquals("content read correctly", new String(r),
					content);
		}
		{
			byte[] c = { 32, 12, 61, 72, 245 - 256, 11, 100, 0, 23, 1, 4, 21,
					254 - 256, 21, 1, 2 };

			FileOutputStream bw = new FileOutputStream(mytempdir
					+ File.separator + filename);
			bw.write(c);
			bw.close();

			byte[] r = fss.readFile(filename);
			Assert.assertEquals("content read correctly", new String(r),
					new String(c));
			Assert.assertEquals("content read correctly", r.length, c.length);
		}
		{
			boolean setupWorked = true;
			try {
				Runtime.getRuntime().exec(
						"chmod 000 " + mytempdir + File.separator + filename);
				Thread.sleep(100);
			} catch (Exception e) {
				setupWorked = false;
			}
			if (setupWorked) {
				try {
					fss.readFile(filename);
					Assert.fail("NotAReadableFileException");
				} catch (NotAReadableFileException e) {
				}
			}
		}
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testFolderExists() throws Exception {
		Assert.assertTrue(fss.folderExists("/"));
		Assert.assertFalse(fss.folderExists("/folderDoesNotExist"));
		String[] dirnames = { "foobar", "/foo/bar/", "ran/dom/stuff" };
		for (String dirname : dirnames) {
			File f = new File(fss.getRootPath() + File.separator + dirname
					+ File.separator);
			f.mkdirs();
			Assert.assertTrue(fss.folderExists(dirname));
		}
		try {
			fss.folderExists("/fol:derDoesNotExist");
			Assert.fail("InvalidFilenameException");
		} catch (InvalidFilenameException e) {
		}
		File f = new File(fss.getRootPath() + File.separator + "test.out");
		f.createNewFile();
		Assert.assertFalse(fss.folderExists("test.out"));
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testFileExists() throws Exception {
		wipeRoot();
		Assert.assertFalse(fss.fileExists("/fileDoesNotExist"));
		Assert.assertFalse(fss.fileExists("fileDoesNotExist"));

		String[] dirnames = { "foobar", "/foo/bar", "ran/dom/stuff" };
		for (String dirname : dirnames) {
			File f = new File(fss.getRootPath() + File.separator + dirname
					+ File.separator);
			f.getParentFile().mkdirs();
			f.createNewFile();
			Assert.assertTrue(fss.fileExists(dirname));
		}
		try {
			fss.fileExists("/fil:eDoesNotExist");
			Assert.fail("InvalidFilenameException");
		} catch (InvalidFilenameException e) {
		}

		File f = new File(fss.getRootPath() + File.separator + "foobar");
		f.delete();
		System.gc();
		Thread.yield();
		f.mkdirs();
		Assert.assertFalse(fss.fileExists("foobar"));

	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testListFolder() throws Exception {
		wipeRoot();
		String folder = "jakeAtestFolder";
		recursiveDelete(new File(fss.getRootPath() + File.separator + folder));

		String[] content = { "B", "C", "E", "D", "F", "G", "H", "J" };
		createFiles(folder, content);

		List<String> s = fss.listFolder(folder);
		String sep = "/";

		for (int j = 0; j < content.length; j++) {
			boolean found = false;
			for (String value : s) {
				if (value.equals(folder + sep + content[j])) {
					found = true;
					break;
				}
			}
			Assert.assertTrue(found);
			if (j < 4) {
				Assert.assertTrue("folder: " + content[j],
						fss.folderExists(folder + sep + content[j]));
			} else {
				Assert.assertTrue("file: " + content[j],
						fss.fileExists(folder + sep + content[j]));
			}
		}

		Assert.assertTrue(s.size() == content.length);
	}

	private void createFiles(String folder, String[] content)
			throws IOException {
		for (int i = 0; i < content.length; i++) {
			File f = new File(fss.getRootPath() + File.separator + folder
					+ File.separator + content[i]);
			if (i < 4) {
				f.mkdirs();
				Assert.assertTrue(f.isDirectory());
			} else {
				f.createNewFile();
				Assert.assertTrue(f.isFile());
			}
		}
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testListFolderWithSlashes() throws Exception {
		wipeRoot();
		String folder = "jakeAtestFolder";
		recursiveDelete(new File(fss.getRootPath() + File.separator + folder));

		List<String> s;
		s = fss.listFolder("///" + folder);
		Assert.assertTrue(s.isEmpty());
		s = fss.listFolder("/");
		Assert.assertTrue(s.isEmpty());
	}

	@Test(timeout = 10000)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testModificationListener() throws Exception {
		wipeRoot();
		fss.setRootPath(new ProjectDir(mytempdir));
		final String file = "myfile";
		File f = new File(fss.getRootPath(), file);
		final Semaphore s = new Semaphore(0);

		IFileModificationListener l = new IFileModificationListener() {

			@Override
			public void fileModified(String relpath, ModifyActions action) {
				log.debug(relpath + " " + action);
				if (relpath.equals(file)
						&& action.equals(ModifyActions.CREATED))
					s.release();
				else
					throw new IllegalStateException("unexpected call");
			}
		};
		fss.addModificationListener(l);
		FileOutputStream fos = new FileOutputStream(f);
		fos.write("foo".getBytes());
		fos.close();
		s.acquire();
		// Assert.assertTrue(s.tryAcquire(5, TimeUnit.SECONDS));
		Assert.assertEquals(
				fss.calculateHashOverFile(file).toString(),
				"f7fbba6e0636f890e56fbbf3283e524c6fa3204ae298382d624741d0dc6638326e282c41be5e4254d8820772c5518a2c5a8c0c7f7eda19594a7eb539453e1ed7");
		fss.removeModificationListener(l);

		// so it is there, now modify it.

		l = new IFileModificationListener() {

			@Override
			public void fileModified(String relpath, ModifyActions action) {
				log.debug(relpath + " " + action);
				if (relpath.equals(file)
						&& action.equals(ModifyActions.MODIFIED))
					s.release();
				else
					throw new IllegalStateException("unexpected call");
			}
		};
		fss.addModificationListener(l);
		Thread.sleep(1000);
		fos = new FileOutputStream(f, true);
		fos.write("bar".getBytes());
		fos.close();
		s.acquire();
		// Assert.assertTrue(s.tryAcquire(3, TimeUnit.SECONDS));
		fss.removeModificationListener(l);
		Assert.assertEquals(
				fss.calculateHashOverFile(file).toString(),
				"0a50261ebd1a390fed2bf326f2673c145582a6342d523204973d0219337f81616a8069b012587cf5635f6925f1b56c360230c19b273500ee013e030601bf2425");

		// so lets delete it now.
		l = new IFileModificationListener() {

			@Override
			public void fileModified(String relpath, ModifyActions action) {
				log.debug(relpath + " " + action);
				if (relpath.equals(file)
						&& action.equals(ModifyActions.DELETED))
					s.release();
				else
					throw new IllegalStateException("unexpected call");
			}
		};
		fss.addModificationListener(l);
		new File(fss.getRootPath(), file).delete();
		s.acquire();
		// Assert.assertTrue(s.tryAcquire(3, TimeUnit.SECONDS));
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testDeleteFolder() throws Exception {
		wipeRoot();
		recursiveDelete(fss.getRootPath());

		String[] content = { "B", "C", "B/foo", "D", "F", "G", "H", "J",
				"B/foo/bar", "C/foo" };
		createFiles("", content);
		Assert.assertFalse(fss.recursiveListFiles().isEmpty());
		Assert.assertEquals(fss.recursiveListFiles().size(), content.length - 4);
		Assert.assertTrue(fss.deleteFolder("B"));
		Assert.assertEquals(fss.recursiveListFiles().size(), content.length - 5);
		Assert.assertTrue(fss.deleteFolder("/"));
		Assert.assertTrue(fss.recursiveListFiles().isEmpty());
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testRecursiveListFolder() throws Exception {
		wipeRoot();
		recursiveDelete(fss.getRootPath());

		String[] content = { "B", "C", "B/foo", "D", "F", "G", "H", "J",
				"B/foo/bar", "C/foo" };
		createFiles("", content);

		List<String> s = fss.recursiveListFiles();

		for (int j = 0; j < content.length; j++) {
			boolean found = false;
			for (String value : s) {
				if (value.equals(content[j])) {
					found = true;
					break;
				}
			}
			if (j < 4) {
				Assert.assertFalse("We don't expect directory " + content[j],
						found);
			} else {
				Assert.assertTrue(found);
				Assert.assertTrue("file: " + content[j],
						fss.fileExists(content[j]));
			}
		}

		Assert.assertTrue(s.size() == content.length - 4);
		wipeRoot();
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testImportFile() throws Exception {
		wipeRoot();
		File sourcedir = File.createTempFile("fstest", "source.txt");
		sourcedir.delete();
		sourcedir.mkdir();
		sourcedir.deleteOnExit();
		File source = new File(sourcedir, "source");
		String text = "foobar";
		FileOutputStream fos = new FileOutputStream(source);
		fos.write(text.getBytes());
		fos.close();
		source.deleteOnExit();
		fss.importFile(sourcedir, "/");
		Assert.assertEquals(
				fss.calculateHashOverFile(
						sourcedir.getName() + "/" + source.getName())
						.toString(),
				"0a50261ebd1a390fed2bf326f2673c145582a6342d523204973d0219337f81616a8069b012587cf5635f6925f1b56c360230c19b273500ee013e030601bf2425");
		Assert.assertTrue(source.delete());
		Assert.assertTrue(sourcedir.delete());
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testMoveAndCopyFile() throws Exception {
		wipeRoot();
		String text = "foobar";
		long before = System.currentTimeMillis();
		Assert.assertFalse(fss.fileExists("source"));
		FileOutputStream fos = new FileOutputStream(new File(fss.getRootPath(),
				"source"));
		fos.write(text.getBytes());
		fos.close();

		Assert.assertTrue(fss.fileExists("source"));
		Assert.assertFalse(fss.fileExists("bar/intermediate/baz"));
		Assert.assertTrue(fss.moveFile("source", "bar/intermediate/baz"));
		Assert.assertFalse(fss.fileExists("source"));
		Assert.assertTrue(fss.fileExists("bar/intermediate/baz"));
		Assert.assertFalse(fss.fileExists("foo/target"));

		Assert.assertTrue(fss.copyFile("bar/intermediate/baz", "foo/target"));
		Assert.assertTrue(fss.fileExists("bar/intermediate/baz"));
		Assert.assertTrue(fss.fileExists("foo/target"));
		LineNumberReader fis = new LineNumberReader(new FileReader(new File(
				fss.getRootPath(), "foo/target")));
		Assert.assertEquals(fis.readLine(), text);

		long after = System.currentTimeMillis();
		long actual = fss.getLastModified("foo/target");
		Assert.assertTrue("actual modification time " + actual
				+ " should be >= before " + before, actual >= before - 10000);
		Assert.assertTrue("actual modification time " + actual
				+ " should be <= after" + after, actual <= after + 10000);

		Assert.assertEquals(fss.getFileName("bar/intermediate/baz"), "baz");
	}

	@Test(expected = NotAFileException.class)
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testCopyNonExistantFile() throws Exception {
		wipeRoot();
		fss.copyFile("source", "foo/target");
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testWriteFile() throws Exception {
		wipeRoot();
		try {
			fss.writeFile("ran:dom", null);
			Assert.fail("InvalidFilenameException");
		} catch (InvalidFilenameException e) {
		}
		{
			fss.writeFile("random", new byte[] {});
			Assert.assertEquals(fss.readFile("random").length, 0);
		}
		{
			String content = "Foo bar\nbaz";
			fss.writeFile("random", content.getBytes());
			Assert.assertEquals(new String(fss.readFile("random")), content);
		}
		{
			String content = "Foo\u00c3\u00a4\u00c3\u00b6p\u00c3\u00a4\u00c3\u00bc\u00c3\u00b6 "
					+ "bar\nbau\u00e2\u0082\u00ac@\u00c4\u00b1\u00ce\u00b4\u00c3\u00be\u00c3"
					+ "\u00a6'\u00c5\u0093\u00c5\u0093\u00c3\u00a6'\u00c5\u0093\u00c3\u00b8"
					+ "\u00e2\u0082\u00ac@\u00c4\u00b1z";
			fss.writeFile("random", content.getBytes("utf8"));
			Assert.assertEquals(new String(fss.readFile("random"), "utf8"),
					content);
		}
		fss.writeFile("foo", new byte[] { 12 });
		try {
			fss.writeFile("foo/random", new byte[] { 12, 23 });
			Assert.fail("CreatingSubDirectoriesFailedException");
		} catch (CreatingSubDirectoriesFailedException e) {
		}

		{
			fss.writeFile("bar/baz/random", "Foobar".getBytes());
			Assert.assertTrue("recursice create", fss.folderExists("bar")
					&& fss.folderExists("bar/baz"));
			Assert.assertTrue("recursice create",
					fss.fileExists("bar/baz/random"));
			Assert.assertEquals("recursice create", "Foobar",
					new String(fss.readFile("bar/baz/random")));
		}

	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testDeleteFile() throws Exception {
		wipeRoot();
		{
			Assert.assertFalse(fss.fileExists("bar/baz/random")
					&& fss.folderExists("bar/baz") && fss.folderExists("bar"));

			fss.writeFile("bar/baz/random", "Foobar".getBytes());

			Assert.assertTrue(fss.fileExists("bar/baz/random")
					&& fss.folderExists("bar/baz") && fss.folderExists("bar"));

			Assert.assertTrue(fss.deleteFile("bar/baz/random"));

			Assert.assertFalse(fss.fileExists("bar/baz/random")
					&& fss.folderExists("bar/baz") && fss.folderExists("bar"));
		}
		wipeRoot();
		{
			fss.writeFile("bar/baz/random", "Foobar".getBytes());
			try {
				fss.deleteFile("bar/baz/random2");
				Assert.fail("FileNotFoundException");
			} catch (FileNotFoundException e) {
			}
			try {
				fss.deleteFile("bar/baz/");
				Assert.fail("NotAFileException");
			} catch (NotAFileException e) {
			}
			Assert.assertTrue(fss.fileExists("bar/baz/random"));
			fss.writeFile("bar/baz/random2", "Foobar".getBytes());
			Assert.assertTrue(fss.deleteFile("bar/baz/random"));
			Assert.assertFalse(fss.fileExists("bar/baz/random"));
			Assert.assertTrue(fss.folderExists("bar/baz")
					&& fss.folderExists("bar"));
			Assert.assertTrue(fss.fileExists("bar/baz/random2"));
		}
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testDeleteFile_AutoDeleteDir() throws Exception {
		wipeRoot();
		String relpath = "myNewDir/random";
		String dir = "myNewDir";

		Assert.assertFalse(fss.fileExists(relpath));
		Assert.assertFalse(fss.folderExists(dir));

		fss.writeFile(relpath, "Foobar".getBytes());

		Assert.assertTrue(fss.fileExists(relpath));
		Assert.assertTrue(fss.folderExists(dir));
		System.gc();
		Thread.yield();
		System.gc();
		Assert.assertTrue(fss.deleteFile(relpath));

		Assert.assertFalse(fss.fileExists(relpath));
		Assert.assertFalse(fss.folderExists(dir));
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testHashFile() throws Exception {
		fss.writeFile("bar/baz/random", "Foobar".getBytes());

		Assert.assertEquals(
				fss.calculateHashOverFile("bar/baz/random"),
				"cead1f59a9a0d22e46a28f943a662338dd758d6dce38f7ea6ab13b6615c312b69fffff049781c169b597577cb5566d5d1354364ac032a9d4d5bd8ef833340061");

	}

	/**
	 * Tests that no exceptions are thrown. No application is launched, since
	 * awt/swing is not started
	 * 
	 * @throws Exception
	 */
	@Test
	@Prerequisite(checker = ThrowStuffInMyFaceChecker.class)
	public void launchTest() throws Exception {
		fss.writeFile("launch1.txt", "Foobar".getBytes());
		fss.writeFile("launch2.html",
				"<html><body><h1>Woot!</h1></body></html>".getBytes());
		Thread.yield();
		fss.launchFile("launch1.txt");
		fss.launchFile("launch2.html");
	}

	@Test
	@Prerequisite(checker = DesktopSupportedChecker.class)
	public void testFileSize() throws Exception {
		fss.writeFile("launch1.txt", "Foobar".getBytes());
		fss.writeFile("launch2.html",
				"<html><body><h1>Woot!</h1></body></html>".getBytes());
		Assert.assertEquals(fss.getFileSize("launch2.html"),
				fss.readFile("launch2.html").length);
		Assert.assertEquals(fss.getFileSize("launch1.txt"),
				fss.readFile("launch1.txt").length);
		try {
			fss.getFileSize("does/not/exist.txt");
			Assert.fail();
		} catch (FileNotFoundException e) {
		}
		fss.writeFile("foo/bar.txt", "Foobar".getBytes());
		try {
			fss.getFileSize("foo");
			Assert.fail();
		} catch (NotAFileException e) {
		}

	}

	/* TODO: launchFile registerModificationListener */
}
