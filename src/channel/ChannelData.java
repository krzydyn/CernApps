package channel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChannelData {
	public ChannelDef def;
	public long tm;
	public float[] value=new float[1];
	public int elems=0;
	public ChannelData(ChannelDef def){this.def=def;}
	public boolean equals(Object o){
		if (o instanceof ChannelData) {
			ChannelData c=(ChannelData)o;
			return def.equals(c.def) && tm==c.tm;
		}
		return false;
	}
	public int hashCode() {
		return def.hashCode()+(int)tm;
	}

	public void setValue(float v){value[0]=v;elems=1;}
	public float getValue(){return elems>0 ? value[0] : Float.NaN;}
	public void read(DataInputStream ds) throws IOException {
		tm=ds.readLong();
		setValue(ds.readFloat());
	}
	public void write(DataOutputStream ds) throws IOException {
		ds.writeLong(tm);
		ds.writeFloat(getValue());
	}
}
