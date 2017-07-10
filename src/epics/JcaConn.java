package epics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Set;

import com.io.ByteInputStream;
import com.io.ByteOutputStream;

import sys.StrUtil;
import channel.ChannelData;
import conn.AsyncConn;

public class JcaConn extends AsyncConn {
	final static int DEFAULT_PORT=5064;
	final static long IO_TIMEOUT=2500;
	final static int DBR_STRING=0;//40byte string
	final static int DBR_INT=1;//2byte
	final static int DBR_FLOAT=2;//2byte
	final static int DBR_ENUM=3;
	final static int DBR_CHAR=4;//1byte
	final static int DBR_LONG=5;//4byte
	final static int DBR_DOUBLE=6;//4byte
	final static int DBR_STS_STRING=7;

	protected static class JCAChannel{
		final static int STATE_CLEAN=0;
		final static int STATE_WAIT=1;
		final static int STATE_READY=2;
		final static int STATE_NOTEXIST=3;
		public JCAChannel(String pv,int cid) {
			this.pv=pv; this.cid=cid;
		}
		private int state=STATE_CLEAN;
		private long reqtmo=0;
		public final String pv;//real pv
		public final int cid;
		public int sid=-1;
		public int dbrtype=-1;
		public int dbrcnt=0;
		public int rights=0;
		public long tm;
	}
	private static class Msg {
		final static int CA_INIT=0;
		final static int PV_CREATE=1;
		final static int PV_CLEAR=2;
		final static int PV_READ=3;
		final static int PV_WRITE=4;
		Msg(int cmd,String pv) {this.cmd=cmd;this.pv=pv;}
		int cmd;
		String pv;
		Object data;
	}
	private int cid_counter=0;
	private final HashMap<String,JCAChannel> pvmap=new HashMap<String,JCAChannel>();
	private final HashMap<Integer,JCAChannel> cidmap=new HashMap<Integer,JCAChannel>();

	public JcaConn() {
		super(new JcaLink());
	}
	@Override
	public int defaultPort(){return DEFAULT_PORT;}

	public Set<String> getChannels() { return pvmap.keySet(); }

	public void sendInit(String user){
		if (isStopped()) return ;
		Msg m=new Msg(Msg.CA_INIT,null);
		m.data=user;
		msgq.add(m);
	}
	public void createPv(String pv){
		if (isStopped()) return ;
		if (!pvmap.containsKey(pv))
			msgq.add(new Msg(Msg.PV_CREATE,pv));
	}
	public void clearPv(String pv){
		if (isStopped()) return ;
		if (!pvmap.containsKey(pv)) return ;
		msgq.add(new Msg(Msg.PV_CLEAR,pv));
	}
	public void readPv(String pv){
		if (isStopped()) return ;
		msgq.add(new Msg(Msg.PV_READ,pv));
	}
	public void writePv(String pv,float value){
		if (isStopped()) return ;
		Msg m=new Msg(Msg.PV_WRITE,pv);
		float[] v=new float[1];
		v[0]=value;
		m.data=v;
		msgq.add(m);
	}

	public void readChn(ChannelData c) {
		if (c.def.name==null) throw new NullPointerException("channel def.name");
		log.debug("readChn(%s)",c.def.name);
		if (c.def.pv != null) readPv(c.def.name);
		else readPv(c.def.name);
	}
	public void writeChn(ChannelData c) {
		if (c.def.name==null) throw new NullPointerException("channel def.name");
		log.debug("readChn(%s)",c.def.name);
		if (c.def.pv != null) writePv(c.def.name, c.getValue());
		else writePv(c.def.name, c.getValue());
	}

	@Override
	protected void disconnected() {
		pvmap.clear();
		cidmap.clear();
		cid_counter=0;
	}

