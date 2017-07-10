package plot;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import sys.Const;
import sys.ui.UiUtils;

@SuppressWarnings("serial")
public class PlotTime extends PlotXY {
	final static SimpleDateFormat dfSecond=new SimpleDateFormat("H:mm:ss");
	final static SimpleDateFormat dfMinute=new SimpleDateFormat("H:mm");
	final static SimpleDateFormat dfDay=new SimpleDateFormat("MM-dd");
	final static SimpleDateFormat dfMonth=new SimpleDateFormat("yyyy-MM-dd");
	final static SimpleDateFormat dfFull=new SimpleDateFormat("yyyy-MM-dd H:mm");
	final static long zoneoffs=TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000;

	//private long year;
	private long tm0;
	private boolean needSort=false;
	public PlotTime() {
		tm0=System.currentTimeMillis()/1000;
		//year=tm0/365/24/3600;
		//year*=365*24*3600;
		setTM0();
	}
	public void setTM0() {
		tm0/=24*3600*7;
		tm0*=24*3600*7;//start of week
		//offset from UTC to local time
		//tm0-=TimeZone.getDefault().getOffset(tm0*1000)/1000;
		tm0-=zoneoffs;
		tm0+=3*24*3600; //weekend on Sat/Sun
	}
	public double time2double(long tm) { return (tm-tm0)/3600.0; }
	public long double2time(double x) { return (long)(x*3600.0)+tm0; }
	synchronized public void addPoint(long tm,double v) {
		addPoint(0, tm, v);
	}
	@Override
	public void addPoints(int chn,List<Point2D> plist) {
		if (plist.size() == 0) return;
		List<Point2D> pnts=getPoints(chn);
		if (pnts.size() > 0) {
			if (plist.get(0).getX() < pnts.get(pnts.size()-1).getX())
				needSort=true;
		}
		super.addPoints(chn,plist);
	}
	public void addPoint(int chn,long tm,double v) {
		double x=time2double(tm);
		List<Point2D> pnts=getPoints(chn);
		if (pnts!=null && pnts.size()>0) {
			if (x<pnts.get(pnts.size()-1).getX()) needSort=true;
		}
		super.addPoint(chn,x,v);
	}
	@Override
	public void paintComponent(Graphics g){
		sortPoints();
		super.paintComponent(g);
	}
	public void sortPoints() {
		if (needSort) {
			//log.debug("sorting ...");
			List<Point2D> pnts;
			for (int i=0; (pnts=getPoints(i))!=null; ++i) {
				synchronized (pnts){
					Collections.sort(pnts,new Point2DComparator());
				}
			}
			needSort=false;
		}
	}
	public long getCaretTime() {
		return double2time(getCaret().getX());
	}
	@Override
	public double gridX(double x) {
		double c=0.5;
		//NOTE set a year has 365.25 days because of leap year every 4 years
		double[] g={1.0/60,1.0/12,1.0/6.0,0.25,0.5,1.0,2.0,4.0,6.0,12.0,24.0,
				//week, 2weeks, 4weeks, ~year
				7*24.0,14*24.0,28*24.0,365.25*24.0};
		x=Math.abs(x);
		if (x<g[0]*c) return grid(x);
		for (int i=0; i<g.length-1; ++i)
			if (x<g[i]*(1-c)+g[i+1]*c) return g[i];
		if (x<g[g.length-1]*c) return g[g.length-1];
		return grid(x);
	}
	@Override
	public String formatX(double v,double grid) {
		long tm=double2time(v);
		Date d=new Date(tm*1000);
		if (grid<=0){
			d.setTime(d.getTime()+30*1000);
			return dfFull.format(d);
		}
		if (grid<1.0/60.0) return dfSecond.format(d);
		d.setTime(d.getTime()+30*1000);
		if (grid<24) return dfMinute.format(d);
		if (grid<60*24) return dfDay.format(d);
		return dfMonth.format(d);
	}
	@Override
	public double parseX(String txt) throws ParseException {
		String[] dt;
		Date d=new Date(double2time(view.getMinX())*1000);
		dt=dfFull.format(d).split("[- ]");
		if (txt.matches("[0-9]{1,2}:[0-9]{2}")) txt=dt[0]+"-"+dt[1]+"-"+dt[2]+" "+txt;
		else if (txt.matches("[0-9]{2}-[0-9]{2}")) txt=dt[0]+"-"+txt+" 0:00";
		else if (txt.matches("[0-9]{4}-[0-9]{1,2}")) txt=txt+"-01 0:00";
		else if (txt.matches("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}")) txt=txt+" 0:00";
		else if (txt.matches("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}")) ;
		else {
			log.warn("'%s' not matches any supported format",txt);
			throw new ParseException("wrong time format "+txt,0);
		}
		d=dfFull.parse(txt);
		return time2double(d.getTime()/1000);
	}
	@Override
	protected PropertiesPanel createPropertiesPanel(){
		return new PropertiesPanel(xUnit,yUnit);
	}

