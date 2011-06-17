package com.jakeapp.jake.fss;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.jakeapp.jake.fss.exceptions.CreatingSubDirectoriesFailedException;
import com.jakeapp.jake.fss.exceptions.FileAlreadyExistsException;
import com.jakeapp.jake.fss.exceptions.FileTooLargeException;
import com.jakeapp.jake.fss.exceptions.InvalidFilenameException;
import com.jakeapp.jake.fss.exceptions.LaunchException;
import com.jakeapp.jake.fss.exceptions.NotADirectoryException;
import com.jakeapp.jake.fss.exceptions.NotAFileException;
import com.jakeapp.jake.fss.exceptions.NotAReadableFileException;

/**
 * Implementation of {@link IFSService}
 * 
 * @author johannes
 * @see IFSService
 */
public class FSService implements IFSService, IModificationListener {
	private static Logger log = Logger.getLogger(FSService.class);

	private ProjectDir rootPath;

	private StreamFileHashCalculator hasher;

	private FolderWatcher fw;

	private FileLauncher launcher;

	private Set<IFileModificationListener> modificationListener =
		new HashSet<IFileModificationListener>();

	public FSService() throws NoSuchAlgorithmException {
		hasher = new StreamFileHashCalculator();
		launcher = new FileLauncher();
	}

	public ProjectDir getRootPath() {
		return rootPath;
	}

	public void unsetRootPath() {
		if (fw != null) {
			fw.cancel();
			fw.removeListener(this);
		}
		modificationListener.clear();
	}

	public void setRootPath(ProjectDir path) throws FileNotFoundException,
		NotADirectoryException {
		unsetRootPath();
		checkFileExists(path);
		if (!path.isDirectory())
			throw new NotADirectoryException();
		rootPath = path;

		try {
			fw = new FolderWatcher(this.rootPath, 700);
		} catch (NoSuchAlgorithmException e) {
			/* won't happen as we use the same algorithm here and it loaded. */
		}
		fw.initialRun();
		fw.addListener(this);
		fw.run();
	}

	public void addModificationListener(IFileModificationListener l) {
		synchronized (modificationListener) {
			modificationListener.add(l);
		}
	}

	public void removeModificationListener(IFileModificationListener l) {
		synchronized (modificationListener) {
			modificationListener.remove(l);
		}
	}

	public Boolean fileExists(String relpath) throws InvalidFilenameException {
		File f = convertToAbsPath(relpath);
		return f.exists() && f.isFile();
	}

	public List<String> listFolder(String inrelpath)
		throws InvalidFilenameException {
		String relpath = inrelpath;
		while (relpath.startsWith("/")) {
			relpath = relpath.substring(1);
		}
		File f = convertToAbsPath("/" + relpath);
		List<String> list = new ArrayList<String>();
		for (String file : FileUtils.listMinusA(f)) {
			if (!relpath.equals(""))
				file = relpath + '/' + file;

			if (isValidRelpath(file))
				list.add(file);
		}
		return list;
	}

	public List<String> recursiveListFiles() throws IOException {
		List<String> list = new ArrayList<String>();
		List<String> dirlist;
		try {
			dirlist = listFolder("");
		} catch (InvalidFilenameException e) {
			throw new IOException(e);
		}
		for (int i = 0; i < dirlist.size(); i++) {
			String f = dirlist.get(i);
			try {
				if (folderExists(f)) {
					dirlist.addAll(listFolder(f));
				} else if (fileExists(f)) {
					list.add(f);
				}
			} catch (InvalidFilenameException e) {
				// won't happen
			}
		}
		return list;
	}

	@Deprecated
	public byte[] readFile(String relpath) throws InvalidFilenameException,
		FileNotFoundException, NotAReadableFileException {

		File f = convertToAbsPath(relpath);
		checkFileExists(f);
		checkIsFile(f);
		if (f.length() > Integer.MAX_VALUE)
			throw new FileTooLargeException();

		FileInputStream fr = null;
		try {
			fr = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			/*
			 * This is thrown if permissions wrong. we already know the file
			 * exists.
			 */
			throw new NotAReadableFileException();
		}

		int len = (int) f.length();
		byte[] buf = new byte[len];
		int n;
		try {
			n = fr.read(buf, 0, len);
			fr.close();
		} catch (IOException e) {
			throw new NotAReadableFileException();
		}
		if (len > n)
			throw new NotAReadableFileException();
		return buf;
	}

