package com.jakeapp.jake.fss;

import java.util.Arrays;

public class HashValue {

	public static String DIGEST = "SHA-1";

	public static int N_BITS = 160;

	/**
	 * In hex, there are 2 chars per 1 byte.
	 */
	public static int getStringLength() {
		return N_BITS * 2 / 8;
	}

	private byte[] value;

	private String s = null;

	public HashValue(byte[] value) {
		this.value = value;
	}

	public byte[] getValue() {
		return value;
	}

	@Override
	public String toString() {
		if (s != null)
			return s;
		s = "";
		for (int i = 0; i < value.length; i++) {
			int c = value[i];
			if (value[i] < 0)
				c = c + 256;
			s = s.concat(halfbyte2str(c / 16) + halfbyte2str(c % 16));
		}
		return s;
	}

	private static String halfbyte2str(int i) {
		switch (i) {
			case 0:
				return "0";
			case 1:
				return "1";
			case 2:
				return "2";
			case 3:
				return "3";
			case 4:
				return "4";
			case 5:
				return "5";
			case 6:
				return "6";
			case 7:
				return "7";
			case 8:
				return "8";
			case 9:
				return "9";
			case 10:
				return "a";
			case 11:
				return "b";
			case 12:
				return "c";
			case 13:
				return "d";
			case 14:
				return "e";
			case 15:
				return "f";
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(value);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof String)
			return toString().equals(obj);
		if (obj instanceof byte[])
			return value.equals(obj);
		if (getClass() != obj.getClass())
			return false;
		HashValue other = (HashValue) obj;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

}
