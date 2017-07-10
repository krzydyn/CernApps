package com.io;

import java.io.IOException;
import java.io.OutputStream;

import sys.StrUtil;

//use java.io.ByteArrayOutputStream
public class ByteOutputStream extends OutputStream {
	private final StringBuilder buf;
	public ByteOutputStream(){buf=new StringBuilder();}
	public ByteOutputStream(StringBuilder b){buf=b;}
	@Override
	public void write(int c) throws IOException {
		buf.append((char)(c&0xff));
	}
	public int size() {return buf.length();}
	public void reset(){buf.setLength(0);}
	/**
	 * note don't use to store binary data
	 * @return
	 */
	public StringBuilder get(){return buf;}
	public byte[] toByteArray(){return StrUtil.bytes(buf);}

	//ByteBuffer API
	public void clear() {buf.setLength(0);}
	public byte[] array(){return StrUtil.bytes(buf);}
}