	public InputStream readFileStream(String relpath)
		throws InvalidFilenameException, FileNotFoundException,
		NotAReadableFileException {
		return readFileStreamAbs(convertToAbsPath(relpath));
	}

	public static InputStream readFileStreamAbs(File f)
		throws FileNotFoundException, NotAReadableFileException {
		checkFileExists(f);
		checkIsFile(f);

		FileInputStream fr = null;
		try {
			fr = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			/*
			 * This is thrown if permissions wrong. we already know the file
			 * exists.
			 */
			throw new NotAReadableFileException();
		}
		return fr;
	}

	private File convertToAbsPath(String relpath)
		throws InvalidFilenameException {
		if (!isValidRelpath(relpath))
			throw new InvalidFilenameException("File " + relpath
				+ " is not a valid filename!");
		return new File(getRootPath(), relpath);
	}

	public String getFullpath(String relpath) throws InvalidFilenameException {
		if (getRootPath() == null)
			return null;
		String fp = convertToAbsPath(relpath).getAbsolutePath();
		while (fp.endsWith("/"))
			fp = fp.substring(0, fp.length() - 1);
		return fp;
	}

	public String joinPath(String parentPath, String inrelpath) {
		String relpath;
		if ('/' != File.separatorChar)
			relpath = inrelpath.replace('/', File.separatorChar);
		else
			relpath = inrelpath;
		String p = parentPath + File.separator + relpath;
		if (File.separatorChar == '\\') {
			p = p.replaceAll("\\\\\\\\", "\\\\");
		} else {
			p = p.replaceAll(File.separator + File.separator, File.separator);
		}
		return p;
	}

	@Deprecated
	public void writeFile(String relpath, byte[] content)
		throws InvalidFilenameException, IOException, FileTooLargeException,
		NotAFileException, CreatingSubDirectoriesFailedException {
		String filename = getFullpath(relpath);
		File f = new File(filename);

		if (f.exists() && !f.isFile())
			throw new NotAFileException();
		if (content.length > Integer.MAX_VALUE)
			throw new FileTooLargeException();
		if (f.getParentFile().exists()) {
			if (!f.getParentFile().isDirectory())
				throw new CreatingSubDirectoriesFailedException();
		} else {
			if (!f.getParentFile().mkdirs())
				throw new CreatingSubDirectoriesFailedException();
		}

		FileOutputStream fr = new FileOutputStream(filename);
		fr.write(content);
		fr.close();
		fr = null;
		System.gc();
	}

	public void writeFileStream(String relpath, InputStream source)
		throws InvalidFilenameException, NotAFileException,
		CreatingSubDirectoriesFailedException, IOException {
		writeFileStreamAbs(convertToAbsPath(relpath), source);
	}

	public static void writeFileStreamAbs(File f, InputStream source)
		throws NotAFileException, CreatingSubDirectoriesFailedException,
		IOException {
		if (f.exists() && !f.isFile())
			throw new NotAFileException();
		if (f.getParentFile().exists()) {
			if (!f.getParentFile().isDirectory())
				throw new CreatingSubDirectoriesFailedException();
		} else {
			if (!f.getParentFile().mkdirs())
				throw new CreatingSubDirectoriesFailedException();
		}

		FileOutputStream destination = null;
		destination = new FileOutputStream(f);

		byte[] buf = new byte[1024];
		int len;
		while ((len = source.read(buf)) > 0) {
			destination.write(buf, 0, len);
		}
		if (destination != null) {
			destination.close();
		}
		if (source != null) {
			source.close();
		}
	}

	public Boolean folderExists(String relpath)
		throws InvalidFilenameException, IOException {
		File f = convertToAbsPath(relpath);
		return f.exists() && f.isDirectory();
	}

	public String getTempDir() throws IOException {
		return System.getProperty("java.io.tmpdir");
	}

	public String getTempFile() throws IOException {
		File f = File.createTempFile("jake", "");
		return f.getAbsolutePath();
	}

	public Boolean isValidRelpath(String relpath) {
		String regex = "[A-Z a-z0-9\\-+_./\\(\\)]+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(relpath);

		if (!(m.find() && m.start() == 0 && m.end() == relpath.length())) {
			return false;
		}
		return !(relpath.contains("/../") || relpath.startsWith("../")
			|| relpath.endsWith("/..") || relpath.equals(".."));
	}

	public boolean deleteFile(String relpath) throws InvalidFilenameException,
		FileNotFoundException, NotAFileException {
		File f = convertToAbsPath(relpath);
		log.info("Delete File: " + f);
		checkFileExists(f);
		checkIsFile(f);
		if (!f.delete())
			return false;

		/* TODO: Check if this is a infinite loop on a empty drive on windows */
		deleteEmptyFolderRecursive(f.getParentFile());

		return true;
	}

