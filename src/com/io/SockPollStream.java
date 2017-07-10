package com.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import sys.Logger;
import sys.SysUtil;

public class SockPollStream extends InputStream implements Pollable{
	static Logger log=Logger.getLogger();
	private final Selector sel;
	private final InputStream is;
	private final SocketChannel soc;
	public SockPollStream(Socket soc) throws IOException {
		this.soc=soc.getChannel();
		this.is=soc.getInputStream();
		if (this.soc!=null) sel=Selector.open();
		else sel=null;
	}
	@Override
	public void close() throws IOException {
		is.close();
		if (soc!=null){
			soc.close();
			sel.close();
		}
	}
	@Override
	public int available() throws IOException {return is.available();}
	@Override
	public int read() throws IOException { return is.read(); }
	@Override
	public int read(byte[] b) throws IOException { return is.read(b,0,b.length); }
	@Override
	public int read(byte[] b,int off,int len) throws IOException { return is.read(b, off, len); }

	@Override
	public int poll(long tm) throws IOException {
		if (soc==null) {
			SysUtil.delay(100);
			return 0;
		}
		SelectionKey k=soc.register(sel,SelectionKey.OP_READ|SelectionKey.OP_ACCEPT,null);
		long t0=System.currentTimeMillis();
		int r=sel.select(tm);
		t0=System.currentTimeMillis()-t0;
		log.info("poll(%d)=%d  in %d",tm,r,t0);
		k.cancel();
		return r;
	}

}
