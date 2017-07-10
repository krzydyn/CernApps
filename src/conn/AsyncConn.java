package conn;

import java.util.ArrayList;
import java.util.List;

import sys.Errno;

import com.link.AbstractLink;

public abstract class AsyncConn extends AbstrConn {
	private final List<Object> repeat=new ArrayList<Object>();
	public AsyncConn() {}
	public AsyncConn(AbstractLink l) {super(l);}

	protected void repeat(Object o) {this.repeat.add(o);}
	abstract protected int prepare(Object m,StringBuilder bi);
	abstract protected void dispatch(StringBuilder bi);

	@Override
	protected void loop() throws Exception {
		StringBuilder b=new StringBuilder();
		int r;
		log.debug("entering async loop");
		for (;!thread.isInterrupted();) {
			if ((r=link.recv(b))<0) {
				if (r==-Errno.EAGAIN) {
					Object m=msgq.get(100);
					if (m==null) {link.idle();continue;}
					long tm=System.currentTimeMillis()+2000;
					for (r=0; ; ++r) {
						b.setLength(0);
						if (prepare(m,b)>=0)
							if (link.send(b)<0) break;
						if (msgq.isEmpty()) break;
						if (tm<System.currentTimeMillis()) break;
						m=msgq.get();
					}
					if (!repeat.isEmpty()) {
						while (!repeat.isEmpty()) msgq.add(repeat.remove(0));
					}
					continue;
				}
				log.error("link.recv=%d",r);
				break;
			}
			//DataInputStream bi=new DataInputStream(new ByteInputStream(b));
			dispatch(b);
		}
	}
}
