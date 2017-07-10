package com.link;

import sys.Logger;

public interface DataLink {
	static final Logger log = Logger.getLogger();

	public int open();
	public int close();
	public int send(StringBuilder b);
	public int recv(StringBuilder b);
}
