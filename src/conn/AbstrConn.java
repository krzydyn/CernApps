package conn;

import java.io.IOException;

import common.Logger;
import common.SysUtil;
import common.connection.Connection;
import common.connection.link.AbstractLink;
import common.connection.link.TCP;
import common.util.SyncQueue;

public abstract class AbstrConn {
	static protected Logger log=Logger.getLogger();

	public static interface ConnectorListener {
		public void connected();
		public void disconnected();
		public void exception(Exception e);
		
		public void execDone(int id); //synchronous command execution with donw
		public void readDone(int r,String pv,float[] v); //async read done
		public void writeDone(int r,String pv); //async write done
	}

	protected ConnectorListener listener;

	protected String addr;
	protected final SyncQueue<Object> msgq=new SyncQueue<Object>();
	
	protected Thread thread;
	private boolean stopped=true;
	final protected AbstractLink link;
	protected boolean persistent=true; //persistent is opened once, never closed
	
	private final Runnable runloop=new Runnable(){
		public void run(){
			log.info("connector loop started");
			while (!stopped) {
				try{
					connect();
				}
				catch (IOException e) {
					disconnected();
					if (listener!=null) listener.disconnected();
					if (!persistent) break;
					SysUtil.delay(60*1000);
					continue;
				}

				connected();
				if (listener!=null) listener.connected();
				
				try{
					loop();
					log.debug("normal loop end");
				}
				catch (InterruptedException e) {
					log.debug("loop end by thread interrupt");
				}
				catch (Exception e) {
					log.error(e,"loop end by exception");
					if (!stopped && listener!=null) listener.exception(e);	
				}
				catch (Throwable e) {
					log.error(e);
				}
				finally{
					if (!persistent) stopped=true;
					msgq.clear();
					disconnect();
					disconnected();
					if (listener!=null) listener.disconnected();
				}
			}
			thread=null;
		}
	};
	
	protected AbstrConn() {
		this(new TCP());
	}
	protected AbstrConn(AbstractLink link) {
		this.link=link;
	}
	public AbstractLink getLink() {return link;}
	public boolean isStopped(){return stopped;}
	
	public void setAddr(String addr) {
		if (addr.indexOf(':')<0) addr+=":"+defaultPort();
		this.addr=addr;
	}
	public void setConnectorListener(ConnectorListener l) {listener=l;}
	public String getName(){return "AbstrConnector";}
	public void start(){
		if (thread!=null) {
			log.info("thread already started");
			return ;
		}
		msgq.clear();
		log.info("starting connector with addr=%s",addr);
		stopped=false;
		thread=new Thread(runloop);
		thread.start();
	}
	public void stop(){
		stopped=true;
		if (thread!=null) thread.interrupt();
		msgq.clear();
		for (int i=0; i<10 && thread!=null; ++i) SysUtil.delay(100);
	}
	public void clearq(){msgq.clear();}

	protected void connected(){}
	protected void disconnected(){}
	
	protected void connect() throws IOException {
		Connection io=Connection.getConnection(addr);
		int r;
		io.setChrTmo(500);
		io.connect();
		io.flush();
		link.setIO(io);
		if ((r=link.open())<0) throw new IOException("link.open = "+r);
	}
	
	protected boolean isConnected() {
		Connection io=(Connection)link.getIO();
		if (io!=null) return io.isConnected();
		return false;
	}
	
	protected void disconnect(){
		Connection io=(Connection)link.getIO();
		link.close();
		if (io!=null) io.disconnect();
	}
	
	//abstract methods, needs specialization
	abstract protected void loop() throws Exception;
	abstract protected int defaultPort();
}
