package plot;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;
import javax.swing.UIManager;

import common.Logger;
import common.algebra.Const;

@SuppressWarnings("serial")
public class PlotBar extends JPanel {
	static final Logger log=Logger.getLogger();
	static final int SP=5;
	static final Dimension prefsz=new Dimension(50,115);
	static final Font unitfont=UIManager.getFont("Panel.font").deriveFont(Font.BOLD,12f);
	static final Font gridfont=UIManager.getFont("Panel.font").deriveFont(Font.PLAIN,10f);
	static final Stroke dashLine=new BasicStroke(1,0,0,3,new float[]{3f,3f},0);

	protected String yUnit="";
	private int automodeY=PlotXY.AUTOBOUNDS_SCALE;
	private final Rectangle plotArea=new Rectangle();
	private boolean recalcPlotArea;
	private double yValue;
	protected Rectangle2D.Double view=new Rectangle2D.Double();

	public PlotBar(){
		setEnabled(true);
		setFocusable(true);
		recalcPlotArea=true;

		addComponentListener(new ComponentListener(){
			public void componentHidden(ComponentEvent e) {log.debug("PlotXY hidden");}
			public void componentMoved(ComponentEvent e) { }
			public void componentResized(ComponentEvent e){PlotBar.this.componentResized();}
			public void componentShown(ComponentEvent e) {}
		});
		setMinimumSize(prefsz);
		setPreferredSize(prefsz);
	}
	protected void componentResized() {
		recalcPlotArea=true;
		repaint();
	}
	public Rectangle2D getView(){return view;}
	public void setUnit(String x,String y){
		yUnit=y;
		recalcPlotArea=true;
		repaint();
	}
	synchronized public void addView(double x,double y){
		if (view.width<Const.eps) view.x=x;
		if (view.height<Const.eps) view.y=y;
		if (automodeY==PlotXY.AUTOBOUNDS_SCALE){
			view.add(view.x,y+(y+1)*0.2);
			view.add(view.x,y-(y+1)*0.2);
			/*
			if (view.height/2<Math.abs(y-view.y)){
				view.height=0;view.y=y;
				view.add(view.x,y+(y+1)*0.2);
				view.add(view.x,y-(y+1)*0.2);
			}*/
		}
		else view.add(x,y);
		view.x+=view.width; view.width=0;
	}
	synchronized public void addPoint(double x,double y){
		addView(x,y);
		yValue=y;
		repaint();
	}

	private void calcPlotArea(){
		if (!recalcPlotArea) return ;
		if (getGraphics()==null) return ;
		FontMetrics mtr=getFontMetrics(gridfont);
		recalcPlotArea=false;
		int r,lw=mtr.stringWidth("100.0");
		if (yUnit!=null){
			r=mtr.stringWidth(yUnit);
			if (lw<r) lw=r;
		}
		double grid=gridY(view.getHeight()/getHeight()*40);
		r=mtr.stringWidth(formatY(yValue,grid));
		if (lw<r) lw=r;
		Insets ins=getInsets();
		plotArea.width=getWidth()-SP;plotArea.height=getHeight()-mtr.getHeight();
		plotArea.width-=ins.left+ins.right+SP+lw;
		plotArea.height-=ins.top+ins.bottom;
		plotArea.x=ins.right+SP+lw;
		plotArea.y=ins.top+mtr.getHeight()/2;
	}
	public void paintComponent(Graphics g){
		//FontMetrics mtr;
		Graphics2D g2=(Graphics2D)g;
		super.paintComponent(g);
		calcPlotArea();

		FontMetrics mtr=getFontMetrics(gridfont);
		
		int yunitH=0;
		if (yUnit!=null && !yUnit.isEmpty()) yunitH=mtr.getHeight();

		g2.setColor(Color.GRAY);
		g2.fillRect(plotArea.x,plotArea.y,plotArea.width,plotArea.height);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setFont(gridfont);
		g2.setColor(Color.BLACK);
		/*for (int i=0; i<=100; i+=50){
			String l=Integer.toString(i);
			//int w=mtr.stringWidth(l);
			int p=plotArea.y+plotArea.height-i*plotArea.height/100;
			g2.drawString(l,plotArea.x+plotArea.width+SP/2,p+mtr.getHeight()/2-2);
		}*/
		if (view.getHeight()>Const.eps) {
			double fy=-(plotArea.height-1)/view.getHeight();
			g2.setColor(Color.GREEN);
			int p=plotArea.height+(int)((yValue-view.getY())*fy);
			g2.fillRect(plotArea.x,plotArea.y+p,plotArea.width,plotArea.height-p);
			g2.setColor(Color.BLACK);
			double grid=gridY(view.getHeight()/getHeight()*40);
			for (double v=PlotXY.round(view.getY(),grid); v<view.getMaxY(); v+=grid){
				if (v<view.getY()) continue;
				p=plotArea.y+plotArea.height+(int)((v-view.getY())*fy);
				String l=formatY(v,grid);
				if (p>plotArea.y+yunitH && p<plotArea.y+plotArea.height+SP/2){
					g2.drawString(l,plotArea.x-mtr.stringWidth(l)-SP/2-2,p+mtr.getHeight()/2-2);
					g2.drawLine(plotArea.x-SP/2, p, plotArea.x, p);
				}
			}
		}
		g2.setFont(unitfont);mtr=g2.getFontMetrics(unitfont);
		if (yUnit!=null){
			g2.drawString(yUnit,plotArea.x-mtr.stringWidth(yUnit)-SP/2,
					plotArea.y+mtr.getAscent()-SP/2);			
		}
	}
	public double gridY(double v) { return PlotXY.grid(v); }
	public String formatY(double v,double grid) { return PlotXY.format(v,grid); }
}
