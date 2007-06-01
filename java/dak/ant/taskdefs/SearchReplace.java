/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dak.ant.taskdefs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;

import org.apache.tools.ant.taskdefs.MatchingTask;

/**
 * This task replaces class on a running JVM. This task can take the following
 * arguments:
 * <ul>
 * <li/>verbose
 * <li/>failonerror
 * <li/>host
 * <li/>port
 * <li/>name
 * </ul>
 * Of these arguments, the <b>host</b> and <b>port</b> are required. Or,
 * the <b>name</b> can be used instead to indicate a shared mem connection.
 * <p/>
 * See the JPDA documentation for details on the JVM runtime options.
 * <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jpda/conninv.html#Invocation">
 * http://java.sun.com/j2se/1.4.2/docs/guide/jpda/conninv.html#Invocation</a>
 * <p/>
 * Add this line to your build.xml<br/>
 * <code>
 *   <taskdef name="SearchReplace" classname="dak.ant.taskdefs.SearchReplace"/>
 * </code>
 * <p/>
 * This is an example of how to hotswap with a JVM on port 9000 on your local machine
 * <br/>
 * <code>
 * <!-- note, replace the <star> tags below with "*". This kept the example from breaking the javadoc -->
 *	<hotswap verbose="true" port="9000">
 *		<!-- This line matches 3 classes in the ant build/classes directory -->
 *		<fileset dir="build/classes" includes="<star><star>/Hot*.class"/>
 *		<!-- This line matches all classes in the taskefs package (and below) -->
 *		<fileset dir="build/classes" includes="<star><star>/taskdefs"/>
 *	</hotswap>
 * </code>
 * <br/>
 * The preferred way to build the <fileset> would be based on modification time.
 * At present, the tstamp isn't fine grained enough. The <outofdate> task from ant-contrib
 * provides absolute paths to all of the class files, which isn't compatible with the
 * way <hotswap> needs the paths. 
 *
 * @author David A. Kavanagh <a href="mailto:dak@dotech.com">dak@dotech.com</a>
 */

public class SearchReplace extends MatchingTask {

    private static final String FAIL_MSG
        = "Hotswap failed; changes to class(es) might not be compatible with replacement on your VM.";

    private boolean verbose = false;
    private boolean failonerror = true;

	protected String find;
	protected String replace;
    protected Vector filesets = new Vector();

    /**
     * SearchReplace task for compilation of Java files.
     */
    public SearchReplace() { }

    /**
     * If true, asks the compiler for verbose output.
     * @param verbose if true, asks the compiler for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Gets the verbose flag.
     * @return the verbose flag
     */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * Gets the name of the find with the running VM.
     * @return the hotswap find name
     */
    public String getFind() {
        return find;
    }

    /**
     * Sets the name of the find with the running VM.
     * @param find the find to be used when connecting to a running VM
     */
    public void setFind(String find) {
        this.find = find;
    }

    /**
     * Gets the name of the replace with the running VM.
     * @return the hotswap replace name
     */
    public String getReplace() {
        return replace;
    }

    /**
     * Sets the name of the replace with the running VM.
     * @param replace the replace to be used when connecting to a running VM
     */
    public void setReplace(String replace) {
        this.replace = replace;
    }

    /**
     * If false, note errors but continue.
     *
     * @param failonerror true or false
     */
     public void setFailOnError(boolean failonerror) {
         this.failonerror = failonerror;
     }

	/**
	 * Adds a set of files to be deployed.
	 * @param set the set of files to be deployed
	 */
	public void addFileset(FileSet set) {
		filesets.addElement(set);
	}

