package com.doublesignal.sepm.jake.fss;

import com.doublesignal.sepm.jake.fss.exceptions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * The file system service ought to provide a operating system independent way 
 * of read/write and watch operations.
 * 
 * 
 * <p>rootpath: The project root directory. Has to be set first.</p> 
 * 
 * <p>relpath:  A relative path starting from the rootpath of a file or folder. 
 *           It may only contain characters supported by common operating 
 *           systems (@see <code>isValidRelpath</code>).
 *           relpaths contain slashes as path seperators. 
 *           NOTE: For the real file access, the FSService has to convert them 
 *           to the OS-specific way
 * </p>
 * 
 * @author johannes
 **/

public interface IFSService {
	/**
	 * Checks that the file exists and that it is a regular file (no link, 
	 * device, pipe, ...)
	 * @return wether the file exists
	 */
	public Boolean fileExists(String relpath) 
		throws InvalidFilenameException, IOException;
	
	/**
	 * Checks that the folder exists and that it is a folder
	 * @return wether the folder exists
	 * @throws InvalidFilenameException
	 * @throws IOException
	 */
	public Boolean folderExists(String relpath) 
		throws InvalidFilenameException, IOException;
	
	@Deprecated
	/**
	 * Joins the rootpath with the relpath. The absolute filename is converted 
	 * to the right path seperator.
	 * @return the absolute path for the relpath
	 * @throws InvalidFilenameException 
	 */
	public String getFullpath(String relpath) throws InvalidFilenameException;
	
	/**
	 * @return the rootpath set previously by <code>setRootPath</code>
	 *         if no valid rootpath was set, null is returned.
	 */
	public String getRootPath();
	
	/**
	 * Checks wether the relpath contains characters 
	 * acceptable for various operating systems and file systems
	 * 
	 * It matches for: [A-Z a-z0-9\-+_./\(\)]+ and checks that no /../ can be 
	 * applied (which could reference outside the rootpath)  
	 */
	public Boolean isValidRelpath(String relpath);
	
	/**
	 * Concatinates the parentpath and the subpath together and
	 * converts to the right path seperator
	 * @return a absolute path usable to the OS
	 */
	public String joinPath(String parentpath, String subpath);
	
	/**
	 * Launches the associated application and returns (i.e. does not wait for 
	 * termination) 
	 * @param relpath the file to be edited/viewed
	 * @throws InvalidFilenameException
	 * @throws LaunchException
	 * @throws IOException 
	 */
	public void launchFile(String relpath) 
		throws InvalidFilenameException, LaunchException, IOException;
	
	/**
	 * Lists folder content following isValidRelpath
	 * @param relpath Folder to be viewed
	 * @return directory content: file and folder names as relpaths
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.InvalidFilenameException
	 * @throws IOException
	 */
	public List<String> listFolder(String relpath) 
		throws InvalidFilenameException, IOException;
	
	/**
	 * Lists all files in rootpath following isValidRelpath
	 * @return directory content: files as relpaths
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.InvalidFilenameException
	 * @throws IOException
	 */
	public List<String> recursiveListFiles() 
		throws InvalidFilenameException, IOException;
	
	/**
	 * Reads the full content of a given file into a String
	 * @return content of the file
	 * @throws InvalidFilenameException
	 * @throws FileNotFoundException
	 * @throws NotAReadableFileException
	 * @throws NotAFileException
	 */
	public byte[] readFile(String relpath) 
		throws InvalidFilenameException, NotAFileException, 
			FileNotFoundException, NotAReadableFileException;
	
	/**
	 * Sets and stores the root path for operations that use a relpath.
	 * @throws IOException
	 * @throws NotADirectoryException 
	 */
	public void setRootPath(String path) 
		throws IOException, NotADirectoryException;
	/**
	 * Unsets the root path (e.g. stops listeners)
	 * @throws IOException
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.NotADirectoryException
	 */
	public void unsetRootPath();

	/**
	 * Writes the content to the file.
	 * Creates subdirectories, if needed.
	 * @param content The full, new file content as a String
	 * @throws InvalidFilenameException
	 * @throws IOException
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.NotAFileException
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.FileTooLargeException
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.CreatingSubDirectoriesFailedException
	 */
	public void writeFile(String relpath, byte[] content) 
		throws InvalidFilenameException, IOException, FileTooLargeException,
			NotAFileException, CreatingSubDirectoriesFailedException;
	
	
	/**
	 * Gets the operating system preferred temporary directory
	 * It is deleted eventually by the operating system after program 
	 * termination 
	 * @return a temporary directory
	 * @throws IOException
	 */
	public String getTempDir() 
		throws IOException;
	
	/**
	 * Get the path to a new temporary file that can be written to.
	 * The file resides in a temporary directory.
	 * It is deleted eventually by the operating system after program 
	 * termination 
	 * @return the path
	 * @throws IOException
	 */
	public String getTempFile()
		throws IOException;
	
	/**
	 * Deletes the file and recursively removes parent folders if they are empty
	 * @return wether the delete was successful
	 * @throws InvalidFilenameException 
	 * @throws FileNotFoundException 
	 * @throws NotAFileException 
	 */
	public boolean deleteFile(String relpath) throws InvalidFilenameException, FileNotFoundException, NotAFileException;
	
	
	/**
	 * @param relpath
	 * @return the hash over the file
	 * @throws InvalidFilenameException
	 * @throws NotAReadableFileException
	 * @throws FileNotFoundException 
	 */
	String calculateHashOverFile(String relpath) 
		throws InvalidFilenameException, NotAReadableFileException, FileNotFoundException;

	/**
	 * @param bytes
	 * @return hash over the bytes
	 */
	String calculateHash(byte[] bytes);
	
	/**
	 * @return length of the returning String of the implemented Hash operation
	 */
	int getHashLength();
	
	/**
	 * @param relpath
	 * @return size of the file in Bytes
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.InvalidFilenameException
	 * @throws FileNotFoundException
	 * @throws NotAFileException
	 */
	long getFileSize(String relpath)
		throws InvalidFilenameException, FileNotFoundException, NotAFileException; 


	/**
	 * Registers a callback for watching the rootpath.
	 * Events are create, modify, delete for files. 
	 * It is recursive and when a folder is created, the newly created folder is
	 * watched too. When a folder is removed a delete-Callback is issued for 
	 * each file.
	 * @see IProjectModificationListener
	 */
	public void addModificationListener(IProjectModificationListener ob);

	/**
	 * Removes a callback for watching the rootpath.
	 * @see IProjectModificationListener
	 */
	public void removeModificationListener(IProjectModificationListener ob);
	
	/**
	 * get the last modified date for a file
	 * @throws InvalidFilenameException 
	 * @throws com.doublesignal.sepm.jake.fss.exceptions.NotAFileException
	 */
	public long getLastModified(String relpath) 
		throws InvalidFilenameException, NotAFileException;
	
}
