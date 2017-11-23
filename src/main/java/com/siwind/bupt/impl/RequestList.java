/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.siwind.bupt.impl;

import java.util.*;

public class RequestList {

	ArrayList<SourceMac> numOfRequestList;

	public RequestList(){
		this.numOfRequestList = new ArrayList<SourceMac>();
	};

	public boolean isExisted(byte[] sourceMac){
		boolean result;
		result = false;
		for (SourceMac record : numOfRequestList){
			if (Arrays.equals(record.getSrcMac(),sourceMac)) {
				result =  true;
				break;
			}
		}
		return result;
	}

	public int getNumOfReq(byte[] sourceMac){
		int result = 0;
	for (SourceMac record : numOfRequestList){
			if (Arrays.equals(record.getSrcMac(),sourceMac)) {
				result = record.getNumberOfReq();
				break;
			}
		}
		return result;
	}

	public void addRequest(byte[] sourceMac){
		SourceMac record = new SourceMac(sourceMac);
		numOfRequestList.add(record);
	}

	public void updateRequestList(byte[] sourceMac){
		for (SourceMac record : numOfRequestList){
			if (Arrays.equals(record.getSrcMac(),sourceMac)) {
				record.incrNumOfReq();
				break;
			}
		}
	}

	public void removeRequest(byte[] sourceMac) {
		for (SourceMac record : numOfRequestList) {
			if (Arrays.equals(record.getSrcMac(),sourceMac)) {
				numOfRequestList.remove(record);
				break;
			}
		}
	}

	private static class SourceMac {

         private byte[] srcMac;
         private int numOfReq;
			
		public SourceMac(byte[] srcMac){
			this.srcMac = srcMac;
			this.numOfReq = 1;
		};

		public byte[] getSrcMac(){
			return this.srcMac;
		}

		public int getNumberOfReq(){
			return this.numOfReq;
		}

		public void incrNumOfReq(){
			this.numOfReq += 1;
		}
	};
}
