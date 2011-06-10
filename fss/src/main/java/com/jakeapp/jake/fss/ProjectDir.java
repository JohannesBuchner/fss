package com.jakeapp.jake.fss;

import java.io.File;

@SuppressWarnings("serial")
public class ProjectDir extends File {

	public ProjectDir(String pathname) {
		super(pathname);
	}
	public ProjectDir(File pathname) {
		super(pathname.getAbsolutePath());
	}
}
