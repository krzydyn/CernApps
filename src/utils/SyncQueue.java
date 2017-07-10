package utils;

import java.util.ArrayList;
import java.util.List;

import sys.SysUtil;

public class SyncQueue<T> {
	private final List<T> msgq=new ArrayList<T>();
	private boolean dbg=false;
	public void setDebug(boolean dbg){this.dbg=dbg;}
	public int size() { return msgq.size(); }
	public boolean isEmpty() { return msgq.isEmpty(); }
	public void add(T msg) { put(msg); }
	public void put(T msg) {
		int s;
		synchronized (msgq){
			msgq.add(msg); s=msgq.size();
			msgq.notifyAll();
		}
		if (dbg) SysUtil.log.info("sq=[%d] put '%s'",s,msg.toString());
	}
	public T get(Object key){
		synchronized (msgq){
			for (int i=0; i<msgq.size(); ++i){
				if (msgq.get(i).equals(key))
					return msgq.remove(i);
			}
		}
		return null;
	}
	public List<T> getAll() throws InterruptedException {
		List<T> l=new ArrayList<T>();
		synchronized (msgq){
			if (msgq.isEmpty()) msgq.wait();
			l.addAll(msgq);
			msgq.clear();
		}
		return l;
	}
	public T get() throws InterruptedException {
		return get(0);
	}
	public T get(long t) throws InterruptedException {
		T msg;
		int s;
		synchronized (msgq){
			if (msgq.isEmpty()) msgq.wait(t);
			if (msgq.isEmpty()) return null;
			msg=msgq.remove(0);
			s=msgq.size();
		}
		if (dbg) SysUtil.log.info("sq=[%d] get %s",s,msg.toString());
		return msg;
	}
	public T pop() throws InterruptedException {
		T msg;
		int s;
		synchronized (msgq){
			if (msgq.isEmpty()) msgq.wait();
			if (msgq.isEmpty()) return null;
			s=msgq.size()-1;
			msg=msgq.remove(s);
		}
		if (dbg) SysUtil.log.info("sq=[%d] get %s",s,msg.toString());
		return msg;
	}
	public void clear() {
		synchronized (msgq){
			msgq.clear();
			msgq.notifyAll();
		}
	}
}
