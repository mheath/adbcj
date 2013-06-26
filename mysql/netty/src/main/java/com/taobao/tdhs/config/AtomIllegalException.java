package com.taobao.tdhs.config;

/**
 * @author qihao
 *
 */
public class AtomIllegalException extends Exception {

	private static final long serialVersionUID = -5341803227125385166L;

	public AtomIllegalException() {
		super();
	}

	public AtomIllegalException(String msg) {
		super(msg);
	}

	public AtomIllegalException(Throwable cause) {
		super(cause);
	}

	public AtomIllegalException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
