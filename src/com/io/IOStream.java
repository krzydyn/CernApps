package com.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sys.Errno;
import sys.Logger;
import sys.StrUtil;
import sys.SysUtil;

//TODO output stream buffering
public class IOStream implements Closeable {
	protected static Logger log = Logger.getLogger();
	public final static int MO_BITS8=0;
	public final static int MO_BITS7e=1;//even=parzysty
	public final static int MO_BITS7o=2;//odd=nieparzysty
	public final static int MO_BITS7=3;//just 7 bits (highest bit=0)
	public final static int MO_BITS_MSK=3;

	protected InputStream is=null;
	protected OutputStream os=null;
	protected int connTmo=SysUtil.SECOND;
	protected int rxtmo=SysUtil.SECOND;    //first char tmo in buffer reads
	protected int chrtmo=SysUtil.SECOND/2; //char-to-char tmo in buffer reads
	protected int mode=MO_BITS8;
	protected boolean connected=false;

	public IOStream(InputStream is,OutputStream os){setIO(is,os);}
	protected IOStream(){}
	protected void setIO(InputStream is,OutputStream os){
		this.is=is; this.os=os;
		connected=true;
	}
	public void setMode(int m) {mode=m;}
	public InputStream getInputStream() { return is; }
	public OutputStream getOutputStream() { return os; }
	public void open() throws IOException {}
	public boolean isOpened() {return connected;}
	@Override
	public void close(){
		if (is==null && os==null) return ;
		InputStream i=is;
		OutputStream o=os;
		is=null; os=null;
		if (i!=null) IOUtils.close(i);
		if (o!=null) IOUtils.close(o);
	}
	public void setOpenTmo(int t) {connTmo = t;}
	/**
	 *
	 * @param t timeout in milliseconds (default is SECOND)
	 */
	public void setRxTmo(int t) {rxtmo=t;if (chrtmo>t) chrtmo=t;}
	/**
	 *
	 * @param t timeout in milliseconds (default is SECOND/2)
	 */
	public void setChrTmo(int t) {chrtmo=t;if (rxtmo<t) rxtmo=t;}
	public int available(){
		try{
			return is.available();
		}catch (Exception e) {}
		return -1;
	}
	public int writec(int c){return writec((byte)c);}
	public int writec(byte c){
		if (os==null) return -Errno.ESTATE;
		try { os.write(fixparitybit(c)); return 0; }
		catch (Exception e) { log.error(e,"writec(%02x)",c); }
		return -Errno.EIO;
	}
	public void sync(){
		if (os==null) return;
		try{
			os.flush();
		}catch (Exception e) {log.error(e,"sync");}
	}
	public int write(byte b[],int s,int l){
		if (os==null) return -Errno.ESTATE;
		if ((mode&MO_BITS_MSK)!=MO_BITS8)
			for (int i=0; i<l; i++)b[s+i]=fixparitybit(b[s+i]);
		try { os.write(b,s,l); return l; }
		catch (Exception e) { log.error(e,"write(byte[],int)"); }
		return -Errno.EIO;
	}
	public int write(byte b[]){return write(b,0,b.length);}
	public int write(String b) { return this.write(StrUtil.bytes(b)); }
	public int write(StringBuilder b) { return this.write(StrUtil.bytes(b)); }
	public int poll(long t) {
		if (is==null) return -Errno.ESTATE;
		sync();
		try {
			if (is instanceof Pollable) {
				return ((Pollable)is).poll(t);
			}
			//not efficient
			//log.error(new Exception("io.poll is not efficient"));
			log.error("io.poll is not efficient");
			int r;
			t+=System.currentTimeMillis();
			while((r=is.available())==0) {
				if (t<System.currentTimeMillis()) break;
				SysUtil.delay(chrtmo/4+1);
				if (is==null) return -Errno.ESTATE;//stream closed
			}
			return r;
		}
		catch (java.io.IOException e) { log.error(e.getMessage(),"poll"); }
		catch (Exception e) { log.error(e,"poll"); }
		return -Errno.EIO;
	}
	public void flush() {
		if (is!=null)try{is.skip(is.available());}catch (Exception e) {log.error(e,"flush");}
		if (os!=null)try{os.flush();}catch (Exception e) {log.error(e,"flush");}
	}
	public int readc(){
		int r;
		if (is==null) return -Errno.ESTATE;
		try{
			if ((r=is.read())<0){
				if (r==-1) return r;// -1 means EOF
				r=-Errno.EIO;
				log.error("%s.readc=%d",getClass().getName(),r);
				return r;
			}
			//log.debug("readc=%02X",r);
			//if (r==2) new Exception("r==2").printStackTrace();
		}
		catch (java.net.SocketTimeoutException e){return -Errno.EAGAIN;}
		catch (java.net.SocketException e){log.error(e,"readc");return -Errno.EIO;}
		catch (java.io.IOException e) {
			//FIXME Socket input stream should throw java.net.SocketTimeoutException
			//if (e.getMessage().indexOf("temporary")>=0) {return -Errno.EAGAIN;}
			log.error(e,toString());
			return -Errno.EIO;
		}
		catch (Exception e) { log.error(e,"readc");return -Errno.EIO;}
		if ((mode&MO_BITS_MSK)!=0) r&=0x7f; else r&=0xff;
		return r;
	}
	/**
	 * read maxlen byte from input
	 * @param b
	 * @param offs
	 * @param maxlen
	 * @return number of bytes read
	 */
	public int read(byte b[],int offs,int len) {
		int r=0,i;
		//long tmo=SysUtils.timer_start(chrtmo);
		for (i=0; i<len; ) {
			if (Thread.currentThread().isInterrupted()) {r=0;break;}
			if (is==null) {log.error("i-stream is null");return -Errno.ESTATE;}
			try{
				r=is.read(b, offs+i, len-i);
				if (r==-1) r=-Errno.EEOT; //end of stream reached
			}
			catch (java.net.SocketTimeoutException e){r=0;}
			catch (IOException e) {log.error(e);r=-Errno.EIO;}
			catch (Throwable e) {log.error(e);r=-Errno.EABORT;}
			if (r==0) break;
			if (r<0) {log.error("is.read=%d",r);break;}
			i+=r;
			if (offs+i>=b.length) break;
		}
		return i>0?i:r;
	}
	public int readFully(byte b[],int offs,int len){
		int i,r;
		long tmo=SysUtil.timer_start(rxtmo);
		for (i=0; i<len; i+=r) {
			r=read(b,offs+i,len-i);
			if (r<0) return r;
			if (r==0) {
				if (SysUtil.timer_expired(tmo)){
					log.error("read %d of %d",i,len);
					return -Errno.ETIMEOUT;
				}
				//poll(chrtmo);
				SysUtil.delay(100);
				continue;
			}
			tmo=SysUtil.timer_start(rxtmo);
		}
		return i;
	}
	public int read(StringBuilder b,int maxlen){
		int r=0,i;
		long tmo=SysUtil.timer_start(rxtmo);
		for (i=0; maxlen<=0||b.length()<maxlen; ) {
			if (Thread.currentThread().isInterrupted()) return -Errno.EABORT;
			if ((r=readc())<0) {
				if (r==-Errno.EAGAIN){
					if (SysUtil.timer_expired(tmo)){r=-Errno.ETIMEOUT; break;}
					continue;
				}
				break;
			}
			b.append((char)r); ++i;
			tmo=SysUtil.timer_start(chrtmo);
		}
		return i>0?i:r;
	}
	/**
	 * read until given byte is read
	 * @param b
	 * @param es end sentinel
	 * @return number of bytes read
	 */
	public int readUntil(StringBuilder b,int es) {
		int r;
		b.setLength(0);
		long tmo=SysUtil.timer_start(rxtmo);
		for (;;) {
			if (Thread.currentThread().isInterrupted()) return -Errno.EABORT;
			if ((r=readc())<0) {
				if (r==-Errno.EAGAIN) {
					if (SysUtil.timer_expired(tmo)) {
						if (b.length()>0) log.warn("buf[%d]=%s",b.length(),b.toString());
						return -Errno.ETIMEOUT;
					}
					SysUtil.delay(chrtmo/4+1);continue;
				}
				break;
			}
			tmo=SysUtil.timer_start(chrtmo);
			b.append((char)r);
			if (r==es) return b.length();
		}
		return b.length()>0?b.length():r;
	}
	public int readln(StringBuilder b) {
		int r=readUntil(b,'\n');
		if (r<0) return r;
		if (r>1 && b.charAt(b.length()-1)=='\n') {
			r=b.length()-2;
			if (b.charAt(r)=='\r') b.deleteCharAt(r);
		}
		return b.length();
	}
	/**
	 * read multiple lines
	 * @param buf
	 * @param rep
	 * @return number of lines
	 */
	public int readlns(StringBuilder buf) {
		int ln;
		StringBuilder b=new StringBuilder();
		buf.setLength(0);
		for (ln=0; readln(b)>0; ++ln) buf.append(b);
		return ln;
	}