	private void deleteEmptyFolderRecursive(File f) {
		if (!f.isDirectory())
			return;
		if (!(f.getAbsolutePath().startsWith(getRootPath().getAbsolutePath()) && f
			.getAbsolutePath().length() > getRootPath().getAbsolutePath()
			.length()))
			return;
		if (FileUtils.listFilesMinusA(f).iterator().hasNext())
			return;
		f.delete();

		deleteEmptyFolderRecursive(f.getParentFile());
	}

	public boolean deleteFolder(String relpath)
		throws InvalidFilenameException, FileNotFoundException,
		NotADirectoryException {
		File f = convertToAbsPath(relpath);
		log.info("Delete Folder: " + f);
		checkFileExists(f);
		if (!f.isDirectory())
			throw new NotADirectoryException();

		for (String children : FileUtils.listMinusA(f)) {
			File childFile = new File(f, children);
			String relpathchild = relpath + "/" + children;
			boolean success = false;
			if (childFile.isDirectory()) {
				success = deleteFolder(relpathchild);
			} else {
				try {
					success = deleteFile(relpathchild);
				} catch (NotAFileException e) {
					log.warn(
						"Error deleting file: " + childFile.getAbsolutePath(),
						e);
				}
			}
			if (!success) {
				return false;
			}
		}

		deleteEmptyFolderRecursive(f);

		return true;
	}

	@Override
	public boolean trashFile(String relativePath)
		throws InvalidFilenameException, FileNotFoundException {
		try {
			// TODO implement via a REAL trash...
			return deleteFile(relativePath);
		} catch (NotAFileException e) {
			log.warn("trash failed for " + relativePath, e);
			// silently discarded - the file is not there so it is already
			// deleted
		}

		return false;
	}

	@Override
	public boolean trashFolder(String relativePath)
		throws InvalidFilenameException, FileNotFoundException {
		try {
			// TODO implement via a REAL trash...
			return deleteFolder(relativePath);
		} catch (NotADirectoryException e) {
			log.warn("trash failed for " + relativePath, e);
		}

		return false;
	}

	@Override
	public boolean copyFile(String from, String to)
		throws InvalidFilenameException, NotAReadableFileException,
		FileAlreadyExistsException, IOException,
		CreatingSubDirectoriesFailedException {
		File fileFrom, fileTo;

		checkIsValidRelPath(to);

		fileFrom = convertToAbsPath(from);
		fileTo = convertToAbsPath(to);

		return copyFileInternal(fileFrom, fileTo);
	}

	/**
	 * This methode does the actual copying, without the rel/abs path whobbling
	 * 
	 * @param fileFrom
	 * @param fileTo
	 * @return
	 * @throws NotAReadableFileException
	 * @throws FileAlreadyExistsException
	 * @throws InvalidFilenameException
	 * @throws CreatingSubDirectoriesFailedException
	 * 
	 * @throws IOException
	 */
	private boolean copyFileInternal(File fileFrom, File fileTo)
		throws NotAReadableFileException, FileAlreadyExistsException,
		InvalidFilenameException, CreatingSubDirectoriesFailedException,
		IOException {
		checkFileIsReadable(fileFrom);
		checkFileNotExists(fileTo);

		// TODO this should be atomic
		FSService.writeFileStreamAbs(fileTo, readFileStreamAbs(fileFrom));

		return true;
	}

	@Override
	public boolean moveFile(String from, String to)
		throws InvalidFilenameException, NotAReadableFileException,
		FileAlreadyExistsException, IOException,
		CreatingSubDirectoriesFailedException {
		File fileFrom, fileTo;
		boolean result = true;

		checkIsValidRelPath(to);

		fileFrom = convertToAbsPath(from);
		fileTo = convertToAbsPath(to);

		checkFileIsReadable(fileFrom);
		checkFileNotExists(fileTo);

		if (!fileFrom.renameTo(fileTo)) {
			// FALLBACK SOLUTION FOR MOVE - copy the file and remove it

			// TODO this should be atomic
			this.copyFile(from, to);
			this.deleteFile(from);
		}

		return result;
	}

	public void launchFile(String relpath) throws InvalidFilenameException,
		LaunchException {
		launcher.launchFile(convertToAbsPath(relpath));
	}

