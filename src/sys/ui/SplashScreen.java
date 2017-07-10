package sys.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.BevelBorder;

@SuppressWarnings("serial")
public class SplashScreen extends JFrame {
	private JLabel txt=null;
	public SplashScreen(Image img){
		this("Starting ...",img,null);
	}
	public SplashScreen(Image img,String info){
		this("Starting ...",img,info);
	}
	public SplashScreen(String title,Image img){
		this(title,img,null);
	}
	public SplashScreen(String title,Image img,String info) {
	    super(title);
	    getRootPane().setWindowDecorationStyle(JRootPane.NONE);
	    setUndecorated(true);
		
	    //setTitle(title);
	    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    setAlwaysOnTop(true);
	    JPanel p=(JPanel)getContentPane();
	    p.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.GRAY,Color.GRAY));
	    p.add(new JLabel(new ImageIcon(img)),BorderLayout.CENTER);
	    if (info!=null) setText(info);
	    else pack();
	    setLocationRelativeTo(null);
	    setVisible(true);
	}
	public void setText(String s){
		if (txt==null){
			JPanel p=(JPanel)getContentPane();
			txt=new JLabel(s);
		    txt.setBorder(BorderFactory.createEmptyBorder(1,5,1,5));
		    p.add(txt,BorderLayout.SOUTH);
		    pack();
		}
		else txt.setText(s);
	}
}
