package com.io;

import java.io.IOException;
import java.io.InputStream;

public class ByteInputStream extends InputStream {
	StringBuilder buf;
	int pos=0;
	public ByteInputStream(StringBuilder b) { buf=b; }
	public ByteInputStream(StringBuilder b,int p) { buf=b; pos=p;}
	public ByteInputStream(String s) {
		this(new StringBuilder(s));
	}
	public int available() throws IOException {
		return buf.length()-pos;
	}
	public int read() throws IOException {
		if (pos>=buf.length()) return -1;
		return buf.charAt(pos++)&0xff;
	}	
}
