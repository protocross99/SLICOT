/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.util.*;

public class FlowRecord {

	private byte[] srcMac;
	private byte[]	dstMac;
	private byte[] srcPort;
	private byte[]	dstPort;
	private String status;
	private int tableId;
	private String flowId;

	public FlowRecord(byte[] srcMac,byte[] dstMac, byte[] srcPort, byte[] dstPort, String status, int tableId, String flowId){
		this.srcMac = srcMac;
		this.dstMac = dstMac;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.status = status;
		this.tableId = tableId;
		this.flowId = flowId;
	};

	public byte[] getSrcMac(){
		return this.srcMac;
	};

	public byte[] getDstMac(){
		return this.dstMac;
	};
	
	public void setStatus(String status){
		this.status = status;
 	};

	public int getTableId(){
		return this.tableId;
	};

	public String getFlowId(){
		return this.flowId;
	};
}	
