package com.link;

import java.io.IOException;

import com.io.IOStream;

abstract public class AbstractLink implements DataLink {
	public static final int STATE_RECV=1;
	public static final int STATE_SEND=2;

	public static interface LinkStateListener {
		public void stateChanged(int st);
	}

	protected IOStream io=null;
	protected LinkStateListener stateListener;
	private int state=0;

	public final void setStateListener(LinkStateListener sl){
		this.stateListener=sl;
	}
	public final void setIO(IOStream io){
		this.io=io;
	}
	public final IOStream getIO() { return io; }
	public int available(){return io.available();}

	@Override
	public int open() { return 0; }

	@Override
	public int close() {
		setState(-1,false);
		if (io!=null) {io.close();io=null;}
		return 0;
	}
	public boolean isConnected() {
		if (io==null) return false;
		return io.isOpened();
	}

	protected final void setState(int st, boolean on) {
		if (on) st = state|st;
		else st = state&(~st);
		if (st==state) return ;
		state=st;
		if (stateListener!=null) stateListener.stateChanged(st);
	}

	public final void send(String b) throws IOException {
		int r = send(new StringBuilder(b));
		if (r < 0) throw new IOException("send=" + r);
	}
	public final String recv() throws IOException {
		StringBuilder b = new StringBuilder();
		int r = recv(b);
		if (r < 0) throw new IOException("recv=" + r);
		return b.toString();
	}
	public void idle() {setState(-1,false);}
}
