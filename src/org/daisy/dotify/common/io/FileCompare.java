package org.daisy.dotify.common.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides functionality to check if files are equal.
 * @author Joel Håkansson
 */
public class FileCompare {
	private int pos;

	/**
	 * Creates a new FileCompare object
	 */
	public FileCompare() {
		super();
	}

	/**
	 * Gets the byte position where the latest call to compareBinary or compareXML failed, or -1
	 * if compare was successful
	 * @return returns the byte position
	 */
	public int getPos() {
		return pos;
	}

	/**
	 * Compares the input streams binary.
	 * @param f1 the first input stream
	 * @param f2 the second input stream
	 * @return returns true if the streams are equal, false otherwise
	 * @throws IOException if IO fails
	 */
	public boolean compareBinary(InputStream f1, InputStream f2) throws IOException {
		try (	InputStream bf1 = new BufferedInputStream(f1);
				InputStream bf2 = new BufferedInputStream(f2) ){
			int b1;
			int b2;
			pos = 0;
			while ((b1 = bf1.read())!=-1 & b1 == (b2 = bf2.read())) {
				pos++;
				//continue
			}
			if (b1!=-1 || b2!=-1) {
				return false;
			}
			pos = -1;
			return true;
		}
	}
}
