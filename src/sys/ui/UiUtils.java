package sys.ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import sys.Logger;
import sys.SysUtil;

public class UiUtils {
	public final static Logger log = Logger.getLogger();

	public final static void macify(String appname) {
		if (!SysUtil.isMac()) return ;
		System.setProperty("apple.awt.graphics.EnableQ2DX", "true");//???
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", appname);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
	}

	public static void setFullScreenEnabled(Window window,boolean enabled) {
		if (!SysUtil.isMac()) return ;
        String className = "com.apple.eawt.FullScreenUtilities";
        String methodName = "setWindowCanFullScreen";
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, new Class<?>[] {
                    Window.class, boolean.class });
            method.invoke(null, window, enabled);
        } catch (Throwable t) {}
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void quitAction(final Window w,final WindowListener l) {
		if (!SysUtil.isMac()) return ;

		try {
			//close all windows before quit
			Class<?> app = Class.forName("com.apple.eawt.Application");
			Method getApp = app.getMethod("getApplication");
			Object inst = getApp.invoke(app);
			Class<Enum> strategy = (Class<Enum>)Class.forName("com.apple.eawt.QuitStrategy");
			Enum closeAllWindows = Enum.valueOf(strategy, "CLOSE_ALL_WINDOWS");
			Method method = app.getMethod("setQuitStrategy", strategy);
			method.invoke(inst, closeAllWindows);
		} catch (Throwable e) {
		}
	}

	public final static void setModified(JFrame f, boolean m) {
		f.getRootPane(  ).putClientProperty("windowModified", m);
	}
	public final static void setIcon(JFrame f,ImageIcon icon){
		if (icon==null) return ;
		if (!SysUtil.isMac()) f.setIconImage(icon.getImage());
		else {
			Class<?> cl;
			try {
				cl = Class.forName("com.apple.eawt.Application");
				Method m = cl.getMethod("getApplication");
				Object app=m.invoke(null);
				m=cl.getMethod("setDockIconImage", Image.class);
				m.invoke(app, icon.getImage());
			} catch (Exception e) {}
		}
	}
	public final static void makeSimpleButton(JButton b) {
		b.setOpaque(true);
		b.setBorder(BorderFactory.createRaisedBevelBorder());
	}
	public final static void makeRawButton(JButton b) {
		b.setBorderPainted(false);
		b.setFocusPainted(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		b.setContentAreaFilled(false);
		b.setBorder(null);
		//b.setBorder(BorderFactory.createRaisedBevelBorder());
		b.setOpaque(true);
	}


	public final static void showScreens(){
		GraphicsDevice[] gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		for (int i=0; i<gs.length; i++) {
			DisplayMode dm = gs[i].getDisplayMode();
			int screenWidth = dm.getWidth();
			int screenHeight = dm.getHeight();
			log.debug("screen[%d]: %d x %d",i,screenWidth,screenHeight);
			for (GraphicsConfiguration gc1 : gs[i].getConfigurations()) {
				/*if (SysUtil.isMac()) {
					sun.java2d.opengl.CGLGraphicsConfig gc = (sun.java2d.opengl.CGLGraphicsConfig)gc1;
					log.debug("gc: %s",gc);
					log.debug("gc.maxtexture: %d x %d",gc.getMaxTextureWidth(),gc.getMaxTextureHeight());
				}
				else*/
					log.debug("gc: %s",gc1);
			}
		}
	}

	static public void messageBox(JPanel p,String title,Object msg,int type){
		if (title==null){
			if (type==JOptionPane.ERROR_MESSAGE) title="Error";
			else if (type==JOptionPane.QUESTION_MESSAGE) title="Question";
			else if (type==JOptionPane.WARNING_MESSAGE) title="Warning";
			else title="Information";
		}
		if (msg==null) msg="Internal error: NULL object";
		if (!(msg instanceof JComponent)) {
			JTextArea a=new JTextArea(msg.toString());
			a.setEditable(false);
			a.setOpaque(false);
			msg=a;
		}
		JOptionPane.showMessageDialog(p!=null?p.getTopLevelAncestor():null,msg,title,type);
	}
	static public void messageBox(JPanel p,String title,Object msg,int type,Icon icon){
		if (title==null) {
			if (type==JOptionPane.ERROR_MESSAGE) title="Error";
			else if (type==JOptionPane.QUESTION_MESSAGE) title="Question";
			else if (type==JOptionPane.WARNING_MESSAGE) title="Warning";
			else title="Information";
		}
		JOptionPane.showMessageDialog(p!=null?p.getTopLevelAncestor():null,msg,title,type,icon);
	}
	static public void showDialog(JComponent rel,JPanel panel,String title){
		showDialog(rel,panel,title,true);
	}
	static public void removeButtons(Component c){
		if (c instanceof AbstractButton || c instanceof Button){
			Container p=c.getParent();
			//log.debug("remove button %s from %s",c.getAccessibleContext().getAccessibleName(),p.getClass().getName());
			p.remove(c);
		}
		else if (c instanceof Container){
			//log.debug("processing %d components of %s",((Container) c).getComponentCount(),c.getClass().getName());
			Component[] comps = ((Container)c).getComponents();
			for(int i = 0; i<comps.length; ++i)
				removeButtons(comps[i]);
		}
		else {
			//log.debug("untouched component %s",c.getClass().getName());
		}
	}

	//TODO use JOptionPane.YES_NO_OPTION and so on
	static public JDialog showDialog(JComponent rel,JComponent panel,String title,boolean modal){
		Window w=(Window)(rel!=null?rel.getTopLevelAncestor():null);
		JDialog dialog=new JDialog(w,title,modal?ModalityType.APPLICATION_MODAL:ModalityType.MODELESS);
		removeButtons(dialog);
		//if (SysUtil.isMac()) dialog.setUndecorated(true);

		//dialog.setResizable(false);
		panel.setOpaque(true);
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setMinimumSize(dialog.getSize());
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(rel);
		if (modal) {
			dialog.setVisible(true);
			dialog=null;
		}
		else if (rel!=null) dialog.setVisible(true);
		return dialog;
	}
	static public JFrame showWindow(JComponent rel,JComponent panel,String title){
		JFrame frame=new JFrame(title);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setMinimumSize(frame.getSize());
		frame.setLocationRelativeTo(rel);
		return frame;
	}
	static public void showHelp(JPanel parent,String title,String url){
		try{ showHelp(parent,title,new URL(url)); }catch (Exception e) {}
	}
	static private JFrame helpFrame;
	static private JEditorPane helpPane;
	static public void showHelp(final JPanel parent,final String title,URL url){
		showHelp(parent, title, url, new Dimension(500,400));
	}
	static public void showHelp(final JPanel parent,final String title,URL url,Dimension sz){
		if (url==null){
			messageBox(parent,null,
				"This help not available yet",JOptionPane.WARNING_MESSAGE);
			return ;
		}
		if (helpPane!=null){
			helpPane.fireHyperlinkUpdate(new HyperlinkEvent(helpFrame,HyperlinkEvent.EventType.ACTIVATED,url));
			helpFrame.setVisible(true);
			helpFrame.toFront();
			return;
		}
		//log.debug("help URL = "+url);
		//JEditorPane supports HTML, but not CSS !
		helpPane=new JEditorPane();
		helpFrame=new JFrame();
		if (title!=null) helpFrame.setTitle(title);
		if (parent!=null){
			Image i=((Frame)parent.getTopLevelAncestor()).getIconImage();
			if (i!=null) helpFrame.setIconImage(i);
		}
		helpPane.setEditable(false);
		helpPane.setBackground(Color.LIGHT_GRAY);
		helpPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent ev){
				if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
					URL url=ev.getURL();
					log.info("HELP: "+url.getPath());
					try{
						String s=url.getPath();
						int r,i=0,j=s.length();
						if ((r=s.indexOf("//",i))>=0)i=r+2;
						if ((r=s.indexOf("!",i))>=0)i=r+1;
						if ((r=s.indexOf("res/",i))>=0)i=r+4;
						if ((r=s.indexOf("help/",i))>=0)i=r+5;
						if ((r=s.lastIndexOf('.'))>=0 && r>i) j=r;
						helpFrame.setTitle(title+" "+s.substring(i,j));
						helpPane.setPage(url);
					}catch (Exception e) {
						log.error(e);
					}
				}
				else if (ev.getEventType() == HyperlinkEvent.EventType.ENTERED) ;
				else if (ev.getEventType() == HyperlinkEvent.EventType.EXITED) ;
				else log.warn("event not supproted eventtype=%s",ev.getEventType().toString());
			}
		});
		helpPane.fireHyperlinkUpdate(new HyperlinkEvent(helpFrame,HyperlinkEvent.EventType.ACTIVATED,url));
		helpPane.setPreferredSize(sz);
		helpFrame.getContentPane().add(new JScrollPane(helpPane));
		helpFrame.pack();
		helpFrame.setLocationRelativeTo(parent);
		helpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		helpFrame.setVisible(true);
	}

	@SuppressWarnings("serial")
	static public void assignKeyStroke(JPanel p,KeyStroke k,final JButton b){
        AbstractAction act=new AbstractAction(){
			@Override
			public void actionPerformed(ActionEvent ev){ b.doClick(); }
        };
		InputMap map=p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		map.put(k,b.getActionCommand());
		p.getActionMap().put(b.getActionCommand(),act);
	}
	@SuppressWarnings("serial")
	static public void assignKeyStroke(JComponent p,KeyStroke k,final ActionListener l,final String cmd){
        AbstractAction act=new AbstractAction(){
			@Override
			public void actionPerformed(ActionEvent ev){ l.actionPerformed(new ActionEvent(ev.getSource(),ev.getID(),cmd)); }
        };
		InputMap map=p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		map.put(k,cmd);
		p.getActionMap().put(cmd,act);
	}

	static public boolean showInBrowser(String url){
    	String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();
        try{
	        if (os.indexOf( "win" ) >= 0) {
	        	// this doesn't support showing urls in the form of "page.html#nameLink"
	        	//rt.exec("start "+url);
	            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
	        } else if (os.indexOf( "mac" ) >= 0) {
	            rt.exec( "open " + url);
	        } else {
	        	// Do a best guess on unix until we get a platform independent way
	        	// Build a list of browsers to try, in this order.
	        	String[] browsers = {"chrome", "epiphany", "firefox", "mozilla", "konqueror",
	        			"netscape","opera","links","lynx"};

	        	// Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
	        	StringBuilder cmd = new StringBuilder();
	        	for (int i=0; i<browsers.length; i++)
	        		cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + url + "\" ");

	        	rt.exec(new String[] { "sh", "-c", cmd.toString() });
	        }
        }catch (IOException e){
        	return false;
        }
        return true;
    }

	public static JComponent findComponent(JComponent c, Class<? extends JComponent> cl){
		for (int i=0; i<c.getComponentCount(); ++i) {
			Component c1=c.getComponent(i);
			if (cl.isInstance(c1)) return (JComponent)c1;
			if (c1 instanceof JComponent){
				c1=findComponent((JComponent)c1, cl);
				if (c1!=null) return (JComponent)c1;
			}
		}
		return null;
	}

	static public void setGroupEnabled(JComponent c,boolean ena){
		//Component[] comps=c.getComponents();
		//for (int i=0; i<comps.length; ++i) comps[i].setEnabled(ena);
		for (int i=0; i<c.getComponentCount(); ++i) {
			c.getComponent(i).setEnabled(ena);
		}
		c.setEnabled(ena);
	}
	static public void updateComponentText(JComponent c,ResourceBundle text)
	{
		if (c==null) return ;
		String s=c.getName();
		if (s!=null)
		{
			String t;
			try{t=text.getString(s);}catch (Exception e) {t=null;}
			if (t!=null)
			{
				try{
					Method m=c.getClass().getMethod("setText", String.class);
					m.invoke(c,t);
				}catch (Exception e) {}
			}
			try{t=text.getString(s+"tip");}catch (Exception e) {t=null;}
			if (t!=null) c.setToolTipText(t);
		}
		if (c instanceof JMenu)
		{
			JMenu m=(JMenu)c;
			for (int i=0; i<m.getItemCount(); ++i)
				updateComponentText(m.getItem(i),text);
		}
		else if (c instanceof JMenuItem) ;
		else {
			//log.debug("comp %s %d",c.getClass().getCanonicalName(),c.getComponentCount());
			for (int i=0; i<c.getComponentCount(); ++i)
			{
				Component jc=c.getComponent(i);
				if (jc instanceof JComponent)
					updateComponentText((JComponent)jc,text);
			}
		}
	}

	public static void setupUIManagerFonts() {
		//use with care (slows down)
		//System.setProperty("awt.useSystemAAFontSettings","on");
		//System.setProperty("swing.aatext", "true");

		if (File.separatorChar=='\\'){
			JFrame.setDefaultLookAndFeelDecorated(true);
			JDialog.setDefaultLookAndFeelDecorated(true);
		}

		Font f=UIManager.getFont("Button.font");
		Font bfont,font;
		if (f.isBold()){
			bfont=f;
			font=bfont.deriveFont(Font.PLAIN);
		}
		else {
			font=f;
			bfont=font.deriveFont(Font.BOLD);
		}

		//log.debug("%s",UIManager.get("Button.font").toString());
		UIManager.put("Button.font", bfont);
		UIManager.put("ToggleButton.font", bfont);
		UIManager.put("RadioButton.font", bfont);
		UIManager.put("CheckBox.font", bfont);
		UIManager.put("ColorChooser.font", bfont);
		UIManager.put("ComboBox.font", bfont);
		UIManager.put("Label.font", bfont);
		UIManager.put("List.font", font);
		UIManager.put("MenuBar.font", bfont);
		UIManager.put("MenuItem.font", bfont);
		UIManager.put("RadioButtonMenuItem.font", bfont);
		UIManager.put("CheckBoxMenuItem.font", bfont);
		UIManager.put("Menu.font", bfont);
		UIManager.put("PopupMenu.font", bfont);
		UIManager.put("OptionPane.font", bfont);
		UIManager.put("Panel.font", font);
		UIManager.put("ProgressBar.font", bfont);
		UIManager.put("ScrollPane.font", bfont);
		UIManager.put("Viewport.font", font);
		UIManager.put("TabbedPane.font", bfont);
		UIManager.put("Table.font", font);
		UIManager.put("TableHeader.font", bfont);
		UIManager.put("TextField.font", font);
		UIManager.put("PasswordField.font", font);
		UIManager.put("TextArea.font", font);
		UIManager.put("TextPane.font", font);
		UIManager.put("EditorPane.font", font);
		UIManager.put("TitledBorder.font", bfont);
		UIManager.put("ToolBar.font", bfont);
		UIManager.put("ToolTip.font", font);
		UIManager.put("Tree.font", font);

		//Color c=UIManager.getColor("JLabel.background");
		//UIManager.put("Button.margin", new Insets(1,1,1,1));

		/*Color c=Color.GREEN;
		UIManager.put("MenuBar.background", c);
        UIManager.put("Menu.background", c);
        UIManager.put("MenuItem.background", c.brighter().brighter());*/
        //log.debug("mb: %s, m: %s",UIManager.get("MenuBarUI"),UIManager.get("MenuUI"));
	}

}
