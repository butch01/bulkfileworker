package bulkFileWorker;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulkFileWorker {

	private static final String version = new String("0.3.0");  
	private static final int SKIP = 0;
	private static final int OVERWRITE = 1;
	private static final int MOVE = 2;
	
	
	
	private static final int DATESOURCE_MODIFIED = 0;
	private static final int DATESOURCE_CREATED = 1;
	private static final int DATESOURCE_ACCESSED = 2;
	
	
	
	private static String inputBasePath = null;
	private static String outputBasePath = null;
	private static HashSet<String> directoryList = null;
	private static HashSet<FileCopy> copyList = null;
	
	private static int choosenCopyOption= -1;
	private static String fileExtension = null;
	
	private static String subPathString = "";
	
	
	private static void info(String infoLogString)
	{
		System.out.println("INFO - " + infoLogString);
	}
	
	private static void error(String errorLogString)
	{
		System.out.println("ERROR - " + errorLogString);
	}
	
	
	/**
	 * do some sanity check
	 * @return true -> check ok, false check not ok
	 */
	private static boolean isSanityCheckOk()
	{
		boolean isCheckOk=true;
		if (inputBasePath == null)
		{
			System.out.println("ERROR: inputBasePath not set");
			isCheckOk = false;
		}
		if (outputBasePath == null)
		{
			System.out.println("ERROR: outputBasePath not set");
			isCheckOk = false;
		}
		if (choosenCopyOption == -1)
		{
			System.out.println("ERROR: mode not set");
			isCheckOk = false;
		}
		return isCheckOk;
	}
	
	private static void printUsage()
	{
		System.out.println("\n"
				+ "usage:\n"
				+ "version:" + version + "\n\n"
				+ "java -jar bulkFileWorker.jar <OPTIONS>"
				+ "OPTIONS:"
				+ "   mode=<copyOption> ext=<fileExtension> subdir=<subDirectory\n"
				+ "  source=<sourceRoot>: root directory of source. Subdirectories will be searched\n"
				+ "  target=<targetRoot>: root directory of target. Following structure will be created:"
				+ "                       targetRoot/yyyy/yyyy-mm-dd/<subDir>\n"
				+ "  mode=c             : copy and replace if existing\n"
				+ "       m             : move and skip if existing\n"
				+ "       s             : copy and skip if existing\n"
				+ " ext=<fileExtension> : only process files matching the given extension. Without dot. e.g: mp4)"
				+ " subdir=<subDir>     : static subdir path is added at the end of the generated target path. (eg. targetRoot/yyyy/yyyy-mm-dd/subDir");
	}
	
	public static void main(String[] args) 
	{
		
		
//		if (args.length != 4)
//		{
//			error("bad number of arguments!\n"
//					+ "exiting.");
//			printUsage();
//			return;
//		}
		
		for (int i=0; i< args.length; i++)
		{
			String kv[] = args[i].split("=");
			if (kv[0].toLowerCase().equals("source"))
			{
				inputBasePath = new String (kv[1]);
			}
			else if (kv[0].toLowerCase().equals("target"))
			{
				outputBasePath = new String (kv[1]);
			}
			else if (kv[0].toLowerCase().equals("mode"))
			{
				if (kv[1].toLowerCase().equals("m"))
				{
					choosenCopyOption = MOVE;
				}
				else if (kv[1].toLowerCase().equals("c"))
				{
					choosenCopyOption = OVERWRITE;
				}
				else if (kv[1].toLowerCase().equals("s"))
				{
					choosenCopyOption = SKIP;
				}
			}
			else if (kv[0].toLowerCase().equals("ext"))
			{
				fileExtension = new String (kv[1]);
			}
			else if (kv[0].toLowerCase().equals("subdir"))
			{
				subPathString = "/" + new String (kv[1]);
			}

		}
		
		System.out.println("options:");
		System.out.println("inputBasePath=" + inputBasePath);
		System.out.println("outputBasePath=" + outputBasePath);
		System.out.println("choosenCopyOption=" + choosenCopyOption);
		System.out.println("fileExtension=" + fileExtension);
		System.out.println("subPathString=" + subPathString);
		
		if (!isSanityCheckOk())
		{
			System.out.println ("aborting because of errors");
			printUsage();
			return;
		}
		
		directoryList = new HashSet<String>();
		copyList = new HashSet<FileCopy>();
//		
//		// parse cmd arguments
//		inputBasePath = new String(args[0]);
//		outputBasePath = new String(args[1]);
//		
//		if (args[2].toLowerCase().equals("m"))
//		{
//			choosenCopyOption = MOVE;
//		}
//		else if (args[2].toLowerCase().equals("s"))
//		{
//			choosenCopyOption = SKIP;
//		}
//		else if (args[2].toLowerCase().equals("c"))
//		{
//			choosenCopyOption = OVERWRITE;
//		}
//		else if (args[3.toLowerCase().equals])
//		
//		fileExtension = new String (args[3]);
		
		
		
		
		try 
		{
			List<Path> dirList = generateDirectoryList();
			dirList.forEach(element -> generateLists(element));
			
			
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// create directories
		directoryList.forEach(element -> createFolderIfNotExists(element));
		
		// copy or move files
		
		copyList.forEach(element -> 
		{
			try 
			{
				copyFile(element.getSource(), element.getTarget(), choosenCopyOption);
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		});
		
		info("copying file attributes...");
		copyList.forEach(element -> copyDateAttribute(element.getTarget(), DATESOURCE_MODIFIED));
		
		info ("ended.");
	}

	/**
	 * walks through the directory and puts files to a list
	 * 
	 * @param basePath
	 * @return
	 * @throws IOException
	 */
	public static List<Path> generateDirectoryList() throws IOException
	{
		List<Path> files = Files.walk(Paths.get(inputBasePath)).collect(Collectors.toList());
		return files;
	}

	
	/**
	 * checks if a file is already existing
	 * @param pathAsString
	 * @return true -  file exists or false - file does not exist
	 */
	private static boolean isFileExisting(String pathAsString)
	{
		Path path = FileSystems.getDefault().getPath(pathAsString);
		if (Files.exists(path))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	/**
	 * create a folder if it does not exist
	 * @param folder
	 */
	public static void createFolderIfNotExists(String folder)
	{
		Path path = FileSystems.getDefault().getPath(folder);
		if (!isFileExisting(path.toString())) 
		{
			new File(folder).mkdirs();
			info("crated folder: " + folder);
		}
	}
	
	
	/**
	 * gets the filename without the path and returns it
	 * @param path
	 * @return fileName
	 */
	private static String getFileNameFromPath(Path path)
	{
		return path.getFileName().toString();
	}

	
	
	/**
	 * copy a file, replace if exists
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	private static void copyFile(String source, String target, int copyMode) throws IOException
	{
	    Path sourcePath = Paths.get(source);
	    Path targetPath = Paths.get(target);
	    
	    switch (copyMode)
	    {
	    	// skip existing
	    	case SKIP: 
	    	{
	    		if (!isFileExisting(target))
	    		{
    				// file does not exist
	    			Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
	    			info(sourcePath + " copied to " + targetPath);
	    		}
	    		else
	    		{
	    			// file does exist
	    			error("file " + target + " already exisits. skipping...");
	    		}
	    		break;
	    	}

	    	// copy and replace
	    	case OVERWRITE: 
	    	{
	    		Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
	    		info(sourcePath + " copied to " + targetPath);
	    		break;
	    	}
	    	
	    	// move files, skip existing
	    	case MOVE: 
	    	{
	    		if (!isFileExisting(target))
	    	
	    		{
    				// file does not exist
	    			Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
	    			info(sourcePath + " moved to " + targetPath);
	    		}
	    		else
	    		{
	    			// file does exist
	    			error("file " + target + " already exisits. skipping...");
	    		}
	    		break;
	    	}
	    }
	}
	
	/**
	 * 
	 * @param pathString
	 * @param dateSource
	 */
	private static void copyDateAttribute(String fileName, int dateSource)
	{
		File file = new File(fileName);
		Path path = FileSystems.getDefault().getPath(fileName);
		
		// get file attribute which is defined as source
		BasicFileAttributes attr = null;
		
		
		try 
		{
			attr = Files.readAttributes(path, BasicFileAttributes.class);
			FileTime fileTimeSource = null;
			String logCopiedFrom = null;
			
			switch (dateSource)
			{
				case DATESOURCE_ACCESSED:
					fileTimeSource = attr.lastAccessTime();
					logCopiedFrom = new String ("last accessed");
					break;

				case DATESOURCE_CREATED:
					fileTimeSource = attr.creationTime();
					logCopiedFrom = new String ("creation time");
					break;
				
				case DATESOURCE_MODIFIED:
					fileTimeSource = attr.lastModifiedTime();
					logCopiedFrom = new String ("last modified");
					break;
			}
			
			// copy fileTime source to all 3 times (accessed, creation, modified)
			file.setLastModified(fileTimeSource.toMillis());
			Files.setAttribute(path, "creationTime", fileTimeSource);
			info("times copied from " + logCopiedFrom + "(" + fileTimeSource.toString() + ")");
			
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	/**
	 * this function generates adds the path to one of the following lists:
	 * - copyList
	 * - directoryList
	 * @param myFile
	 */
	private static void generateLists(Path myFile)
	{
		//Path myFile = FileSystems.getDefault().getPath(fileName);
		BasicFileAttributes attr = null;
		
		try 
		{
			attr = Files.readAttributes(myFile, BasicFileAttributes.class);
			if (!attr.isDirectory())
			{
				// check for extension filter
				if (myFile.toString().endsWith(fileExtension)) 
				{
					// remember to create the directory later
					String outputDir = outputBasePath + "/" + attr.lastModifiedTime().toString().substring(0,4) + "/" + attr.lastModifiedTime().toString().substring(0,10) + subPathString;
					directoryList.add(outputDir);
					// remember to copy the file later
					copyList.add(new FileCopy(myFile.toString(), outputDir + "/" +  getFileNameFromPath(myFile)));
				}
				else
				{
					info("extension not matching for file: " + myFile.toString() + " extension fitler is: " + fileExtension );
				}
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