    /**
     * Executes the task.
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        checkParameters();

		try {
			// load classes and replace them on target VM
        	for (int i = 0; i < filesets.size(); i++) {
            	FileSet fs = (FileSet) filesets.elementAt(i);
				try {
					DirectoryScanner ds = fs.getDirectoryScanner(getProject());
					String[] files = ds.getIncludedFiles();
					String[] dirs = ds.getIncludedDirectories();
					searchFiles(fs.getDir(getProject()), files, dirs);
				} catch (BuildException be) {
					// directory doesn't exist or is not readable
					if (failonerror) {
						throw be;
					} else {
						log(FAIL_MSG);
						log(be.getMessage());
					}
				}
			}
		} catch (Exception ex) {
			if (failonerror) {
				throw new BuildException(ex);
			} else {
				log(FAIL_MSG);
				log(ex.getMessage());
			}
		}
    }

    /**
     * Check that all required attributes have been set and nothing
     * silly has been entered.
     *
     * @since Ant 1.5
     * @exception BuildException if an error occurs
     */
    protected void checkParameters() throws BuildException {
        if (filesets.size() == 0) {
            throw new BuildException("At least one of the file or dir "
                                     + "attributes, or a fileset element, "
                                     + "must be set.");
        }

        if ((find == null) && (replace == null)) {
            throw new BuildException("find is null or replace is null");
        }
    }

    /**
     * remove an array of files in a directory, and a list of subdirectories
     * which will only be deleted if 'includeEmpty' is true
     * @param d directory to work from
     * @param files array of files to delete; can be of zero length
     * @param dirs array of directories to delete; can of zero length
     */
    protected void searchFiles(File d, String[] files, String[] dirs) throws Exception {
        if (files.length > 0) {
            log("searching " + files.length + " files from "
                + d.getAbsolutePath());
            for (int j = 0; j < files.length; j++) {
				processFile(d, files[j], find, replace);
            }
        }

        if (dirs.length > 0) {
            int dirCount = 0;
            for (int j = dirs.length - 1; j >= 0; j--) {
 				log("swapping dir " + d.getAbsolutePath() +", "+ dirs[j]);
				processDirectory(d, dirs[j]);
//                dirCount++;
            }

			// TODO: need an accurate count?
            if (dirCount > 0) {
                log("searched " + dirCount + " director"
                    + (dirCount == 1 ? "y" : "ies")
                    + " from " + d.getAbsolutePath());
            }
        }
    }

	private void processDirectory(File d, String subdir) throws Exception {
		File [] files = new File(d, subdir).listFiles();
		for (int i=0; i<files.length; i++) {
			if (files[i].isDirectory()) {
				processDirectory(d, getClassOrPackage(d, files[i]));
			}
			else {
				processFile(d, getClassOrPackage(d, files[i]), find, replace);
			}
		}
	}

	private String getClassOrPackage(File baseDir, File fileOrDir) {
		return fileOrDir.getAbsolutePath().substring(baseDir.getAbsolutePath().length()+1);
	}

	private void processFile(File d, String file, String find, String replace) throws Exception {
        boolean foundOne = false;
		File f = new File(d, file);
		if (verbose)
			log("searching " + f.getPath());

        FileInputStream in = new FileInputStream(f);
        File tmpFile = File.createTempFile("antSR", "tmp", new File("."));
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), 4096);

		byte [] buffer = new byte [16*1024];
		int count = in.read(buffer);
        int len = find.length();
        String rem = "";
		while (count != -1) {
			if (count > 0) {
                String buf = rem + new String(buffer, 0, count);
                int idx = buf.indexOf(find);
                while (idx > -1) {
                    out.write(buf.getBytes(), 0, idx);
                    out.write(replace.getBytes());
                    buf = buf.substring(idx+len);
//                    System.err.println("found string... text after find : "+buf);
                    idx = buf.indexOf(find);
                    foundOne = true;
                }
                if (buf.length() > len) {
                    out.write(buf.getBytes(), 0, buf.length() - len+1);
                    rem = buf.substring(buf.length() - (len-1));
                }
                else {
                    rem = buf;
                }

            }

			count = in.read(buffer);
		}
        out.write(rem.getBytes());

		in.close();
		out.flush();
		out.close();

        if (foundOne) {
            // now, remove old, and replace with edited version.
            f.delete();
            tmpFile.renameTo(f);
        }
        else {
            tmpFile.delete();
        }
	}
}