	final protected JCAChannel getChannel(String pv) {
		if (pv==null) return null;
		JCAChannel ci=pvmap.get(pv);
		if (ci==null){
			if (cid_counter==0xffff) cid_counter=0;
			++cid_counter;
			//log.debug("Creating new JCAChnannel(%s,%d)",pv,cid_counter);
			ci=new JCAChannel(pv,cid_counter);
			pvmap.put(pv,ci);
			cidmap.put(ci.cid,ci);
		}
		return ci;
	}
	private void sendInit(String u,StringBuilder buf) {
		JcaLink link=(JcaLink)this.link;

		link.setCmd(JcaLink.CA_PROTO_VERSION);
		buf.setLength(0);

		if (u!=null) {
			link.send(buf);

			String[] uh=u.split("@", 2);
			link.setCmd(JcaLink.CA_PROTO_CLIENT_NAME);
			buf.setLength(0);buf.append(uh[0]);
			link.send(buf);

			link.setCmd(JcaLink.CA_PROTO_HOST_NAME);
			buf.setLength(0);buf.append(uh[1]);
			//link.send(buf);
		}
	}
	private void createPv(JCAChannel ci,StringBuilder buf){
		if (ci.state!=JCAChannel.STATE_CLEAN) {
			return ;
		}
		ci.state=JCAChannel.STATE_WAIT;
		ci.reqtmo=System.currentTimeMillis()+IO_TIMEOUT;
		JcaLink link=(JcaLink)this.link;
		link.setCmd(JcaLink.CA_PROTO_CREATE_CHAN);
		link.setCid(ci.cid);
		buf.setLength(0);
		buf.append(ci.pv);
		buf.append((char)0);//EPICS want '\0' terminated string
	}
	private void clearPv(JCAChannel ci,StringBuilder buf){
		if (ci.state!=JCAChannel.STATE_CLEAN) {
			return ;
		}
		ci.state=JCAChannel.STATE_WAIT;
		ci.reqtmo=System.currentTimeMillis()+IO_TIMEOUT;
		JcaLink link=(JcaLink)this.link;
		link.setCmd(JcaLink.CA_PROTO_CLEAR_CHANNEL);
		link.setSid(ci.sid);
		link.setCid(ci.cid);
		buf.setLength(0);
		buf.append(ci.pv);
		buf.append((char)0);//EPICS want '\0' terminated string
	}
	private int readPv(JCAChannel ci,StringBuilder buf){
		if (ci.state==JCAChannel.STATE_NOTEXIST) return -1;
		long tm=System.currentTimeMillis();
		if (ci.state!=JCAChannel.STATE_READY) {
			if (ci.reqtmo<tm) {
				log.debug("chn(%s) timeout st=%d tmo=%d",ci.pv,ci.state,tm-ci.reqtmo);
				ci.state=JCAChannel.STATE_NOTEXIST;
				return -1;
			}
			log.debug("chn(%s) not ready, st=%d",ci.pv,ci.state);
			return -1; //don't send
		}
		if (ci.sid<0) {
			log.error("chn(%s) has no sid",ci.pv);
			return -1;
		}
		ci.reqtmo=tm+IO_TIMEOUT;
		JcaLink link=(JcaLink)this.link;
		link.setCmd(JcaLink.CA_PROTO_READ_NOTIFY);
		link.setDbrtype(ci.dbrtype);
		link.setDbrcnt(ci.dbrcnt);
		link.setSid(ci.sid);
		link.setCid(ci.cid);
		buf.setLength(0);//data
		return 1;
	}
	private int writePv(JCAChannel ci,StringBuilder buf){
		if (ci.state!=JCAChannel.STATE_READY) {
			log.debug("chn(%s) not ready yet",ci.pv);
			return 0;
		}
		if (ci.sid<0) {
			log.error("chn(%s) has no sid",ci.pv);
			return -1;
		}
		ci.reqtmo=System.currentTimeMillis()+IO_TIMEOUT;
		JcaLink link=(JcaLink)this.link;
		link.setCmd(JcaLink.CA_PROTO_WRITE_NOTIFY);
		link.setDbrtype(ci.dbrtype);
		link.setDbrcnt(ci.dbrcnt);
		link.setSid(ci.sid);
		link.setCid(ci.cid);
		return 1;
	}

	@Override
	protected int prepare(Object m,StringBuilder buf) {
		Msg msg=(Msg)m;
		JCAChannel ci=getChannel(msg.pv);
		int r=-1;
		if (msg.cmd==Msg.CA_INIT) {
			sendInit((String)msg.data,buf);
			return 1;
		}
		else if (ci==null) {
			return -1;
		}
		else if (ci.state==JCAChannel.STATE_CLEAN) {
			createPv(ci, buf);
			if (msg.cmd!=Msg.PV_CREATE) repeat(m);
			return 0;
		}
		else if (msg.cmd==Msg.PV_CREATE) {
			createPv(ci,buf);
		}
		else if (msg.cmd==Msg.PV_CLEAR) {
			clearPv(ci,buf);
		}
		else if (msg.cmd==Msg.PV_READ) {
			if ((r=readPv(ci,buf))==0) repeat(m);
			if (r<0) {
				if (listener!=null) listener.readDone(r, ci.pv, null);
			}
		}
		else if (msg.cmd==Msg.PV_WRITE) {
			if ((r=writePv(ci,buf))==0) repeat(m);
			if (r<0) {
				if (listener!=null) listener.writeDone(r, ci.pv);
			}
			buildData(ci,JcaLink.CA_PROTO_WRITE_NOTIFY,(float[])msg.data,buf);
			log.debug("write pv: %s=%s",ci.pv,StrUtil.vis(buf));
		}
		else return -1;
		return r;
	}

