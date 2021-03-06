package com.github.chrisruffalo.pincushion.util;

import java.io.File;
import java.io.IOException;

public class PathUtil {

	/**
	 * Returns a sanitized path for the given file
	 * 
	 * @param file
	 * @return
	 */
	public static String sanitize(File file) {
		try {
			final String path = file.getCanonicalPath();
			return path;
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}
	
}
