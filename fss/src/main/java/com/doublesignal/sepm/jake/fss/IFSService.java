package com.doublesignal.sepm.jake.fss;

import java.io.IOException;

/**
 * The file system service ought to provide a operating system independent way 
 * of read/write and watch operations.
 * 
 * @author johannes
 * 
 * rootpath: The project root directory. Has to be set first.
 * relpath:  A relative path starting from the rootpath of a file or folder. 
 *           It may only contain characters supported by common operating 
 *           systems.
 *           relpaths contain slashes as path seperators. 
 *           NOTE: For the real file access, the FSService has to convert them 
 *           to the OS-specific way
 * 
 **/

public interface IFSService {
	/* TODO: Maybe we should return some streaming thingy in readFile() and 
	 *       writeFile()? */
	
	/**
	 * @param relpath
	 * @return the hash over the file as a string
	 * @throws InvalidFilenameException
	 * @throws IOException
	 */
	String calculateHash(String relpath) 
		throws InvalidFilenameException, IOException;
	
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
	
	/**
	 * @return joins the rootpath with the relpath and
	 * converts to the right path seperator
	 */
	public String getFullpath(String relpath);
	
	/**
	 * @return the rootpath set previously by SetRootRule
	 */
	public String getRootPath();
	
	/**
	 * Checks wether the relpath contains characters 
	 * acceptable for various operating systems and file systems
	 * 
	 * These are printable ascii characters: [A-Za-z0-9\-_.]+
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
	 */
	public void launchFile(String relpath) 
		throws InvalidFilenameException, LaunchException;
	
	/**
	 * Lists folder content following isValidRelpath
	 * @param relpath Folder to be viewed
	 * @return directory content: file and folder names
	 * @throws InvalidFilenameException
	 * @throws IOException
	 */
	public String[] listFolder(String relpath) 
		throws InvalidFilenameException, IOException;
	
	/**
	 * Reads the full content of a given file into a String
	 * @return content of the file
	 * @throws InvalidFilenameException
	 * @throws IOException
	 */
	public String readFile(String relpath) 
		throws InvalidFilenameException, IOException;
	
	/**
	 * Registers a callback for watching the rootpath.
	 * Events are create, modify, delete for files. 
	 * It is recursive and when a folder is created, the newly created folder is
	 * watched too. When a folder is removed a delete-Callback is issued for 
	 * each file.
	 * @see ModificationListener
	 */
	public void registerModificationListener(ModificationListener ob);
	
	/**
	 * Sets and stores the root path for operations that use a relpath.
	 * @throws InvalidFilenameException
	 * @throws IOException
	 */
	public void setRootPath(String path) 
		throws InvalidFilenameException, IOException;

	/**
	 * Writes the content to the file.
	 * Creates subdirectories, if needed.
	 * @param content The full, new file content as a String
	 * @throws InvalidFilenameException
	 * @throws IOException
	 */
	public Boolean writeFile(String relpath, String content) 
		throws InvalidFilenameException, IOException;
	
	
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
}
