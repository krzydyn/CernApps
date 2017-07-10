package conn;

import com.link.AbstractLink;

public abstract class SyncConn extends AbstrConn {

	public SyncConn() {}
	public SyncConn(AbstractLink link) {super(link);}

	public static abstract class Command{
		final private int id;
		public Command(int id){this.id=id;}
		public Command() {this.id=0;}
		abstract protected int execute();
	}

	protected void request(Command r){
		if (!isStopped()) msgq.put(r);
		else log.error("Connector is stopped");
	}
	protected void idle(){}
	@Override
	protected void loop() throws Exception {
		for (;!thread.isInterrupted();) {
			Command fn = (Command)msgq.get(2000);
			if (fn==null) {
				idle();
				if (!persistent && msgq.isEmpty()){
					if (isConnected()) disconnect();
				}
				continue;
			}

			if (!isConnected()) connect();
			while (fn != null) {
				//log.debug("perform id=%d",fn.id);
				int r=fn.execute();
				if (r<0) break;
				if (listener!=null)listener.execDone(fn.id);
				if (msgq.isEmpty()) break;
				fn=(Command)msgq.get();
			}
		}
	}
}
