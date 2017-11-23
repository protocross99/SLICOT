/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.util.*;
import com.siwind.bupt.impl.FlowRecord;


public class PendingList {

	int RECORD_LIMIT = 10;	
	public ArrayList<FlowRecord> pendingList; 

	public PendingList() {
		this.pendingList = new ArrayList<FlowRecord>();
	};

	public void addRecord(byte[] srcMac, byte[] dstMac,byte[] srcPort, byte[] dstPort, String status,int tableId, String flowId){
		FlowRecord record = new FlowRecord( srcMac,dstMac, srcPort, dstPort, status, tableId, flowId);
		pendingList.add(record);
	};

	public void removeRecord(byte[] srcMac, byte[] dstMac)
	{
		Iterator<FlowRecord> recordIt = pendingList.iterator();
        	while (recordIt.hasNext()) {
            	if (Arrays.equals(recordIt.next().getSrcMac(),srcMac)) {
                recordIt.remove();
                // If you know it's unique, you could `break;` here
            }
        }
	};

	public void updateRecordStatus(byte[] srcMac,byte[] dstMac,String status)
	{
   	    for (FlowRecord record : pendingList) {
            if (Arrays.equals(record.getSrcMac(),srcMac) && Arrays.equals(record.getDstMac(),dstMac)) {
           	      record.setStatus(status);
       	          // If you know it's unique, you could `break;` here
   	          }
         }
	};
}
