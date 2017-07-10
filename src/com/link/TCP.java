package com.link;

import sys.Errno;
import sys.StrUtil;

public class TCP extends AbstractLink{
	/*
	 * There are broken hosts (PekaoSA) that must receive data in one IP packet,
	 * and we must rebuild all data into one buffer before issue "send"
	 */
	private byte[] sndbuf=new byte[200];
	private byte[] hdrbuf=new byte[2];
	protected String pfx="";
	public void setPrefix(String pfx) {
		this.pfx=(pfx==null?"":pfx);
		if (hdrbuf.length<2+this.pfx.length())
			hdrbuf=new byte[2+this.pfx.length()];
	}

	@Override
	public int send(StringBuilder b) {
		int r=0;
		if (sndbuf.length<pfx.length()+2+b.length())
			sndbuf=new byte[pfx.length()+2+b.length()+50];
		StrUtil.bytes(pfx,0,sndbuf,r,pfx.length()); r+=pfx.length();
		sndbuf[r++]=(byte)((b.length()>>8)&0xff);
		sndbuf[r++]=(byte)(b.length()&0xff);
		StrUtil.bytes(b,0,sndbuf,r,b.length()); r+=b.length();
		//int n=pfx.length()+2;
		//log.debug("send[%d]: %s %s",r,StrUtil.hex(sndbuf,0,n),StrUtil.hex(sndbuf,n,r-n));

		setState(STATE_SEND,true);
		if ((r=io.write(sndbuf,0,r))<0) return r;
		io.sync();
		setState(STATE_SEND,false);
		return b.length();
	}
	public int send(byte[] b){
		int r=0;
		if (sndbuf.length<pfx.length()+2+b.length)
			sndbuf=new byte[pfx.length()+2+2*b.length];
		StrUtil.bytes(pfx,0,sndbuf,r,pfx.length()); r+=pfx.length();
		sndbuf[r++]=(byte)((b.length>>8)&0xff);
		sndbuf[r++]=(byte)(b.length&0xff);
		System.arraycopy(b, 0, sndbuf, r, b.length); r+=b.length;
		//int n=pfx.length()+2;
		//log.debug("send[%d]: %s %s",r,StrUtil.hex(sndbuf,0,n),StrUtil.hex(sndbuf,n,r-n));

		setState(STATE_SEND,true);
		if ((r=io.write(sndbuf,0,r))<0) return r;
		io.sync();
		setState(STATE_SEND,false);
		return b.length;
	}

	@Override
	public int recv(StringBuilder b) {
		int r,len;
		b.setLength(0);
		r=io.read(hdrbuf, 0, hdrbuf.length);
		if (r==0){
			if (Thread.currentThread().isInterrupted()) return -Errno.EABORT;
			return -Errno.EAGAIN;
		}
		if (r<0) { log.error("TCP.read(HDR)=%d",r); return r; }

		setState(STATE_RECV,true);
		if (r<hdrbuf.length){
			r=io.readFully(hdrbuf, r, hdrbuf.length-r);
			if (r<0) { log.error("TCP.read(HDR)=%d",r); return r; }
		}
		//log.debug("TCP.read(HDR)=%s",StrUtil.hex(buf));
		r=pfx.length();
		len=((hdrbuf[r]&0xff)<<8)+(hdrbuf[r+1]&0xff);
		for (; b.length()<len; ) {
			if ((r=io.read(b,len))<0) {
				log.error("TCP.read(DATA)=%d, read %d/%d",r,b.length(),len);
				if (b.length()>0)
					log.debug("TCP.buf[%d]: %s",b.length(),StrUtil.vis(b.toString()));
				if (r==-Errno.EAGAIN) r=-Errno.ETIMEOUT;
				return r;
			}
		}
		setState(STATE_RECV,false);
		//log.debug("recv[%d]: %s %s",r,hdr,StrUtil.hex(b.toString(),0,b.length()));
		return b.length();
	}
}
