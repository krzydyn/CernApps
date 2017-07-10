package sys.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import sys.Logger;

@SuppressWarnings("serial")
public class DialogPanel extends JPanel implements ActionListener {
	static protected Logger log=Logger.getLogger();
	static final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true);
	protected JButton defaultButton,okButton;
	protected int result=0;

	public DialogPanel() {super(new BorderLayout());}
	public DialogPanel(LayoutManager lm) { super(lm); }

	public void setDefaultButton(JButton b) {
		defaultButton=b;
		JRootPane p=getRootPane();
		if (p!=null) p.setDefaultButton(defaultButton);
	}
	public void addButtons(){addButtons(null);}
	public void addButtons(JButton other){
		JPanel p=new JPanel();
		JButton b=other;
		if (b!=null) p.add(b);
		p.add(b=new JButton(UIManager.getString("OptionPane.okButtonText")));
		okButton=b;
		b.setActionCommand("ok");
		b.addActionListener(this);
		setDefaultButton(b);
		p.add(b=new JButton(UIManager.getString("OptionPane.cancelButtonText")));
		b.setActionCommand("can");
		b.addActionListener(this);
		if (getLayout() instanceof GridBagLayout){
			GridBagConstraints constr = new GridBagConstraints();
			constr.fill = GridBagConstraints.BOTH;
			constr.gridwidth = GridBagConstraints.REMAINDER;
			add(p,constr);
		}
		else add(p,BorderLayout.SOUTH);
	}
	@Override
	public void addNotify(){
		super.addNotify();
		JRootPane p=getRootPane();
		if (defaultButton!=null) p.setDefaultButton(defaultButton);
		//assign esc key to "can" command
		String key="ESC";
		p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke,key);
		p.getActionMap().put(key,new CommandAction("can",this));
		final Container c=getTopLevelAncestor();
		MouseAdapter ma=new MouseAdapter(){
			private Point start=null;
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton()!=MouseEvent.BUTTON1) return ;
				Point p=e.getPoint();
				SwingUtilities.convertPointToScreen(p,c);
				start=p;
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton()!=MouseEvent.BUTTON1) return ;
				start=null;
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				if (e.getModifiersEx()!=MouseEvent.BUTTON1_DOWN_MASK) return ;
				Point p=e.getPoint();
				if (start==null) return ;
				SwingUtilities.convertPointToScreen(p,c);
				if (p.equals(start)) return;
				c.invalidate();
				int dx=p.x-start.x,dy=p.y-start.y;
				p=c.getLocation();
				start.x+=dx; start.y+=dy;
				p.x+=dx;p.y+=dy;
				c.setLocation(p);
				c.validate();
			}
		};
		c.addMouseMotionListener(ma);
		c.addMouseListener(ma);
	}
	public final int getResult(){return result;}

	@Override
	public void actionPerformed(ActionEvent ev) {
		final String cmd=ev.getActionCommand();
		if ("ok".equals(cmd)) result=1;
		else if ("can".equals(cmd)) result=0;
		else {log.warn("unhandled cmd '%s'",cmd);return ;}
		getTopLevelAncestor().setVisible(false);
		//this causes to destroy this panel (on Linux).
		//if (top instanceof Window) ((Window)top).dispose();
		//else if (top instanceof Dialog) ((Dialog)top).dispose();
		//this also not prevent
		//this.getParent().remove(this);
		//if (top instanceof Window) ((Window)top).dispose();
		//else if (top instanceof Dialog) ((Dialog)top).dispose();
	}
}
