package epics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import common.Errno;
import common.StrUtil;
import common.connection.link.AbstractLink;

/*
 * http://epics.cosylab.com/cosyjava/JCA-Common/Documentation/CAproto.html
 * 
 * TCP communication protocol
 * client				server
 * connect	->			accept
 * 
 * initialization (VER=0000000D):
 * 			CMD	 PLEN  							  PAYLOAD
 * recv[16]:0000 0000 0000000D 00000000 00000000
 * send[16]:0000 0000 0000000D 00000000 00000000
 * 
 * send[24]:0014 0008 00000000 00000000 00000000 6B727A79 73000000 (krzys)
 * send[24]:0015 0008 00000000 00000000 00000000 6B727A79 63686F00 (krzycho)
 * 
 * create/register channel request (0,0,CID,VER,NAME,pad(0))
 * send[40]:0012 0018 00000000 00000001 0000000D (LV:DaisyChain:0:Error)+PAD(0)
 * recv[16]:0016 0000 00000000 00000001 00000003
 * 							   (CID,RIGHTS)
 * recv[16]:0012 0000 0003 0001 00000001 00000468
 * 					(DBRTYPE,DBRCNT,CID,SID)
 * 
 * read channel request (DBRTYPE,DATACNT,SID,CID):
 * send[16]:000F 0000 0003 0001 00000468 00000001
 * recv[24]:000F 0008 0003 0001 00000001 00000001 00000000 00000000
 * 					(DBRTYPE,DATACNT,?,CID,DBRDATA):
 * 
 * send[40]:0012 0018 0000 0000 00000002 0000000D (LV:VTPC1:1:1:Read)+PAD(0)
 * recv[32]:0016 0000 0000 0000 00000002 00000003 00120000 00060001 00000002 00000469
 * send[16]:000F 0000 0006 0001 00000469 00000002
 * recv[24]:000F 0008 0006 0001 00000001 00000002 40EFFFE0 00000000
 * 					(DBRTYPE,DATACNT,?,CID,DBRDATA):
 */

public class JcaLink extends AbstractLink {
	final static int CA_VERSION=13;
	//procolt commands
	final static int CA_PROTO_VERSION=0;
	final static int CA_PROTO_EVENT_ADD=1;
	final static int CA_PROTO_EVENT_CANCEL=2;
	//final static int CA_PROTO_READ=3;//deprecated
	final static int CA_PROTO_WRITE=4;//no resp. 
	final static int CA_PROTO_SEARCH=6; //search for channel name(resp in UDP)
	final static int CA_PROTO_EVENTS_OFF=8;
	final static int CA_PROTO_EVENTS_ON=9;
	//final static int CA_PROTO_READ_SYNC=0x0A;//deprecated
	final static int CA_PROTO_ERROR=0x0B;
	final static int CA_PROTO_CLEAR_CHANNEL=0x0C;
	//final static int CA_PROTO_NOT_FOUND=0xE; //only UDP
	final static int CA_PROTO_READ_NOTIFY=0xF;
	final static int CA_PROTO_CREATE_CHAN=0x12;
	final static int CA_PROTO_WRITE_NOTIFY=0x13;
	final static int CA_PROTO_CLIENT_NAME=0x14;//no resp.
	final static int CA_PROTO_HOST_NAME=0x15;  //no resp
	final static int CA_PROTO_ACCESS_RIGHTS=0x16;//only resp
	final static int CA_PROTO_ECHO=0x17;
	final static int CA_PROTO_CREATE_CH_FAIL=0x1A;
	final static int CA_PROTO_SERVER_DISCONN=0x1B;//only resp
	
	public int getCmd() { return cmd; }
	public void setCmd(int cmd) { this.cmd = cmd; }
	public int getDatalen() { return datalen; }
	public void setDatalen(int datalen) { this.datalen = datalen; }
	public int getCaVersion() { return CA_VERSION; }
	public int getHostVersion() { return srver; }
	public int getCid() { return cid; }
	public void setCid(int cid) { this.cid = cid; }
	public int getSid() { return sid; }
	public void setSid(int sid) { this.sid = sid; }
	public int getDbrType() { return dbrtype; }
	public void setDbrtype(int dbrtype) { this.dbrtype = dbrtype; }
	public int getDbrCCount() { return dbrcnt; }
	public void setDbrcnt(int dbrcnt) { this.dbrcnt = dbrcnt; }
	public int getRights() { return rights; }
	
	private byte[] hdrbuf=new byte[16];
	private int cmd;
	private int datalen;
	private int srver;
	private int cid,sid;
	private int rights,dbrtype,dbrcnt;
	private int wrstatus;
	
	public int recv(StringBuilder b) {
		int r;
		r=io.read(hdrbuf, 0, hdrbuf.length);
		if (r==0) return -Errno.EAGAIN;
		if (r<0) { log.error("TCP.read(HDR)=%d",r); return r; }
		try {
			setState(STATE_RECV);
			if (r<hdrbuf.length){
				r=io.readFully(hdrbuf, r, hdrbuf.length-r);
				if (r<0) { log.error("JCA.read(HDR)=%d",r); return r; }
			}
			try{
				parseHdr();	
			}catch (Exception e) {
				log.error(e); return -Errno.EINVAL;
			}
			for (b.setLength(0); b.length()<datalen; ) {
				if ((r=io.read(b,datalen))<0) {
					log.error("JCA.read(DATA)=%d, read %d/%d",r,b.length(),datalen);
					if (b.length()>0)
						log.debug("JCA.buf[%d]: %s",b.length(),StrUtil.vis(b.toString()));
					if (r==-Errno.EAGAIN) r=-Errno.ETIMEOUT;
					return r;
				}
			}
			r=hdrbuf.length+b.length();
			if (cmd==CA_PROTO_VERSION||cmd==CA_PROTO_CLIENT_NAME||cmd==CA_PROTO_HOST_NAME)
				log.info("recv[%d]: %s %s",r,StrUtil.hex(hdrbuf),StrUtil.hex(b.toString()));

			//log.debug("jcalink.recv cmd=%d hdr=%s, data=%s",cmd,StrUtil.hex(hdrbuf),StrUtil.vis(b));
			return b.length();
		}
		finally {
			setState(0);
		}
	}