	private void buildData(JCAChannel ci,int cmd, float[] v,StringBuilder buf) {
		buf.setLength(0);
		if (ci==null||ci.sid==0) return ;
		DataOutputStream d=new DataOutputStream(new ByteOutputStream(buf));
		try{
		if (cmd==JcaLink.CA_PROTO_WRITE_NOTIFY){
			for (int i=0; i<v.length &&i<ci.dbrcnt; ++i){
				if (ci.dbrtype==DBR_INT) d.writeShort((int)v[i]);
				else if (ci.dbrtype==DBR_FLOAT)d.writeFloat(v[i]);
				else if (ci.dbrtype==DBR_ENUM) d.writeShort((int)v[i]);
				else if (ci.dbrtype==DBR_LONG) d.writeInt((int)v[i]);
				else if (ci.dbrtype==DBR_DOUBLE)d.writeDouble(v[i]);
				else  {
					log.error("err build: pv=%s, dbrtype=%d",ci.pv,ci.dbrtype);
				}
			}
		}
		d.close();
		} catch (Exception e) {}
	}
	private void parseData(JCAChannel ci,int cmd,StringBuilder buf) {
		if (buf.length()==0) return ;
		//log.debug("parse cmd=%d dtp=%d cnt=%d: %s",cmd,ci.dbrtype,ci.dbrcnt,StrUtil.hex(buf));
		DataInputStream d=new DataInputStream(new ByteInputStream(buf));
		try{
		if (cmd==JcaLink.CA_PROTO_READ_NOTIFY){
			float[] v=new float[ci.dbrcnt];
			int r=0;
			for (int i=0; i<ci.dbrcnt; ++i){
				if (ci.dbrtype==DBR_INT)        v[i]=d.readShort();
				else if (ci.dbrtype==DBR_FLOAT) v[i]=d.readFloat();
				else if (ci.dbrtype==DBR_ENUM)  v[i]=d.readShort();
				else if (ci.dbrtype==DBR_LONG)  {
					v[i]=d.readInt();
					d.readInt();
				}
				else if (ci.dbrtype==DBR_DOUBLE){
					v[i]=(float)d.readDouble();
				}
				else {
					r=-1;
					log.error("err parse: pv=%s, dbrtype=%d, buf=%s",ci.pv,ci.dbrtype,StrUtil.vis(buf));
					v[i]=Float.NaN;
				}
				//log.debug("pv=%s v[%d]=%f",ci.pv,c.elems,v);
				//c.value[c.elems++]=v;
			}
			if (d.available()>0) {
				int l=d.available();
				log.warn("pv=%s dbt=%d dbn=%d: not all data processed  left[%d]: %s",
						ci.pv,ci.dbrtype,ci.dbrcnt,l,StrUtil.vis(buf,buf.length()-l,l));
			}
			if (listener!=null) listener.readDone(r,ci.pv,v);
		}
		d.close();
		}catch (Throwable e){}
	}

	@Override
	protected void dispatch(StringBuilder buf) {
		JcaLink link=(JcaLink)this.link;
		JCAChannel ci=cidmap.get(link.getCid());
		int r=link.getCmd();
		//log.debug("dispatch cmd=%d",r);
		if (r==JcaLink.CA_PROTO_VERSION){
			//received immediately after connection form server
			log.info("Server version=%d",link.getHostVersion());
		}
		else if (r==JcaLink.CA_PROTO_ACCESS_RIGHTS){
			if (ci==null){
				log.debug("CID %d not requested");
			}
			ci.rights=link.getRights();
			//log.debug("Rights pv=%s cid=%d sid=%X rth=%X",ci.pv,ci.cid,ci.sid,ci.rights);
		}
		else if (r==JcaLink.CA_PROTO_CREATE_CHAN){
			ci.dbrtype=link.getDbrType();
			ci.dbrcnt=link.getDbrCCount();
			ci.sid=link.getSid();
			log.debug("Created pv=%s dbrtype=%d dbrdnt=%d cid=%d sid=%X",ci.pv,ci.dbrtype,ci.dbrcnt,ci.cid,ci.sid);
			ci.state=JCAChannel.STATE_READY;
			ci.reqtmo=0;
		}
		else if (r==JcaLink.CA_PROTO_CLEAR_CHANNEL){
			pvmap.remove(ci.pv);
			cidmap.remove(ci.cid);
			ci.state=JCAChannel.STATE_CLEAN;
			ci.sid=-1;
		}
		else if (r==JcaLink.CA_PROTO_READ_NOTIFY){
			if (ci==null){
				log.debug("CID %d not requested");
				return;
			}
			ci.reqtmo=0;
			parseData(ci, r, buf);
		}
		else if (r==JcaLink.CA_PROTO_WRITE_NOTIFY){
			ci=cidmap.get(link.getCid());
			if (ci==null){
				log.debug("CID %d not requested");
				return;
			}
			ci.reqtmo=0;
			if (listener!=null) listener.writeDone(link.getWrstatus(),ci.pv);
		}
		else log.debug("no dispach for cmd=%d",r);
	}
}
