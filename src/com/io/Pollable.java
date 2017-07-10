package com.io;

import java.io.IOException;

public interface Pollable {
	public int poll(long tm)throws IOException;
}