	public long getFileSize(String relpath) throws InvalidFilenameException,
		FileNotFoundException, NotAFileException {
		File f = convertToAbsPath(relpath);
		if (!f.exists())
			throw new FileNotFoundException("Not found: " + relpath);
		if (!f.isFile())
			throw new NotAFileException(relpath);
		return f.length();
	}

	@Deprecated
	public HashValue calculateHash(byte[] bytes) {
		return this.hasher.calculateHash(new ByteArrayInputStream(bytes));
	}

	public HashValue calculateHash(InputStream stream) {
		return this.hasher.calculateHash(stream);
	}

	public HashValue calculateHashOverFile(String relpath)
		throws InvalidFilenameException, NotAReadableFileException,
		FileNotFoundException {
		return this.hasher.calculateHash(readFileStream(relpath));
	}

	public int getHashLength() {
		return HashValue.getStringLength();
	}

	public long getLastModified(String relpath)
		throws InvalidFilenameException, NotAFileException {
		if (!fileExists(relpath))
			throw new NotAFileException();
		return convertToAbsPath(relpath).lastModified();
	}

	public void fileModified(File f, ModifyActions action) {
		if (rootPath == null)
			return;

		String relpath =
			f.getAbsolutePath().replace(rootPath + File.separator, "")
				.replace(File.separatorChar, '/');
		synchronized (modificationListener) {
			for (IFileModificationListener l : modificationListener) {
				l.fileModified(relpath, action);
			}
		}
	}

	@Override
	public String getFileName(String relpath) throws InvalidFilenameException {
		return convertToAbsPath(relpath).getName();
	}

	@Override
	public void createFolder(String relpath) throws InvalidFilenameException,
		IOException {
		File f = convertToAbsPath(relpath);
		if (f.exists())
			throw new IOException();
		if (!f.mkdirs())
			throw new IOException();
	}

	@Override
	public void importFile(File file, String destFolderRelPath)
		throws IOException, InvalidFilenameException,
		NotAReadableFileException, FileAlreadyExistsException,
		CreatingSubDirectoriesFailedException {
		log.debug("importing file: " + file + " into " + destFolderRelPath);
		checkFileExists(file);
		checkIsValidRelPath(destFolderRelPath);

		if (file.isFile()) {
			File dest =
				new File(convertToAbsPath(destFolderRelPath), file.getName());

			// TODO: save a list of success/fails and return to user
			try {
				importFileInternal(file, dest);
			} catch (Exception ex) {
				log.warn("Failed importing " + file, ex);
			}

		} else if (file.isDirectory()) {
			// woohoo - copy whole directory and all subdirs within!
			for (File afile : file.listFiles()) {
				importFile(afile, destFolderRelPath + "/" + file.getName());
			}
		}
	}

	/**
	 * Internal helper that performs the copying
	 * 
	 * @param fileFrom
	 * @param fileTo
	 * @throws NotAReadableFileException
	 * @throws FileAlreadyExistsException
	 * @throws InvalidFilenameException
	 * @throws IOException
	 * @throws CreatingSubDirectoriesFailedException
	 * 
	 */
	private void importFileInternal(File fileFrom, File fileTo)
		throws NotAReadableFileException, FileAlreadyExistsException,
		InvalidFilenameException, IOException,
		CreatingSubDirectoriesFailedException {
		checkFileIsReadable(fileFrom);
		checkFileNotExists(fileTo);

		// TODO this should be atomic
		writeFileStreamAbs(fileTo, readFileStreamAbs(fileFrom));
	}

	private static void checkIsFile(File f) throws NotAFileException {
		if (!f.isFile())
			throw new NotAFileException("Not a file: " + f.getAbsolutePath());
	}

	private static void checkFileNotExists(File f)
		throws FileAlreadyExistsException {
		if (f.exists()) {
			throw new FileAlreadyExistsException("File already exists: "
				+ f.getAbsolutePath());
		}
	}

	private static void checkFileExists(File f) throws FileNotFoundException {
		if (!f.exists())
			throw new FileNotFoundException("File not found: "
				+ f.getAbsolutePath());
	}

	private static void checkFileIsReadable(File f)
		throws NotAReadableFileException {
		if (!f.exists()) {
			throw new NotAFileException("Not a file: " + f);
		}

		if (!f.canRead()) {
			throw new NotAReadableFileException("Not readable: " + f);
		}

		checkIsFile(f);
	}

	private void checkIsValidRelPath(String to) throws InvalidFilenameException {
		if (!this.isValidRelpath(to)) {
			throw new InvalidFilenameException(to);
		}
	}
}