	public int getWrstatus() {
		return wrstatus;
	}
	public int send(StringBuilder b) {
		datalen=b.length();
		try{
			buildHdr();	
		}catch (Exception e) {
			log.error(e); return -Errno.EINVAL;
		}
		//log.debug("jcalink.send cmd=%d hdr=%s, data=%s",cmd,StrUtil.hex(hdrbuf),StrUtil.vis(b));
		while (b.length()<datalen) b.append((char)0);
		int r=hdrbuf.length+b.length();
		//if (cmd==CA_PROTO_WRITE_NOTIFY||cmd==CA_PROTO_READ_NOTIFY)
		if (cmd==CA_PROTO_VERSION||cmd==CA_PROTO_CLIENT_NAME||cmd==CA_PROTO_HOST_NAME)
			log.info("send[%d]: %s %s",r,StrUtil.hex(hdrbuf),StrUtil.hex(b.toString()));
		setState(STATE_SEND);
		if ((r=io.write(hdrbuf,0,hdrbuf.length))<0) {setState(0);return r;}	
		if ((r=io.write(b))<0) {setState(0);return r;}
		io.sync();
		return b.length();
	}
	
	private void parseHdr() throws IOException{
		DataInputStream d=new DataInputStream(new ByteArrayInputStream(hdrbuf));
		cmd=d.readUnsignedShort();
		datalen=d.readUnsignedShort();
		cid=-1; sid=-1;
		dbrtype=-1; dbrcnt=-1;
		wrstatus=-1;
		if (cmd==CA_PROTO_VERSION){
			srver=d.readInt();
		}
		else if (cmd==CA_PROTO_CLIENT_NAME){}
		else if (cmd==CA_PROTO_HOST_NAME){}
		else if (cmd==CA_PROTO_ECHO){}
		else if (cmd==CA_PROTO_ACCESS_RIGHTS){
			d.readInt();//should be zero
			cid=d.readInt();			
			rights=d.readInt();
		}
		else if (cmd==CA_PROTO_CREATE_CHAN){
			dbrtype=d.readShort();
			dbrcnt=d.readShort();
			cid=d.readInt();
			sid=d.readInt();
		}
		else if (cmd==CA_PROTO_READ_NOTIFY){
			dbrtype=d.readShort();
			dbrcnt=d.readShort();
			d.readInt();//sid
			cid=d.readInt();//ioid			
		}
		else if (cmd==CA_PROTO_WRITE_NOTIFY){
			dbrtype=d.readShort();
			dbrcnt=d.readShort();
			wrstatus=d.readInt();//status
			cid=d.readInt();//ioid	
		}
		else if (cmd==CA_PROTO_CREATE_CH_FAIL){
			log.error("create channel fail");
			d.readShort();//must be 0
			d.readShort();//must be 0
			d.readInt();//must be 0
			cid=d.readInt();
		}
		else log.error("recv unknown cmd=%02X, hdr=%s",cmd,StrUtil.hex(hdrbuf));
		d=null;
	}
	private void buildHdr() throws IOException{
		ByteArrayOutputStream out=new ByteArrayOutputStream(16);
		DataOutputStream d=new DataOutputStream(out);
		if ((datalen&0x7)!=0){
			datalen=(datalen+7)/8;
			datalen*=8;			
		}
		d.writeShort(cmd);
		d.writeShort(datalen);
		if (cmd==CA_PROTO_VERSION){
			d.writeInt(CA_VERSION);
		}
		else if (cmd==CA_PROTO_CLIENT_NAME){}
		else if (cmd==CA_PROTO_HOST_NAME){}
		else if (cmd==CA_PROTO_ECHO){}
		else if (cmd==CA_PROTO_CREATE_CHAN){
			//0000 0000 00000001 0000000D
			d.writeShort(0);
			d.writeShort(0);
			d.writeInt(cid);
			d.writeInt(CA_VERSION);
		}
		else if (cmd==CA_PROTO_CLEAR_CHANNEL){
			d.writeShort(0);
			d.writeShort(0);
			d.writeInt(sid);
			d.writeInt(cid);
		}
		else if (cmd==CA_PROTO_READ_NOTIFY){
			//0003 0001 00000468 00000001
			d.writeShort(dbrtype);
			d.writeShort(dbrcnt);
			d.writeInt(sid);
			d.writeInt(cid);//as ioid
		}
		else if (cmd==CA_PROTO_WRITE_NOTIFY){
			d.writeShort(dbrtype);
			d.writeShort(dbrcnt);
			d.writeInt(sid);
			d.writeInt(cid);//as ioid
		}
		else log.error("unknown cmd=%02X",cmd);
		while (out.size()<hdrbuf.length) out.write(0);
		System.arraycopy(out.toByteArray(),0,hdrbuf,0,hdrbuf.length);
	}
}