	public static class PropertiesPanel extends PlotXY.PropertiesPanel{
		protected JTextField xrng;
		protected JPanel xrngProp,xfrtoProp;
		protected JRadioButton xrngb,xfrtob;
		public PropertiesPanel(String xunit,String yunit){
			super(xunit, yunit);
			check();
		}
		@Override
		public void addNotify(){
			super.addNotify();
			xrng.requestFocusInWindow();
		}
		@Override
		protected void buildXPropsUI(JPanel p,GridBagConstraints constr){
			ButtonGroup group;
			JRadioButton rb;
			group = new ButtonGroup();
			p.add(rb=new JRadioButton("width[hrs]",true),constr);
			group.add(rb);
			rb.setActionCommand("xrng");
			rb.addActionListener(this); xrngb=rb;
			xrngProp=new JPanel();
			xrngProp.add(xrng=new JTextField(5));
			p.add(xrngProp,constr);
			constr.gridwidth = GridBagConstraints.REMAINDER;
			p.add(new JLabel(),constr);
			constr.gridwidth = 1;
			p.add(rb=new JRadioButton("range"),constr);
			group.add(rb);
			rb.setActionCommand("xfrto");
			rb.addActionListener(this); xfrtob=rb;
			xfrtoProp=new JPanel();
			xfrtoProp.add(xmin=new JTextField(9));
			xfrtoProp.add(xmax=new JTextField(9));
			constr.gridwidth = GridBagConstraints.REMAINDER;
			p.add(xfrtoProp,constr);
			//xfrtob.setEnabled(false);
		}

		public void setXRange(double rng) {
			if (xrng!=null) xrng.setText(PlotXY.format(rng));
		}
		public double getXRange() {
			if (xrng==null) return Double.NaN;
			return Double.parseDouble(xrng.getText());
		}
		@Override
		public void setProperties(PlotXY obj){
			super.setProperties(obj);
			Rectangle2D r=obj.getSelection();
			if (r.getWidth()<Const.eps || r.getHeight()<Const.eps)
				r=obj.getView();

			if (xrng!=null){
				xrng.setText(PlotXY.format(r.getWidth()));
				//if (obj.autoBounds.x==AUTOBOUNDS_MOVE)
				//	xfrtob.setEnabled(false);
				//else
				//	xfrtob.setEnabled(true);
			}
		}
		@Override
		public void getProperties(PlotXY obj) {
			Rectangle2D.Double r=new Rectangle2D.Double();
			if (xrngb.isSelected()) {
				r.width=Double.parseDouble(xrng.getText());
				if (Math.abs(r.width-obj.getView().getWidth())>1e-10){
					if (obj.autoBounds.x==AUTOBOUNDS_MOVE) r.x=obj.getView().getMaxX()-r.width;
					else r.x=obj.getCaret().getX()-r.width/2;
				}
				else {r.x=obj.getView().getMinX();r.width=obj.getView().getWidth();}
				xmin.setText(obj.formatX(r.getMinX(),0));
				xmax.setText(obj.formatX(r.getMaxX(),0));
			}
			super.getProperties(obj);
		}
		private void check(){
			if (xrngb!=null && xrngb.isSelected()){
				UiUtils.setGroupEnabled(xfrtoProp,false);
				UiUtils.setGroupEnabled(xrngProp,true);
			}
			else if (xfrtob!=null && xfrtob.isSelected()){
				UiUtils.setGroupEnabled(xrngProp,false);
				UiUtils.setGroupEnabled(xfrtoProp,true);
			}
		}
		@Override
		public void actionPerformed(ActionEvent ev) {
			final String cmd=ev.getActionCommand();
			if ("xrng".equals(cmd) || "xfrto".equals(cmd))
				check();
			else super.actionPerformed(ev);
		}
	}

	static class Point2DComparator implements Comparator<Point2D>{
		@Override
		public int compare(Point2D o1, Point2D o2) {
			return (int)Math.signum(o1.getX()-o2.getX());
		}
	}

}
