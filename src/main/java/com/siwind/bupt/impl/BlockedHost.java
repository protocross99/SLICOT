/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.util.*;

public class BlockedHost {
	private byte[] srcMac;
	private long startTime;

	public BlockedHost(byte[] srcMac, long startTime) {
		this.srcMac = srcMac;
		this.startTime = startTime;
	}
	
	public byte[] getSrcMac(){
		return this.srcMac;
	}
	
	public long getTime() {
		return this.startTime;
	}
}
