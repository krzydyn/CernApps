/*
 *  Copyright (c) 2016 Krzysztof Dynowski All Rights Reserved
 *
 *  Contact: krzydyn@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package sys;

import java.io.OutputStream;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class Sound {
	final static Logger log = Logger.getLogger();
	//public static final int SAMPLE_RATE = 16 * 1024; // ~16KHz
	public static final int SAMPLE_RATE = 44100;
	public static final int LINE_BUFFER = 2200;

	final static SourceDataLine srcline = createSource();
	final static TargetDataLine tgtline = createTarget();

	private static SourceDataLine createSource() {
		SourceDataLine l=null;
		try {
			l=AudioSystem.getSourceDataLine(new AudioFormat(SAMPLE_RATE, 8, 1, true, true));
		} catch (LineUnavailableException e) {}
		return l;
	}
	private static TargetDataLine createTarget() {
		TargetDataLine l=null;
		try {
			l=AudioSystem.getTargetDataLine(new AudioFormat(SAMPLE_RATE, 8, 1, true, true));
		} catch (LineUnavailableException e) {}
		return l;
	}

	//private static final byte[] lineBuffer=new byte[LINE_BUFER];

	/*
	 * A4 = 440 Hz
	 */
	public enum Note {
		/*  0.00*/ VOID,
		/* 16.35*/ C0, C0h, D0, D0h, E0, F0, F0h, G0, G0h, A0, A0h, B0,
		/* 32.70*/ C1, C1h, D1, D1h, E1, F1, F1h, G1, G1h, A1, A1h, B1,
		/* 65.40*/ C2, C2h, D2, D2h, E2, F2, F2h, G2, G2h, A2, A2h, B2,
		/*130.80*/ C3, C3h, D3, D3h, E3, F3, F3h, G3, G3h, A3, A3h, B3,
		/*261.60*/ C4, C4h, D4, D4h, E4, F4, F4h, G4, G4h, A4, A4h, B4,
		/*523.30*/ C5,
		XX(900)
		;

		private final byte[] waveform;
		private final double fr;
		public double freq() {return fr;}

		static private double idx2freq(int i) {
			if (i > 0) {
				double exp = (i - 1) / 12.0;
				return 16.3516 * Math.pow(2.0, exp);
			}
			return 0d;
		}


		private Note() {
			fr = idx2freq(this.ordinal());
			waveform = createWaveform(fr);
			//double f = idx2freq(this.ordinal());
			//Log.debug("Note %s is %.3f Hz  period= %.3f ms samples=%d",this.name(),f,1000/f,waveform.length);
		}
		private Note(double fr) {
			this.fr=fr;
			waveform = createWaveform(fr);
		}
	}

	static private byte[] createWaveform(double f) {
		byte[] waveform;
		if (f<=0) {
			waveform = new byte[1];
		}
		else {
			double periodtime = 1.0 / f;//one full period
			double soundtime = Math.floor(0.5/periodtime)*periodtime;
			waveform = new byte[(int)(soundtime * SAMPLE_RATE)];
			for (int i = 0; i < waveform.length; i++) {
				double angle = 2.0 * Math.PI * i / (periodtime*SAMPLE_RATE);
				waveform[i] = (byte)(Math.sin(angle) * 127f/2f);
			}
		}
		return waveform;
	}

	static private void play(SourceDataLine line, byte[] waveform, long ms) {
		long length = (SAMPLE_RATE*ms+500) / 1000;
		long offs = 0;
		line.start();
		while (offs < length) {
			int l = waveform.length;
			if (offs+l > length) l=(int)(length-offs);
			if (line.write(waveform, 0, l) != l) {
				log.error("not all samples written");
			}
			offs+=l;
		}
		line.drain();
	}

	static public void play(Note n, long ms) throws Exception {
		long length = (SAMPLE_RATE*ms+500) / 1000;
		log.debug("samples to play %d, wave.len=%d, freq:%.1fHz", length, n.waveform.length, n.freq());

		if (!srcline.isOpen())
			srcline.open(srcline.getFormat(), LINE_BUFFER);
		play(srcline, n.waveform, ms);
		//line.close();
	}

	static public void play(double freq, long ms) throws Exception {
		if (!srcline.isOpen())
			srcline.open(srcline.getFormat(), LINE_BUFFER * 5);
		play(srcline, createWaveform(freq), ms);
		//line.close();
	}

	static public void dong() {
		try {
			play(Note.C5, 50);
		} catch (Exception e) {log.error(e);}
	}

	static public Clip play(URL url) throws Exception {
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
		Clip clip = AudioSystem.getClip();
		clip.open(audioInputStream);
		clip.start();
		return clip;
	}

	static private Clip playing=null;
	static public void play(String url) {
		try{
			if (playing!=null) {
				playing.drain();
				playing.stop();
				playing=null;
			}
			URL u = Resource.getResourceURL(url);
			playing = play(u);
		}catch(Exception e){log.error(e);}
	}

	static public void capture(OutputStream os, int secs) throws Exception {
		tgtline.open(tgtline.getFormat(), LINE_BUFFER * 5);
		byte[] data = new byte[tgtline.getBufferSize() / 5];
		tgtline.start();
		while (secs > 0) {
			int r =  tgtline.read(data, 0, data.length);
			os.write(data, 0, r);
			secs-=1;
		}
	}
}
