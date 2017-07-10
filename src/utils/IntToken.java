package utils;

import java.util.Random;

public class IntToken {
	private final static Random rnd=new Random();
	public static int generate(int digs){
		int tok=rnd.nextInt(9)+1;
		for (int i=1; i < digs; ++i) {
			tok*=10; tok+=rnd.nextInt(10);
		}
		return tok;
	}
}
