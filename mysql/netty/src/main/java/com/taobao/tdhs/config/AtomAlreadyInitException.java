package com.taobao.tdhs.config;

public class AtomAlreadyInitException extends Exception {

	private static final long serialVersionUID = -3907211238952987907L;

	public AtomAlreadyInitException() {
		super();
	}

	public AtomAlreadyInitException(String msg) {
		super(msg);
	}

	public AtomAlreadyInitException(Throwable cause) {
		super(cause);
	}

	public AtomAlreadyInitException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
