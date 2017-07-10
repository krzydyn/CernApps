package plot;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sys.Const;
import sys.Logger;
import sys.StrUtil;
import sys.ui.CheckBoxIcon;
import sys.ui.DialogPanel;
import sys.ui.UiUtils;

@SuppressWarnings("serial")
public class PlotXY extends JPanel implements ActionListener {
	static final Logger log=Logger.getLogger();
	static final DecimalFormatSymbols decSymbols=new DecimalFormatSymbols(Locale.US);
	static final DecimalFormat fmtMAX=new DecimalFormat("0.######",decSymbols);
	static final DecimalFormat fmtINT3=new DecimalFormat("0k",decSymbols);
	//static final DecimalFormat fmtINT2=new DecimalFormat("0.0k",decSymbols);
	static final DecimalFormat fmtINT=new DecimalFormat("0",decSymbols);
	static final DecimalFormat fmtF1=new DecimalFormat("0.0",decSymbols);
	static final DecimalFormat fmtF2=new DecimalFormat("0.00",decSymbols);
	static final DecimalFormat fmtF3=new DecimalFormat("0.000",decSymbols);

	static final Font unitfont=UIManager.getFont("Panel.font").deriveFont(Font.BOLD,12f);
	static final Font gridfont=unitfont.deriveFont(Font.PLAIN,10);
	static final Stroke line1=new BasicStroke(1);
	static final Stroke line1dash=new BasicStroke(1,0,0,3,new float[]{3f,3f},0);
	static final Color SELCOLOR=new Color(0x60aaaaff,true);
	static final Stroke limitLine=new BasicStroke(2,0,0,3,new float[]{7f,5f},0);
	static final Color[] LIMITCOLORS={
			//new Color(0x60ffffff&Color.RED.getRGB(),true),
			//new Color(0x60ffffff&Color.YELLOW.getRGB(),true),
		Color.RED,
		Color.ORANGE,
	};

	static public final int AUTOBOUNDS_NONE=0;
	static public final int AUTOBOUNDS_SCALE=1;
	static public final int AUTOBOUNDS_MOVE=2;

	//plot mode
	static public final int PLOT_NONE=0;
	static public final int PLOT_X=0x01;
	static public final int PLOT_Y=0x02;
	static public final int PLOT_XY=0x03;

	//caret type (same as plot type +none)
	static public final int CARET_NONE=0;
	static public final int CARET_X=0x01;
	static public final int CARET_Y=0x02;
	static public final int CARET_XY=0x03;

	//selection type (same as plot type +none)
	static public final int SELECTION_NONE=0;
	static public final int SELECTION_X=0x01;
	static public final int SELECTION_Y=0x02;
	static public final int SELECTION_XY=0x03;

	static public final int NOCHANGE=0;
	static public final int CHANGED_X=0x01;
	static public final int CHANGED_Y=0x02;
	static public final int CHANGED_W=0x04;
	static public final int CHANGED_H=0x08;

	static final int SP=5;
	static final Dimension prefsz=new Dimension(240,120);
	public static final Color[] chnColor={
		Color.BLUE.darker().darker(),Color.BLUE.darker(),Color.BLUE,
		Color.CYAN.darker().darker(),Color.CYAN.darker(),Color.CYAN,
		Color.GREEN.darker().darker(),Color.GREEN.darker(),Color.GREEN,
		Color.ORANGE.darker(),Color.ORANGE,Color.ORANGE.brighter(),
		Color.RED.darker().darker(),Color.RED.darker(),Color.RED};
	public static final Color[] chnColorSimple={
		Color.YELLOW,Color.CYAN,Color.BLUE,Color.GREEN,Color.MAGENTA,Color.PINK
	};
	static final Border chkboxBorder=BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
	static private final Icon selicon=new CheckBoxIcon();

	private final JPopupMenu popup=new JPopupMenu("Choose option");
	private final JMenuItem autofit=new JMenuItem();
	private final JCheckBox chkbx=new JCheckBox(selicon);

	//plot params
	protected Insets ins=null;
	protected String xUnit="",yUnit="";
	protected Point autoBounds=new Point(AUTOBOUNDS_NONE,AUTOBOUNDS_NONE);
	protected int plotMode=PLOT_NONE;
	protected int selMode=SELECTION_X;
	protected int caretType=CARET_X;
	protected Color plotbg=Color.BLACK;
	protected Rectangle2D.Double view=new Rectangle2D.Double();
	protected Point2D caret=new Point2D.Double(Double.NaN,Double.NaN);
	protected Point2D.Double selStart=new Point2D.Double();
	protected Rectangle2D.Double selFrame=new Rectangle2D.Double();
	protected PlotListener listener=null;
	protected float[] lLimits;
	protected float[] uLimits;

	//plot data
	protected List<PlotChannel> chns=new ArrayList<PlotChannel>();
	protected int selChn=0; //TODO -1, no channels

	//internal
	protected Rectangle plotArea=new Rectangle();
	protected boolean recalcPlotArea;

