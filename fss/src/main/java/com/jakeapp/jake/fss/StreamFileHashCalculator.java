package com.jakeapp.jake.fss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StreamFileHashCalculator {

	private MessageDigest md;

	public StreamFileHashCalculator() throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance(HashValue.DIGEST);
	}

	public HashValue calculateHash(File f) throws FileNotFoundException {
		return calculateHash(new FileInputStream(f));
	}

	public HashValue calculateHash(InputStream is) {
		DigestInputStream dis = new DigestInputStream(is, md);
		byte[] tmp = new byte[1024];
		try {
			while (dis.read(tmp) >= 0);
			dis.close();
		} catch (IOException e) {
			return null;
		}
		return new HashValue(md.digest());
	}

}
