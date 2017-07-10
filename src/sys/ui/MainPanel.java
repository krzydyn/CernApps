package sys.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import sys.Logger;
import sys.Version;

@SuppressWarnings("serial")
public abstract class MainPanel extends JPanel  implements ActionListener {
	protected static final Logger log=Logger.getLogger();
	public MainPanel() {
		this(new BorderLayout());
	}
	public MainPanel(LayoutManager l) {
		super(l);
		setOpaque(true);
		addComponentListener(new ComponentListener(){
			@Override
			public void componentHidden(ComponentEvent e) {}
			@Override
			public void componentMoved(ComponentEvent e) { }
			@Override
			public void componentResized(ComponentEvent e){resized();}
			@Override
			public void componentShown(ComponentEvent e) {}
		});
	}
	public JMenuBar buildMenuBar() {return null;}
	protected void exiting(){}
	protected void resized() {}
	protected boolean hasFullScreen() {return false;}
	public JScrollPane createScroll(JComponent c){
		return createScroll(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}
	public JScrollPane createScroll(JComponent c,int vpolicy,int hpolicy){
		JScrollPane scroll=new JScrollPane(c,vpolicy,hpolicy);
		scroll.setWheelScrollingEnabled(true);
		return scroll;
	}
	public static void invokeInEDT(Runnable runnable) {
		if (EventQueue.isDispatchThread()) runnable.run();
		else EventQueue.invokeLater(runnable);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd=e.getActionCommand();
		if ("quit".equals(cmd)) {
			//exiting(); //TODO check if windowClosing is sent
			JFrame f=(JFrame)getTopLevelAncestor();
			f.setVisible(false);
			System.exit(0);
		}
	}

	/**
	 * EDT multithread safe
	 * @param p
	 * @param s
	 */
	public static void appendText(final JComponent p, final String s){
		invokeInEDT(new Runnable() {
			@Override
			public void run() {
				if (p instanceof JTextArea) {
					JTextArea tx=(JTextArea)p;
					tx.append(s);
					tx.setCaretPosition(tx.getDocument().getLength());
				}
				else if (p instanceof JTextPane) {
					JTextPane tx=(JTextPane)p;
					StyledDocument doc=tx.getStyledDocument();
					try {
						doc.insertString(doc.getLength(),s,null);
					} catch (BadLocationException e) {}
					//tx.scrollRectToVisible(new Rectangle(0,tx.getHeight()-1,1,1));
					tx.setCaretPosition(doc.getLength());
				}
			}
		});
	}

	private static MainPanel _startGUI(String appname,Class<? extends MainPanel> pclass) {
		UiUtils.showScreens();

		try {
			final MainPanel panel=pclass.newInstance();
			final JFrame f=new JFrame(appname+" "+Version.getInstance());
			if (panel.hasFullScreen())
				UiUtils.setFullScreenEnabled(f, true);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setJMenuBar(panel.buildMenuBar());
			panel.setOpaque(true);
			panel.setDoubleBuffered(false);
			f.setContentPane(panel);
			final WindowListener l;
			f.addWindowListener(l=new WindowListener() {
				@Override
				public void windowOpened(WindowEvent e) {
					log.debug("window opened");
				}
				@Override
				public void windowIconified(WindowEvent e) {
					log.debug("window iconified");
				}
				@Override
				public void windowDeiconified(WindowEvent e) {
					log.debug("window deicon");
				}
				@Override
				public void windowDeactivated(WindowEvent e) {
					log.debug("window deactivated"); //when another app selected
				}
				@Override
				public void windowClosing(WindowEvent e) {
					log.debug("window closing");
					panel.exiting();
					f.setVisible(false);
				}
				@Override
				public void windowClosed(WindowEvent e) {
					log.debug("window closed");
				}
				@Override
				public void windowActivated(WindowEvent e) {
					log.debug("window activated"); // when bring to front
					//f.repaint(500);
					//panel.invalidate();
					//f.validate();//call doLayout
					f.doLayout();
				}
			});
			UiUtils.quitAction(f, l);
			f.pack();
			Dimension d=f.getSize();
			Insets ins=f.getInsets();
			Dimension e=new Dimension(ins.left+ins.right,ins.top+ins.bottom);
			if (d.width<300) d.width=500;
			else d.width += e.width;
			if (d.height<200) d.height=400;
			else d.height += e.height;
			f.setPreferredSize(d);
			f.setSize(d);
			f.setVisible(true);
			return panel;
		}catch (Throwable e) {
			log.error(e);
			return null;
		}
	}

	static private class RunGui implements Runnable {
		private final String appname;
		private final Class<? extends MainPanel> pclass;
		public MainPanel panel;
		public RunGui(final String appname,final Class<? extends MainPanel> pclass) {
			this.appname=appname;
			this.pclass=pclass;
		}
		@Override
		public void run() {panel=_startGUI(appname,pclass);}
	}

	public static MainPanel startGUI(final String appname,final Class<? extends MainPanel> pclass) {
		Version.createInstance(pclass);
		try {
			RunGui r=new RunGui(appname,pclass);
			EventQueue.invokeAndWait(r);
			return r.panel;
		} catch (Exception e) {
			log.error(e);
			return null;
		}
	}
}