	public PlotXY() {
		super(null);//no layout manager
		setEnabled(true);
		setFocusable(false);
		//setDoubleBuffered(true);

		chkbx.setBorderPainted(true);//default is false
		//chkbx.setBorder(BorderFactory.createRaisedBevelBorder());
		chkbx.setBorder(chkboxBorder);
		chkbx.setSize(chkbx.getPreferredSize());
		chkbx.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		chkbx.setToolTipText("Select this plot");
		chkbx.setFocusable(false);
		chkbx.setVisible(false);
		add(chkbx);

		JMenuItem mi;
		popup.add(mi=new JMenuItem("Properties..."));
		mi.setActionCommand("props");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,KeyEvent.ALT_DOWN_MASK));
		mi.addActionListener(this);
		popup.add(mi=autofit);
		mi.setActionCommand("autofit");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,KeyEvent.ALT_DOWN_MASK));
		mi.addActionListener(this);

		chns.add(new PlotChannel(0));
		recalcPlotArea=true;
		addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent ev){
				if (checkPopup(ev)) return ;
				if (ev.getButton()==MouseEvent.BUTTON1){
					if (ev.getClickCount()==1) setCaret(ev);
					else if (ev.getClickCount()==2) propsDialog();
				}
			}
			@Override
			public void mouseEntered(MouseEvent ev) { }
			@Override
			public void mouseExited(MouseEvent ev) { }
			@Override
			public void mousePressed(MouseEvent ev) {
				if (getParent().isFocusable()){
					getParent().requestFocus();
				}
				if (checkPopup(ev)) return ;
				if (ev.getButton()==MouseEvent.BUTTON1){
					if (selMode!=0 && plotArea.contains(ev.getX(),ev.getY())){
						selStart.x=ev.getX(); selStart.y=ev.getY();
						scr2view(selStart);
						selFrame.width=selFrame.height=0;
					}
				}
			}
			@Override
			public void mouseReleased(MouseEvent ev) {
				if (checkPopup(ev)) return ;
				if (ev.getButton()==MouseEvent.BUTTON1){
					if (selFrame.width>0 && selFrame.height>0){
						if (listener!=null) listener.selChanged(PlotXY.this);
					}
				}
			}
			boolean checkPopup(MouseEvent ev){
				if (!ev.isPopupTrigger()) return false;
				popupMenu(ev);
				ev.consume();
				return true;
			}
		});
		addMouseMotionListener(new MouseMotionListener(){
			@Override
			public void mouseDragged(MouseEvent ev) {
				if (ev.getModifiersEx()==MouseEvent.BUTTON1_DOWN_MASK){
					if (selMode!=0){
						Point2D p=new Point2D.Double();
						p.setLocation(ev.getPoint());
						scr2view(p);
						selFrame.setFrame(selStart.x,selStart.y,0,0);
						selFrame.add(p);
						repaint(500);
					}
				}
			}
			@Override
			public void mouseMoved(MouseEvent e) {}
		});
		addComponentListener(new ComponentListener(){
			@Override
			public void componentHidden(ComponentEvent e) {}
			@Override
			public void componentMoved(ComponentEvent e) { }
			@Override
			public void componentResized(ComponentEvent e){PlotXY.this.componentResized();}
			@Override
			public void componentShown(ComponentEvent e) {}
		});
		setMinimumSize(prefsz);
		setPreferredSize(prefsz);
		setPlotMode(PLOT_X);
	}
	static public Color[] getColors(){return chnColor;}
	public String getUnitY() {return yUnit;}
	public String getUnitX() {return xUnit;}
	public void setLimis(float[] l, float[] u) {
		lLimits=l;
		uLimits=u;
		repaint(500);
	}
	public void setAutoBounds(int modex,int modey){
		if (autoBounds.x==modex && modey==autoBounds.y) return ;
		autoBounds.x=modex; autoBounds.y=modey;
	}
	public int selectChannel(int chn) {
		if (getPoints(chn)!=null) {
			if (selChn==chn) return selChn;
			selChn=chn;
			if (caretType==CARET_X) {
				Point2D p=new Point2D.Double(caret.getX(),caret.getY());
				view2scr(p);
				MouseEvent ev=new MouseEvent(this, 0, System.currentTimeMillis(),
						0, (int)p.getX(), (int)p.getY(), 0, 0, 1, false, 1);
				setCaret(ev);
			}
		}
		return selChn;
	}
	public int getSelectedChannel() {return selChn;}
	public void setPlotMode(int m) {
		plotMode=m;
		if (m!=0){
			if (selMode!=0) selMode=m;
			if (caretType!=0) caretType=m;
		}
		if (plotMode==PLOT_X){
			autofit.setEnabled(true);
			autofit.setText("Autofit-Y");
			autofit.setActionCommand("autofit-y");
		}
		else if (plotMode==PLOT_Y){
			autofit.setEnabled(true);
			autofit.setText("Autofit-X");
			autofit.setActionCommand("autofit-x");
		}
		else {
			autofit.setEnabled(false);
			autofit.setText("Autofit");
		}

	}
	public int getPlotMode() {return plotMode;}
	public void setSelMode(int m) {selMode=m;}
	public int getSelMode() {return selMode;}
	public void setCheckVisible(boolean b){chkbx.setVisible(b);}
	public void setSelected(boolean b) {chkbx.setSelected(b);}
	public boolean isSelected() {return chkbx.isSelected();}
	public void addPopupItem(JMenuItem mi){ popup.add(mi); }
	protected void popupMenu(MouseEvent ev){
		popup.show(ev.getComponent(),ev.getX(),ev.getY());
	}
	@Override
	public void actionPerformed(ActionEvent ev){
		final String cmd=ev.getActionCommand();
		if ("props".equals(cmd)) propsDialog();
		else if ("autofit-y".equals(cmd)){
			autoFitY2();
		}
		else log.warn("%s: not handled '%s'",getClass().getName(),cmd);
	}
	protected void componentResized() {
		recalcPlotArea=true;
		chkbx.setLocation(0,getHeight()-chkbx.getHeight());
		repaint(500);
	}
	@Override
	public Insets getInsets(){
		if (ins==null) ins=super.getInsets();
		return ins;
	}
	public void setInsets(Insets i){
		Insets ins=getInsets();
		ins.bottom=i.bottom;
		ins.top=i.top;
		ins.left=i.left;
		ins.right=i.right;
		recalcPlotArea=true;
	}
	public void setCaretType(int c){caretType=c;}
	public void setPlotBackground(Color c){plotbg=c;repaint(500);}
	public Color getPlotBackground(){return plotbg;}
	public void clear() {
		synchronized (chns) {
			for (int i=0; i<chns.size(); ++i) {
				chns.get(i).clear();
			}
		}
		if (autoBounds.x==AUTOBOUNDS_SCALE) view.width=0;
		if (autoBounds.y==AUTOBOUNDS_SCALE) view.height=0;
	}
	public void clearAll() {
		synchronized (chns) {
			for (int i=0; i<chns.size(); ++i) {
				chns.get(i).clear();
			}
			if (chns.size()==0) chns.add(new PlotChannel(0));
			chns.get(0).param.color=chnColorSimple[0];
		}
		if (autoBounds.x==AUTOBOUNDS_SCALE) view.width=0;
		if (autoBounds.y==AUTOBOUNDS_SCALE) view.height=0;
		clearSelection();
	}
	public void clear(int chn) {
		synchronized (chns) {
		List<Point2D> pnts=getPoints(chn);
		if (pnts!=null) pnts.clear();
		}
	}
	public List<Point2D> getPoints(int chn) {
		return chn<chns.size()?chns.get(chn).pnts:null;
	}
	public ChannelParams getParams(int chn){
		while (chns.size()<=chn) chns.add(new PlotChannel(chns.size()));
		return chns.get(chn).param;
	}
	public void addPoints(int chn,List<Point2D> plist) {
		if (plist==null || plist.size()==0) return ;
		synchronized (chns) {
		List<Point2D> pnts=getPoints(chn);
		if (pnts==plist) return ;
		if (pnts==null) {
			while (chns.size()<=chn) chns.add(new PlotChannel(chns.size()));
			pnts=getPoints(chn);
		}
		double xmin,xmax,ymin,ymax,v;
		xmin=xmax=plist.get(0).getX();
		ymin=ymax=plist.get(0).getY();
		for (Point2D p:plist) {
			if (xmin>(v=p.getX())) xmin=v;
			else if (xmax<(v=p.getX())) xmax=v;
			if (ymin>(v=p.getY())) ymin=v;
			else if (ymax<(v=p.getY())) ymax=v;
		}
		pnts.addAll(plist);
		addView(pnts,xmin,ymin);
		addView(pnts,xmax,ymax);
		}
		repaint(500);
	}
	public void addPoints(int chn,float x0,float dx,float[] y,int n){
		synchronized (chns) {
		List<Point2D> pnts=getPoints(chn);
		if (pnts==null) {
			while (chns.size()<=chn) chns.add(new PlotChannel(chns.size()));
			pnts=getPoints(chn);
		}
		float ymin,ymax,v;
		ymin=ymax=y[0];
		for (int i=0; i<n; ++i){
			v=y[i];
			pnts.add(new Point2D.Float(x0+dx*i,v));
			if (ymin>v) ymin=v;
			else if (ymax<v) ymax=v;
		}
		//log.debug("x0=%.2f ymin=%.2f ymax==%.2f",x0,ymin,ymax);
		addView(pnts,x0,ymin);
		addView(pnts,x0+dx*n,ymax);
		}
		repaint(500);
	}
	public void addPoints(int ch1,int ch2,float x0,float dx,float[] y,int n){
		synchronized (chns) {
			int i=Math.max(ch1,ch2);
			List<Point2D> pnts1,pnts2;
			if ((pnts1=getPoints(i))==null) {
				while (chns.size()<=i) chns.add(new PlotChannel(chns.size()));
			}
			pnts1=getPoints(ch1);
			pnts2=getPoints(ch2);
			float ymin,ymax,v;
			ymin=ymax=y[0];
			for (i=0; i<n; i+=2){
				v=y[i];
				pnts1.add(new Point2D.Float(x0+dx*i,v));
				if (ymin>v) ymin=v;
				else if (ymax<v) ymax=v;
				v=y[i+1];
				pnts2.add(new Point2D.Float(x0+dx*i,v));
				if (ymin>v) ymin=v;
				else if (ymax<v) ymax=v;
			}
			//log.debug("x0=%.2f ymin=%.2f ymax==%.2f",x0,ymin,ymax);
			addView(pnts1,x0,ymin);
			addView(pnts1,x0+dx*n,ymax);
			}
			repaint(500);
	}
	public void addPoint(int chn,Point2D p) {
		List<Point2D> pnts;
		synchronized (chns) {
			pnts=getPoints(chn);
			if (pnts==null) {
				while (chns.size()<=chn) chns.add(new PlotChannel(chns.size()));
				pnts=getPoints(chn);
			}
		}
		if (!addView(pnts,p.getX(),p.getY())) return ;
		pnts.add(p);
		repaint(500);
	}
	public void addPoint(int chn,float x,float y){
		addPoint(chn,new Point2D.Float(x,y));
	}
	public void addPoint(int chn,double x,double y){
		addPoint(chn,new Point2D.Double(x,y));
	}
	public void addPoint(double x,double y) {addPoint(0, x, y);}

	public void setCaret(double x,double y) {
		caret.setLocation(x,y);
		if (listener!=null) listener.caretChanged(this);
		repaint(500);
	}
	public void setCaret(Point2D loc) {setCaret(loc.getX(),loc.getY());}
	public void clearSelection(){
		selFrame.width=selFrame.height=0;
		repaint(500);
	}
	private Point2D view2scr(Point2D p){
		double fx=0.0;
		if (view.width>Const.eps) fx=plotArea.width/view.width;
		double fy=0.0;
		if (view.height>Const.eps) fy=plotArea.height/view.height;
		p.setLocation(plotArea.x+(p.getX()-view.x)*fx,
			plotArea.y+plotArea.height-(p.getY()-view.y)*fy);
		return p;
	}
	private Point2D scr2view(Point2D p){
		double fx=view.width/plotArea.width;
		double fy=view.height/plotArea.height;
		p.setLocation(view.x+(p.getX()-plotArea.x)*fx,
			view.y+(plotArea.y+plotArea.height-p.getY())*fy);
		return p;
	}

	public Rectangle2D getSelection(){ return selFrame; }
	public void keyPressed(KeyEvent ev) {
		Rectangle2D r=getView();
		if (ev.getKeyCode()==KeyEvent.VK_MINUS||ev.getKeyCode()==KeyEvent.VK_SUBTRACT){
			setView(r.getX()-r.getWidth()*0.5,r.getY(),2*r.getWidth(),r.getHeight());
		}
		else if (ev.getKeyCode()==KeyEvent.VK_EQUALS||ev.getKeyCode()==KeyEvent.VK_ADD){
			if (r.getWidth()<=Const.eps) return ;
			setView(r.getX()+r.getWidth()*0.25,r.getY(),0.5*r.getWidth(),r.getHeight());
		}
		else if (ev.getKeyCode()==KeyEvent.VK_PAGE_DOWN){
			if (r.getWidth()<=Const.eps) return ;
			setView(r.getX()-r.getWidth()/2,r.getY(),r.getWidth(),r.getHeight());
		}
		else if (ev.getKeyCode()==KeyEvent.VK_PAGE_UP){
			if (r.getWidth()<=Const.eps) return ;
			setView(r.getX()+r.getWidth()/2,r.getY(),r.getWidth(),r.getHeight());
		}
		else if (ev.getKeyCode()==KeyEvent.VK_DOWN){
			if (r.getHeight()<=Const.eps) return ;
			setView(r.getX(),r.getY()-r.getHeight()/20,r.getWidth(),r.getHeight());
		}
		else if (ev.getKeyCode()==KeyEvent.VK_UP){
			if (r.getHeight()<=Const.eps) return ;
			setView(r.getX(),r.getY()+r.getHeight()/20,r.getWidth(),r.getHeight());
		}
		else if (ev.getKeyCode()==KeyEvent.VK_LEFT){
			List<Point2D> pnts=getPoints(selChn);
			if (pnts.size()<=0) return ;
			int i=(int)((caret.getX()-view.getX())/view.getWidth()*pnts.size())+1;
			//log.debug("caret.X="+caret.getX()+", view.X="+view.getX()+", sz="+points.size()+", i="+i);
			if (i<0) i=0; else if (i>=pnts.size()) i=pnts.size()-1;
			while (i<pnts.size()-1 && pnts.get(i).getX()<caret.getX()) ++i;
			while (i>0 && pnts.get(i).getX()>=caret.getX()) --i;
			setCaret(pnts.get(i));
		}
		else if (ev.getKeyCode()==KeyEvent.VK_RIGHT){
			List<Point2D> pnts=getPoints(selChn);
			if (pnts.size()<=0) return ;
			int i=(int)((caret.getX()-view.getX())/view.getWidth()*pnts.size())-1;
			//log.debug("caret.X="+caret.getX()+", view.X="+view.getX()+", sz="+points.size()+", i="+i);
			if (i<0) i=0; else if (i>=pnts.size()) i=pnts.size()-1;
			while (i>0 && pnts.get(i).getX()>caret.getX()) --i;
			while (i<pnts.size()-1 && pnts.get(i).getX()<=caret.getX()) ++i;
			if (i<pnts.size()) setCaret(pnts.get(i));
		}
		else if (ev.getKeyCode()==KeyEvent.VK_ENTER){
			propsDialog();
		}
		else {
			JMenuItem mi=getMenuItem(popup, ev);
			if (mi!=null){
				ActionEvent a=new ActionEvent(mi,ev.getID(),mi.getActionCommand(),ev.getModifiers());
				ActionListener l[]=mi.getActionListeners();
				for (int i=0; i<l.length; ++i) l[i].actionPerformed(a);
			}
			else log.debug("KeyCode=0x%X(%d), modif=0x%X(%d)",ev.getKeyCode(),ev.getKeyCode(),
					ev.getModifiers(),ev.getModifiers());
		}
	}
	//TODO move to common
	protected JMenuItem getMenuItem(JPopupMenu m,KeyEvent ev){
		Component[] cmps=m.getComponents();
		int mod=ev.getModifiers();
		if (mod==KeyEvent.ALT_MASK) mod|=KeyEvent.ALT_DOWN_MASK;
		for (int i=0; i<cmps.length; ++i){
			if (!(cmps[i] instanceof JMenuItem)) continue;
			JMenuItem mi=(JMenuItem)cmps[i];
			KeyStroke ks=mi.getAccelerator();
			if (ks==null) continue;
			if (ks.getKeyCode()==ev.getKeyCode() && ks.getModifiers()==mod)
				return mi;
		}
		return null;
	}

	protected void setCaret(MouseEvent ev) {
		Rectangle r=new Rectangle(plotArea);
		r.grow(5,2);
		if (!r.contains(ev.getPoint())||view.getWidth()<Const.eps)
			return ;
		Point2D.Double pe=new Point2D.Double(ev.getX(),ev.getY());
		scr2view(pe);
		if (plotMode!=PLOT_X) {
			setCaret(pe.x,pe.y);
			return ;
		}
		pe.y=Double.NaN;
		List<Point2D> pnts=getPoints(selChn);
		double xdelta=chns.get(selChn).param.connectDist;
		int n=pnts.size();
		if (n>0) {
			Point2D p1,p=pnts.get(0);
			if (pe.x<=p.getX()) {
				if (xdelta>0 && Math.abs(pe.x-p.getX())>2*xdelta);
				else {pe.x=p.getX();pe.y=p.getY();}
			}
			else if ((p=pnts.get(n-1)).getX()<=pe.x) {
				if (xdelta>0 && Math.abs(pe.x-p.getX())>2*xdelta);
				else {pe.x=p.getX();pe.y=p.getY();}
			}
			else {
				int i=(int)((pe.x-view.getX())/view.getWidth()*n+0.5)+1;
				//log.debug("caret.X="+caret.getX()+", view.X="+view.getX()+", sz="+points.size()+", i="+i);
				if (i<=0) pe.setLocation(pnts.get(0));
				else if (i>=n) pe.setLocation(pnts.get(n-1));
				else{
					while (i>0 && pnts.get(i).getX()>pe.x) --i;
					while (i<n-1 && pnts.get(i).getX()<pe.x) ++i;
					p=p1=pnts.get(i);
					if (i>0) p=pnts.get(i-1);
					if (Math.abs(p.getX()-p1.getX())<3*xdelta){
						pe.y=p1.getY()+(pe.x-p1.getX())*(p.getY()-p1.getY())/(p.getX()-p1.getX());
					}
					else {
						double d=Math.abs(p.getX()-pe.x);
						if (d>Math.abs(p1.getX()-pe.x)){d=Math.abs(p1.getX()-pe.x);p=p1;}
						if (d<xdelta){pe.x=p.getX();pe.y=p.getY();}
						//else log.debug("xdelta=%g abs=%g",xdelta,d);
					}
				}
			}
		}
		setCaret(pe.x,pe.y);
	}
	final public void propsDialog(){
		PropertiesPanel p=createPropertiesPanel();
		p.setProperties(this);
		if (getName()==null) UiUtils.showDialog(this,p,"Plot Properties");
		else UiUtils.showDialog(this,p,getName()+" Plot Properties");
		if (p.getResult()==1) {
			clearSelection();
			p.getProperties(this);
		}
	}
	protected PropertiesPanel createPropertiesPanel(){
		return new PropertiesPanel(xUnit,yUnit);
	}
	public void setUnit(String x,String y){
		xUnit=x; yUnit=y;
		recalcPlotArea=true;
	}
	public void setXDelta(double x){
		ChannelParams par=chns.get(selChn).param;
		par.connectDist=x;
	}
	public void setRange(double x,double y) { view.width=x;view.height=y;recalcPlotArea=true;}
	public Rectangle2D getView(){return view;}
	public Point2D getCaret() {return caret;}
	final public void setView(double x,double y,double w,double h) {
		if (Double.isNaN(x)||Double.isNaN(w)||Double.isNaN(y)||Double.isNaN(h)){
			log.warn("setView with NaN arg, ignored");
			return ;
		}
		if (Double.isNaN(view.x)) view.x=0;
		if (Double.isNaN(view.width)) view.width=0;
		if (Double.isNaN(view.y)) view.y=0;
		if (Double.isNaN(view.height)) view.height=0;
		int opt=NOCHANGE;
		if (Math.abs(x-view.getX())>Const.eps) opt|=CHANGED_X;
		if (Math.abs(y-view.getY())>Const.eps) opt|=CHANGED_Y;
		if (Math.abs(w-view.getWidth())>Const.eps) opt|=CHANGED_W;
		if (Math.abs(h-view.getHeight())>Const.eps) opt|=CHANGED_H;
		if (opt==NOCHANGE)return ;
		Rectangle2D.Double ov=new Rectangle2D.Double(view.x,view.y,view.width,view.height);
		view.setRect(x, y, w, h);
		//if ((opt&(CHANGED_Y|CHANGED_H))!=0) automodeY=NOAUTO;
		if (autoBounds.x==AUTOBOUNDS_MOVE){
			for (int i=0; i<chns.size(); ++i){
				List<Point2D> points=getPoints(i);
				while (points.size()>0){
					Point2D p=points.get(0);
					if (p.getX()>view.getMinX()) break;
					points.remove(0);
				}
				while (points.size()>0){
					Point2D p=points.get(points.size()-1);
					if (p.getX()<view.getMaxX()) break;
					points.remove(points.size()-1);
				}
			}
			if (getPoints(selChn).size()>0) {
				List<Point2D> points=getPoints(selChn);
				setCaret(points.get(points.size()-1));
			}
			else setCaret(x+w,caret.getY());
		}
		recalcPlotArea=true;
		if (listener!=null) listener.viewChanged(this,ov,opt);
		ov=null;
		repaint(500);
	}
	public void autoFitY2() {
		double av=0,mn=0,mx=0;
		int cnt=0;
		for (int chn=0; chn<chns.size(); ++chn){
			List<Point2D> pnts=getPoints(chn);
			for (int i=0; i<pnts.size(); ++i){
				double yy=pnts.get(i).getY();
				if (Double.isNaN(yy)) continue;
				if (cnt==0){av=0; mn=mx=yy;}
				av+=yy; ++cnt;
				if (mn>yy) mn=yy; else if (mx<yy) mx=yy;
			}
		}
		if (cnt==0) return ;
		av/=cnt;
		double dy=Math.min(Math.abs(mn-av),Math.abs(mx-av));
		dy=Math.log1p(dy+Math.abs(av)/50);
		if (dy<0.2) dy=0.2;
		view.height=0; view.y=av;
		view.add(view.x,mx+dy);
		view.add(view.x,mn-dy);
		recalcPlotArea=true;
		if (listener!=null) listener.caretChanged(this);
		repaint(500);
	}
	private void autoFitY2(double y) {
		if (Double.isNaN(y)) return ;
		double dy,av;
		av=y;
		dy=Math.log1p(Math.abs(av)/10);
		if (dy<0.1) dy=0.1;
		view.add(view.x,av+dy);
		view.add(view.x,av-dy);
		recalcPlotArea=true;
		repaint(500);
	}
	//FIXME avoid using pnts argument
	private boolean addView(List<Point2D> pnts,double x,double y){
		if (Double.isNaN(x)) {log.error("x=NaN");return false;}
		if (Double.isNaN(caret.getX())) setCaret(x,y);
		if (view.width<Const.eps) {view.width=0.1; view.x=x;}
		if (view.height<Const.eps && !Double.isNaN(y)) {view.height=0.1; view.y=y;}
		if (!Double.isNaN(y)){
			if (autoBounds.y==AUTOBOUNDS_SCALE){
				if (autoBounds.x==AUTOBOUNDS_MOVE) autoFitY2(y);
				else{
					double dy=Math.log1p(Math.abs(y/10));
					view.add(view.x,y-dy);
					view.add(view.x,y+dy);
				}
			}
		}
		if (!Double.isNaN(x)){
			if (autoBounds.x==AUTOBOUNDS_SCALE) {
				double dx=Math.log1p(Math.abs(x/10));
				view.add(x-dx,view.y);
				view.add(x+dx,view.y);
			}
			else if (autoBounds.x==AUTOBOUNDS_MOVE) {
				double xdelta=chns.get(selChn).param.connectDist;
				if (x<view.getMinX()) view.x=x;
				else if (view.getMaxX()<x) {
					view.x=x-view.width*0.999;
				}
				while (pnts.size()>2) {
					Point2D p=pnts.get(1);
					if (p.getX()>view.getMinX()) break;
					pnts.remove(0);
				}
				int n=pnts.size();
				if (n==0 || Math.abs(caret.getX()-x)<xdelta || x>caret.getX())
					setCaret(x, y);
			}
		}
		if (pnts.size()>0) {
			if (caret.getX()<view.x){
				if (autoBounds.x==AUTOBOUNDS_MOVE) setCaret(pnts.get(pnts.size()-1));
				else setCaret(pnts.get(0));
			}
			else if (caret.getX()>view.getMaxX()) setCaret(pnts.get(pnts.size()-1));
		}
		return true;
	}
	private void calcPlotArea(){
		if (!recalcPlotArea) return ;
		if (getGraphics()==null) return ;
		Insets ins=getInsets();
		recalcPlotArea=false;
		plotArea.width=getWidth();plotArea.height=getHeight();
		plotArea.width-=ins.left+ins.right;
		plotArea.height-=ins.top+ins.bottom;
		FontMetrics mtr=getFontMetrics(gridfont);
		int s=mtr.stringWidth("+0.00");{
		double grid=gridY(view.getHeight()/getHeight()*40);
		String l=formatY(view.getCenterY(),grid);
		int w=mtr.stringWidth(l);
		if (s<w) s=w;
		}
		mtr=getFontMetrics(unitfont);
		if (yUnit!=null) s=Math.max(mtr.stringWidth(yUnit),s);
		plotArea.width-=s+SP;
		plotArea.height-=mtr.getHeight();
		plotArea.x=getWidth()-plotArea.width-ins.right;
		plotArea.y=ins.top;
	}
	//TODO optimalization: put GeneralPath into PlotChannel
	private void plotChannel(Graphics2D g2,PlotChannel chn,Rectangle2D.Float pm,Point2D pe){
		List<Point2D> pnts=chn.pnts;
		if (pnts.size()==0) return ;
		ChannelParams par=chn.param;
		g2.setColor(par.color);
		Point2D p1=null;

		if (par.markerType==0) pm.width=pm.height=2f;

		GeneralPath path=new GeneralPath(Path2D.WIND_NON_ZERO,pnts.size());
		for (int i=0; i<pnts.size(); ++i) {
			Point2D p=pnts.get(i);
			if (Double.isNaN(p.getX()) || Double.isNaN(p.getY())){p1=null; continue;}
			pe.setLocation(p);
			view2scr(pe);
			pm.x=(float)pe.getX()-pm.width/2+0.1f;
			pm.y=(float)pe.getY()-pm.height/2+0.1f;
			g2.fill(pm);//draw marker
			if (p1==null) path.moveTo(pe.getX(),pe.getY());
			else if (par.connectDist<0)
				path.lineTo(pe.getX(),pe.getY());
			else if (par.connectDist>0 && Math.abs(p.getX()-p1.getX())<3*par.connectDist)
				path.lineTo(pe.getX(),pe.getY());
			else path.moveTo(pe.getX(),pe.getY());
			p1=p;
		}
		g2.setColor(new Color(par.color.getRGB()&0x7fffffff,true));
		g2.setStroke(par.line);
		g2.draw(path);
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		calcPlotArea();

		FontMetrics mtr;
		Graphics2D g2=(Graphics2D)g;
		//long t0=System.currentTimeMillis();
		int xunitW=0,yunitH=0;
		mtr=g2.getFontMetrics(unitfont);
		if (xUnit!=null && !xUnit.isEmpty()) xunitW=mtr.stringWidth(xUnit);
		if (yUnit!=null && !yUnit.isEmpty()) yunitH=mtr.getHeight();

		Shape origclip=g2.getClip();
		g2.clipRect(plotArea.x,plotArea.y,plotArea.width,plotArea.height);
		g2.setColor(plotbg);
		g2.fillRect(plotArea.x,plotArea.y,plotArea.width,plotArea.height);
		//marker size
		Rectangle2D.Float pm=new Rectangle2D.Float(0,0,3,3);
		if (plotMode==PLOT_XY){
			int s=0;
			for (int chn=0; chn<chns.size(); chn++){
				List<Point2D> pnts=chns.get(chn).pnts;
				for (int i=0; i<pnts.size(); ++i){
					Point2D p=pnts.get(i);
					if (view.contains(p)) ++s;
				}
			}
			//log.debug("points=%d, view.size=%d",s,plotArea.width*plotArea.height);
			pm.width=(float)(plotArea.width*plotArea.height)/s;
			pm.width/=5f;
			if (pm.width<2f) pm.width=2f;
			pm.height=pm.width;
		}

		Stroke origstroke=g2.getStroke();
		Point2D pe=new Point2D.Double();
		if (view.getHeight()>0) {
		//hi and low limits
		g2.setStroke(limitLine);
		if (lLimits!=null && lLimits.length > 0) {
			//Rectangle2D rect=new Rectangle2D.Double();
			Line2D line=new Line2D.Double();
			int l=Math.min(lLimits.length, LIMITCOLORS.length);
			for (int fi=0; fi < l; ++fi) {
				float f=lLimits[fi];
				if (f < view.getMinY()) continue;
				g2.setColor(LIMITCOLORS[fi]);
				pe.setLocation(0,f);
				view2scr(pe);
				//rect.setFrame(plotArea.x, pe.getY(), plotArea.width, plotArea.getMaxY()-pe.getY());
				//g2.fill(rect);
				line.setLine(plotArea.x, pe.getY(), plotArea.getMaxX(), pe.getY());
				g2.draw(line);
			}
		}
		if (uLimits!=null && uLimits.length > 0) {
			//Rectangle2D rect=new Rectangle2D.Double();
			Line2D line=new Line2D.Double();
			int l=Math.min(uLimits.length, LIMITCOLORS.length);
			for (int fi=0; fi < l; ++fi) {
				float f=uLimits[l-fi-1];
				if (f > view.getMaxY()) continue;
				g2.setColor(LIMITCOLORS[l-fi-1]);
				pe.setLocation(0,f);
				view2scr(pe);
				//rect.setFrame(plotArea.x, plotArea.y, plotArea.width, pe.getY());
				//g2.fill(rect);
				line.setLine(plotArea.x, pe.getY(), plotArea.getMaxX(), pe.getY());
				g2.draw(line);
			}
		}
		}
		for (int chn=0; chn<chns.size(); chn++)
			plotChannel(g2, chns.get(chn), pm, pe);
		g2.setClip(origclip);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setStroke(line1dash);
		g2.setFont(gridfont); mtr=g2.getFontMetrics(gridfont);
		//horizontal grids
		if (view.getWidth()>Const.eps) {
			double grid=gridX(view.getWidth()/plotArea.width*70);
			int lp=plotArea.x-SP/2;
			for (double v=PlotXY.round(view.getX(),grid); v<view.getMaxX(); v+=grid){
				if (v<view.getX()) continue;
				pe.setLocation(v,0);
				view2scr(pe);
				int p=(int)(pe.getX()+0.5);
				g2.setColor(Color.GRAY);
				g2.drawLine(p,plotArea.y+plotArea.height-1,p,plotArea.y);
				g2.setColor(Color.BLACK);
				String l=formatX(v,grid);
				int w=mtr.stringWidth(l);
				p=p-w/2; //p = start of string
				if (p>lp && p+w+SP/2+xunitW<plotArea.x+plotArea.width){
					g2.drawString(l,p,plotArea.y+plotArea.height+mtr.getAscent());
					lp=p+w+SP/2;
				}
			}
		}
		//vertical grids
		if (view.getHeight()>Const.eps) {
			double grid=gridY(view.getHeight()/plotArea.height*40);
			for (double v=PlotXY.round(view.getY(),grid); v<view.getMaxY(); v+=grid){
				if (v<view.getY()) continue;
				pe.setLocation(0,v);
				view2scr(pe);
				int p=(int)(pe.getY()+0.5);
				g2.setColor(Color.GRAY);
				g2.drawLine(plotArea.x,p,plotArea.x+plotArea.width-1,p);
				g2.setColor(Color.BLACK);
				String l=formatY(v,grid);
				if (p>plotArea.y+yunitH && p<plotArea.y+plotArea.height+SP/2)
					g2.drawString(l,plotArea.x-mtr.stringWidth(l)-SP/2,p+mtr.getHeight()/2-2);
			}
		}
		if (caretType!=0){
			g2.setColor(Color.RED);
			pe.setLocation(caret);
			view2scr(pe);
			if ((caretType&CARET_X)!=0 && view.contains(caret.getX(),view.getCenterY())){
				int p=(int)(pe.getX()+0.499);
				g2.drawLine(p,plotArea.y+plotArea.height-1,p,plotArea.y);
			}
			if ((caretType&CARET_Y)!=0 && view.contains(view.getCenterX(),caret.getY())){
				int p=(int)(pe.getY()+0.499);
				g2.drawLine(plotArea.x,p,plotArea.x+plotArea.width-1,p);
			}
		}
		g2.setStroke(origstroke);

		g2.setColor(Color.BLACK);
		g2.setFont(unitfont);mtr=g2.getFontMetrics(unitfont);
		if (xUnit!=null){
			g2.drawString(xUnit,plotArea.x+plotArea.width-1-mtr.stringWidth(xUnit),
					plotArea.y+plotArea.height-1+mtr.getAscent());
		}
		if (yUnit!=null){
			g2.drawString(yUnit,plotArea.x-mtr.stringWidth(yUnit)-SP/2,
					plotArea.y+mtr.getAscent()-SP/2);
		}
		if (selMode==SELECTION_XY) ;
		else if (selMode==SELECTION_X){
			selFrame.y=view.y;
			selFrame.height=view.height;
		}
		else if (selMode==SELECTION_Y){
			selFrame.x=view.x;
			selFrame.width=view.width;
		}
		if (selMode!=0 && (selFrame.getWidth()>0 && selFrame.getHeight()>0)){
			g2.setColor(SELCOLOR);
			Rectangle2D r=new Rectangle2D.Double();
			pe.setLocation(selFrame.x,selFrame.y);
			view2scr(pe);
			r.setFrame(pe.getX(),pe.getY(),0,0);
			pe.setLocation(selFrame.getMaxX(),selFrame.getMaxY());
			view2scr(pe);
			r.add(pe);
			g2.fill(r);
			g2.setColor(SELCOLOR.darker()); g2.draw(r);
		}
		//log.debug("paint time=%d",System.currentTimeMillis()-t0);
		if (listener!=null) listener.paintDone(this);
	}
	public double gridX(double v) { return PlotXY.grid(v); }
	public double gridY(double v) { return PlotXY.grid(v); }
	public String formatX(double v,double grid) { return PlotXY.format(v,grid); }
	public String formatY(double v,double grid) { return PlotXY.format(v,grid); }
	public double parseX(String txt) throws ParseException {return Double.parseDouble(txt);}
	public double parseY(String txt) throws ParseException {return Double.parseDouble(txt);}

	public final static double grid(double x){
		double c=0.75;
		double[] g={1.0,2.0,5.0,10.0};

		x=Math.abs(x);
		//double m = 1.0; while (x>m) m*=10; while (x<m) m/=10;
		double m = Math.pow(10,Math.floor(Math.log(x)/Math.log(10)));
		x/=m;
		for (int i=0; i<g.length-1; ++i)
			if (x<g[i]*(1-c)+g[i+1]*c) return g[i]*m;
		return g[g.length-1]*m;
	}
	public final static int prec(double x){
		String s=PlotXY.format(x);
		int i=s.indexOf('.');
		if (i<0){ //int
			i=1;
			while (i<s.length() && s.charAt(s.length()-i)=='0') ++i;
			return 1-i;
		}
		return s.length()-i-1;
	}
	public final static double round(double x,double m){
		return Math.floor(x/m+0.5)*m;
	}
	public final static String format(double v) {
		if (Math.abs(v)<Const.eps) v=0;
		return fmtMAX.format(v);
	}
	public final static String format(double v,int prec) {
		double p=Math.pow(10,prec);
		v=Math.rint(v*p)/p;
		if (Math.abs(v)<Const.eps) v=0;
		if (prec<=-3) return fmtINT3.format(v/1000);
		//if (prec<=-2) return fmtINT2.format(v/1000);
		if (prec<=0) return fmtINT.format(v);
		if (prec==1) return fmtF1.format(v);
		if (prec==2) return fmtF2.format(v);
		if (prec==3) return fmtF3.format(v);
		return fmtMAX.format(v);
	}
	public final static String format(double v,double grid) {
		int prec=PlotXY.prec(grid);
		double p=Math.pow(10,prec);
		v=Math.rint(v*p)/p;
		if (Math.abs(v)<Const.eps) v=0;
		if (prec<=-3) return fmtINT3.format(v/1000);
		//if (prec<=-2) return fmtINT2.format(v/1000);
		if (prec<=0) return fmtINT.format(v);
		if (prec==1) return fmtF1.format(v);
		if (prec==2) return fmtF2.format(v);
		if (prec==3) return fmtF3.format(v);
		log.debug("prec=%d for grid=%f",prec,grid);
		return new DecimalFormat("0."+StrUtil.repeat("0",prec),decSymbols).format(v);
	}

	public void setPlotListener(PlotListener l) {this.listener=l;}

	public void dispose() {
		listener=null;
		for (int chn=0; chn<chns.size(); chn++){
			List<Point2D> pnts=getPoints(chn);
			pnts.clear();
		}
		chns.clear();
		xUnit=null;yUnit=null;
		plotArea=null;
		view=null; caret=null;
	}
	static public interface PlotListener{
		public void caretChanged(PlotXY plot);
		public void viewChanged(PlotXY plot,Rectangle2D oldview,int opt);
		public void selChanged(PlotXY plot);
		public void paintDone(PlotXY plot);
	}

	static class PropertiesPanel extends DialogPanel {
		protected JTextField xmin,xmax;
		protected JTextField ymin,ymax;
		protected PropertiesPanel(){
			super(new GridBagLayout());
		}
		public PropertiesPanel(String xunit,String yunit) {
			super(new GridBagLayout());

			TitledBorder title;
			JPanel p;
			GridBagConstraints constr = new GridBagConstraints();
			constr.fill = GridBagConstraints.BOTH;
			//constr.anchor = GridBagConstraints.LINE_START;
			constr.weightx=constr.weighty=0;
			constr.insets=new Insets(5,5,0,0);//top,left,bot,right
			constr.anchor=GridBagConstraints.CENTER;

			if (xunit!=null){
				title = BorderFactory.createTitledBorder(
						BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
						xunit.length()>0?"X-Range ["+xunit+"]":"X-Range");
				constr.gridwidth = 1;
				p=new JPanel(new GridBagLayout());
				p.setBorder(title);
				buildXPropsUI(p,constr);
				constr.gridwidth = 1;
				add(p,constr);
				constr.gridwidth = GridBagConstraints.REMAINDER;
				add(new JLabel(),constr);
			}

			if (yunit!=null) {
				title = BorderFactory.createTitledBorder(
						BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
						yunit.length()>0?"Y-Range ["+yunit+"]":"Y-Range");
				constr.gridwidth = 1;
				p=new JPanel(new GridBagLayout());
				p.setBorder(title);
				p.add(new JLabel("min:"),constr);
				p.add(ymin=new JTextField(5),constr);
				p.add(new JLabel("  max:"),constr);
				p.add(ymax=new JTextField(5),constr);
				add(p,constr);
				constr.gridwidth = GridBagConstraints.REMAINDER;
				add(new JLabel(),constr);
			}
			addButtons();
		}

		protected void buildXPropsUI(JPanel p,GridBagConstraints constr){
			p.add(new JLabel("min:"),constr);
			p.add(xmin=new JTextField(5),constr);
			p.add(new JLabel("  max:"),constr);
			p.add(xmax=new JTextField(5),constr);
		}

		public void setProperties(PlotXY obj){
			Rectangle2D r=obj.getView();
			if (xmin!=null && xmax!=null) {
				double grid=obj.gridX(r.getWidth());
				xmin.setText(obj.formatX(r.getMinX(),grid));
				xmax.setText(obj.formatX(r.getMaxX(),grid));
			}
			if (ymin!=null && ymax!=null) {
				double grid=obj.gridY(r.getHeight()/50);
				ymin.setText(obj.formatY(r.getMinY(),grid));
				ymax.setText(obj.formatY(r.getMaxY(),grid));
			}
			result=0;
		}
		public void getProperties(PlotXY obj) {
			Rectangle2D.Double r=new Rectangle2D.Double();
			if (xmin!=null && xmax!=null &&
					!xmin.getText().isEmpty() && !xmax.getText().isEmpty()){
				try{
					r.x=obj.parseX(xmin.getText());
					double x2=obj.parseX(xmax.getText());
					if (x2>r.x) r.width=x2-r.x;
				}catch (Exception e) {
					log.error(e);
				}
			}
			else{
				r.x=obj.getView().getMinX();
				r.width=obj.getView().getMaxX()-r.x;
			}
			if (ymin!=null && ymax!=null &&
					!ymin.getText().isEmpty() && !ymax.getText().isEmpty()){
				try{
					r.y=obj.parseY(ymin.getText());
					r.height=obj.parseY(ymax.getText())-r.y;
				}catch (Exception e) {
					log.error(e);
				}
			}
			else{
				r.y=obj.getView().getMinY();
				r.height=obj.getView().getMaxY()-r.y;
			}
			obj.setView(r.x, r.y, r.width, r.height);
		}
	}
	private static class PlotChannel{
		public PlotChannel(int n) {
			param.color=chnColorSimple[n%chnColorSimple.length];
			//if (plotMode==PLOT_XY) g2.setColor(chnColor[chn%chnColor.length]);
			//else g2.setColor(chnColor4[chn%chnColor4.length]);
		}
		final List<Point2D> pnts=new ArrayList<Point2D>();
		final ChannelParams param=new ChannelParams();
		public void clear() { pnts.clear();}
	}
	public static class ChannelParams{
		//int plotType; //make it global or per channel?
		public Color color;
		public Stroke line=line1;
		public int markerType=0; //none,square,circle,triangle
		public double connectDist=-1;
	}
}
