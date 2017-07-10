package sys.ui;
/*
Property String	Object Type
CheckBox.background	Color
CheckBox.border	Border
CheckBox.darkShadow	Color
CheckBox.disabledText	Color
CheckBox.focus	Color
CheckBox.focusInputMap	Object[ ]
CheckBox.font	Font
CheckBox.foreground	Color
CheckBox.gradient	List
CheckBox.highlight	Color
CheckBox.icon	Icon
CheckBox.interiorBackground	Color
CheckBox.light	Color
CheckBox.margin	Insets
CheckBox.rollover	Boolean
Checkbox.select	Color
CheckBox.shadow	Color
CheckBox.textIconGap	Integer
CheckBox.textShiftOffset	Integer
CheckBoxUI	String
 */

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;

/**
 * customized CheckBox (check sign area)
 * @author KySoft, Krzysztof Dynowski
 *
 */
public class CheckBoxIcon implements Icon {
	private static BasicStroke lineType=new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
	public int getIconWidth() { return w; }
	public int getIconHeight() { return h; }

	final private int w,h;
	public CheckBoxIcon(int w, int h) { this.w=w; this.h=h; }
	public CheckBoxIcon() {this(10,10);}
	public void paintIcon(Component c, Graphics g, int x, int y) {
		ButtonModel m=null;
		if (c instanceof AbstractButton){
			m = ((AbstractButton)c).getModel();
		}
		Graphics2D g2=(Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		int w=getIconWidth();
		int h=getIconHeight();
		g2.translate(x,y);
		g2.setClip(0,0,w,h);
		if (c.isOpaque()){
			g2.setColor(c.getBackground());
			g2.fillRect(0,0,w,h);
		}
		if (m!=null && m.isSelected()) {
			g2.setColor(c.getForeground());
			g2.setStroke(lineType);
			g2.drawLine(0,0,w-1,h-1);
			g2.drawLine(0,h-1,w-1,0);
		}
		g2.setClip(null);
		g2.translate(-x,-y);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_DEFAULT);
	}
}