	private static byte parity[]=new byte[16]; {
		parity[0]=0;
		// nice hack, isn't it?!
		for (int i=1; i < parity.length; i*=2)
			for (int j=0; j<i; j++)parity[i+j]=(byte)(parity[j]^1);
	}
	final byte fixparitybit(byte b) {
		if ((mode&MO_BITS_MSK)==MO_BITS8) return b; //8bit clean
		b&=0x7f; //clear 7-th bit
		byte n1=parity[b&0x0f];
		byte n2=parity[(b>>4)&0x0f];
		//nx=0 => is even, nx=1 => is odd
		if ((mode&MO_BITS_MSK)==MO_BITS7e) {if (n1!=n2) b|=0x80;}
		else if ((mode&MO_BITS_MSK)==MO_BITS7o) {if (n1==n2) b|=0x80;}
		return b;
	}

	public static IOStream createIOStream(String addr){
		addr=addr.trim();
		IOStream io=null;
		try{
			//if (addr.indexOf("via:")>=0) conn=new HttpProxy();
			//else
			io=new TCPStream(addr);
			//io.parseURI(addr);
		}
		catch (Exception e) {
			/*String n=addr.toUpperCase();
			if (n.indexOf("COM")>=0) conn=new SerialConnection(addr);
			else if (n.indexOf("TTYS")>=0) conn=new SerialConnection(addr);
			else if (n.indexOf("LP")>=0) conn=new ParallelConnection(addr);
			else log.error(e,addr);
			*/
		}
		return io;
	}
	public static boolean tryOpen(String addr) {
		IOStream c=IOStream.createIOStream(addr);
		try{
			c.setOpenTmo(2000);
			c.open();
			log.debug("host %s connected",addr);
			return true;
		}catch (IOException e) {
		}catch (Exception e) {
			log.error(e);
		}finally{c.close();}
		return false;
	}
}
