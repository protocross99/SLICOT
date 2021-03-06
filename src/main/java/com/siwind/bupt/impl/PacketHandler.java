/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siwind.bupt.impl.FlowManager;
import com.siwind.bupt.impl.RequestList;
import com.siwind.bupt.impl.PendingList;
import com.siwind.bupt.impl.FlowRecord;
import com.siwind.bupt.impl.BlockedHost;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.commons.codec.binary.Base64;
public class PacketHandler implements PacketProcessingListener {

    /**
     * size of MAC address in octets (6*8 = 48 bits)
     */
    private static final int MAC_ADDRESS_SIZE = 6;

    /**
     * start position of destination MAC address in array
     */
    private static final int DST_MAC_START_POSITION = 0;

    /**
     * end position of destination MAC address in array
     */
    private static final int DST_MAC_END_POSITION = 6;

    /**
     * start position of source MAC address in array
     */
    private static final int SRC_MAC_START_POSITION = 6;

    /**
     * end position of source MAC address in array
     */
    private static final int SRC_MAC_END_POSITION = 12;


    /**
     * start position of ethernet type in array
     */
    private static final int ETHER_TYPE_START_POSITION = 12;

    /**
     * end position of ethernet type in array
     */
    private static final int ETHER_TYPE_END_POSITION = 14;
	private static final int SRC_PORT_START_POSITION = 34;
    private static final int SRC_PORT_END_POSITION = 36;	
	private static final int DST_PORT_START_POSITION = 36;
    private static final int DST_PORT_END_POSITION = 38;

	 /**
     * start position of control flag in array
     */
    private static final int CTRL_FLAG_START_POSITION = 47;

    /**
     * end position of control flag in array
     */
    private static final int CTRL_FLAG_END_POSITION = 48;

	String SYN_PACKET = "02";
	String SYN_ACK_PACKET = "12";
	String ACK_PACKET = "10";
	String RST_PACKET = "14";
	int REQ_LIMIT = 10;
	private static final byte[] ETH_TYPE_IPV4 = new byte[] { 0x08, 0x00 };
	PendingList mainPendingList;
	PendingList subPendingList;
	RequestList supportRequestList;
	ArrayList<BlockedHost> blockList;
	String flowName;
	int tableId = 0;
	int flowNum = 0;
	String flowId ;

	private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);

    public PacketHandler() {
        LOG.info("[Siwind] PacketHandler Initiated. ");
		this.mainPendingList = new PendingList();
		this.subPendingList = new PendingList();
		this.supportRequestList = new RequestList();
		this.blockList = new ArrayList<BlockedHost>();
    };

    @Override
    public void onPacketReceived(PacketReceived notification) {
        // TODO Auto-generated method stub
		ArrayList<String> nodeList = getListNode();
        // read src MAC and dst MAC
        byte[] dstMacRaw = extractDstMac(notification.getPayload());
        byte[] srcMacRaw = extractSrcMac(notification.getPayload());
		byte[] dstPortRaw = extractDstMac(notification.getPayload());
        byte[] srcPortRaw = extractSrcMac(notification.getPayload());
        byte[] ethType   = extractEtherType(notification.getPayload());
		byte[] pktType   = extractPacketType(notification.getPayload());

        String dstMac = byteToHexStr(dstMacRaw, ":");
        String srcMac = byteToHexStr(srcMacRaw, ":");
		String dstPort = byteToHexStr(dstPortRaw, ":");
        String srcPort = byteToHexStr(srcPortRaw, ":");
        String ethStr = byteToHexStr(ethType, "");
		String pktStr = byteToHexStr(pktType, "");

// 		System.out.println("from: " + srcMac + " to: " + dstMac + " Packet Type: " + pktStr);
        LOG.info("[Siwind] Received packet from MAC {} to MAC {}, EtherType=0x{} ", srcMac, dstMac, ethStr);
		if (Arrays.equals(ETH_TYPE_IPV4, ethType)) {
			if (pktStr.equals(SYN_PACKET)) {
				double eslapsed = 0;
				boolean isBlocked = false;
				for (BlockedHost host : blockList) {
					if (Arrays.equals(host.getSrcMac(),srcMacRaw)) {
						eslapsed = (double)(System.nanoTime() - host.getTime())/1.0E09;
						isBlocked = true;
						break;
					}
				}
				if (isBlocked) {
					if ( eslapsed > 600 ) {
						tableId = 0;
						for (String nodeId : nodeList) {
							flowName = "Block-SLICOTS" + flowNum;
							flowId = "SLICOT" + flowNum;
							FlowManager.blockHost(nodeId,flowName,srcMac,tableId,flowId);
							flowNum += 1;
						}
						System.out.println("Block host from history: " + srcMac);
					}
				}	
				//System.out.println("Received SYN");
				else if (supportRequestList.isExisted(srcMacRaw)) {
					//search in pending list
					int numOfReq = supportRequestList.getNumOfReq(srcMacRaw);
					//if found get number of current request
					if (numOfReq > REQ_LIMIT) {
						//add rule to block host
						for (FlowRecord record : mainPendingList.pendingList) {
							if (Arrays.equals(srcMacRaw,record.getSrcMac()) && Arrays.equals(dstMacRaw,record.getDstMac())) {
								int tableId = record.getTableId();
								String flowId = record.getFlowId();
								for (String nodeId : nodeList) {
									FlowManager.deleteFlow(nodeId,tableId,flowId);
								}
							}
						}
						for (FlowRecord record : subPendingList.pendingList) {
							if (Arrays.equals(srcMacRaw,record.getSrcMac()) && Arrays.equals(dstMacRaw,record.getDstMac())) {
								int tableId = record.getTableId();
								String flowId = record.getFlowId();
								for (String nodeId : nodeList) {
									FlowManager.deleteFlow(nodeId,tableId,flowId);
								}
							}
						}
						mainPendingList.removeRecord(srcMacRaw,dstMacRaw);
						subPendingList.removeRecord(srcMacRaw,dstMacRaw);
						tableId = 0;
						long start = System.nanoTime();
						BlockedHost blockedHost = new BlockedHost(srcMacRaw,start);
						blockList.add(blockedHost);
						for (String nodeId : nodeList) {
							flowName = "Block-SLICOTS" + flowNum;
							flowId = "SLICOT" + flowNum;
							FlowManager.blockHost(nodeId,flowName,srcMac,tableId,flowId);
							flowNum += 1;
						}
						System.out.println("Detect attack and block host: " + srcMac);
						supportRequestList.removeRequest(srcMacRaw);
					} else {
						//Store data to pending list
						flowId = "SLICOT" + flowNum;
						flowName = "Temp-SLICOTS" + flowNum;
						mainPendingList.addRecord(srcMacRaw,dstMacRaw,srcPortRaw,dstPortRaw,"SYN",tableId,flowId);
						for (String nodeId : nodeList) {
							flowName = "Temp-SLICOTS" + flowNum;
							flowId = "SLICOT" + flowNum;
							FlowManager.installFlow(nodeId,flowName,dstMac,srcMac,tableId,flowId);
							flowNum += 1;
						}
						//To controller
						flowId = "SLICOT" + flowNum;
						flowName = "Temp-SLICOTS-CTRL" + flowNum;
						subPendingList.addRecord(srcMacRaw,dstMacRaw,srcPortRaw,dstPortRaw,"SYN",tableId,flowId);
						for (String nodeId : nodeList) {
							flowName = "Temp-SLICOTS-CTRL" + flowNum;
							flowId = "SLICOT" + flowNum;
							FlowManager.installFlowToController(nodeId,flowName,dstMac,srcMac,tableId,flowId);
							flowNum += 1;
						}
						//Update request num
						supportRequestList.updateRequestList(srcMacRaw);
						//System.out.println("Get SYN and add,update to pendinglist: "+ srcMac);
					}
				} else {//new SYN
					//add
					//Store data to pendding list
					flowId = "SLICOT" + flowNum;
					flowName = "Temp-SLICOTS" + flowNum;
					mainPendingList.addRecord(srcMacRaw,dstMacRaw,srcPortRaw,dstPortRaw,"SYN",tableId,flowId);
					for (String nodeId : nodeList) {
						flowName = "Temp-SLICOTS" + flowNum;
						flowId = "SLICOT" + flowNum;
						FlowManager.installFlow(nodeId,flowName,dstMac,srcMac,tableId,flowId);
						flowNum += 1;
					}
					//To controller
					flowId = "SLICOT" + flowNum;
					flowName = "Temp-SLICOTS-CTRL" + flowNum;
					subPendingList.addRecord(srcMacRaw,dstMacRaw,srcPortRaw,dstPortRaw,"SYN",tableId,flowId);
					for (String nodeId : nodeList) {
						flowName = "Temp-SLICOTS-CTRL" + flowNum;
						flowId = "SLICOT" + flowNum;
						FlowManager.installFlowToController(nodeId,flowName,dstMac,srcMac,tableId,flowId);
						flowNum += 1;
					}
					supportRequestList.addRequest(srcMacRaw);
					//System.out.println("Get SYN and add to pendinglist: "+ srcMac);
				}
			} else if (pktStr.equals(RST_PACKET)) {
				//search in pending list
				int numOfReq = supportRequestList.getNumOfReq(dstMacRaw);
				//if found get number of current request
				if (numOfReq > REQ_LIMIT) {
					//add rule to block host
					for (FlowRecord record : mainPendingList.pendingList) {
						if (Arrays.equals(srcMacRaw,record.getDstMac()) && Arrays.equals(dstMacRaw,record.getSrcMac())) {
							int tableId = record.getTableId();
							String flowId = record.getFlowId();
							for (String nodeId : nodeList) {
								FlowManager.deleteFlow(nodeId,tableId,flowId);
							}
						}
					}
					for (FlowRecord record : subPendingList.pendingList) {
						if (Arrays.equals(srcMacRaw,record.getDstMac()) && Arrays.equals(dstMacRaw,record.getSrcMac())) {
								int tableId = record.getTableId();
								String flowId = record.getFlowId();
								for (String nodeId : nodeList) {
									FlowManager.deleteFlow(nodeId,tableId,flowId);
							}
						}
					}
					mainPendingList.removeRecord(dstMacRaw,srcMacRaw);
					subPendingList.removeRecord(dstMacRaw,srcMacRaw);
					tableId = 0;
					long start = System.nanoTime();
					BlockedHost blockedHost = new BlockedHost(srcMacRaw,start);
					blockList.add(blockedHost);
					for (String nodeId : nodeList) {
						flowName = "Block-SLICOTS" + flowNum;
						flowId = "SLICOT" + flowNum;
						FlowManager.blockHost(nodeId,flowName,dstMac,tableId,flowId);
						flowNum += 1;
					}
					System.out.println("Detect attack and block host: " + dstMac);
					supportRequestList.removeRequest(srcMacRaw);
				} else {
					//find record and update to RST
					mainPendingList.updateRecordStatus(srcMacRaw,dstMacRaw,"RESET");
					subPendingList.updateRecordStatus(srcMacRaw,dstMacRaw,"RESET");
					//System.out.println("Get RESET and update status: " + srcMac + " to " + dstMac);
				}
			} else if (pktStr.equals(SYN_ACK_PACKET)) {	
				//find record and update to SYN_ACK
				mainPendingList.updateRecordStatus(srcMacRaw,dstMacRaw,"SYN_ACK");
				subPendingList.updateRecordStatus(srcMacRaw,dstMacRaw,"SYN_ACK");

			} else if (pktStr.equals(ACK_PACKET)) {
				//find and remove in pending list
				//add flow 
				for (FlowRecord record : subPendingList.pendingList) {
					if (Arrays.equals(srcMacRaw,record.getSrcMac()) & Arrays.equals(dstMacRaw,record.getDstMac())) {
						int tableId = record.getTableId();
						String flowId = record.getFlowId();
						for (String nodeId : nodeList) {
							FlowManager.deleteFlow(nodeId,tableId,flowId);
						}
					}
				}
				mainPendingList.removeRecord(srcMacRaw,dstMacRaw);
				subPendingList.removeRecord(srcMacRaw,dstMacRaw);
			}
        } else {
            // non IPv4 package
            //flood(notification.getPayload(), notification.getIngress());
		}
    };

	/**
     * @param payload
     * @return destination MAC address
     */
    public static byte[] extractDstMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_MAC_START_POSITION, DST_MAC_END_POSITION);
    };

    /**
     * @param payload
     * @return source MAC address
     */
    public static byte[] extractSrcMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, SRC_MAC_START_POSITION, SRC_MAC_END_POSITION);
    }

    /**
     * @param payload
     * @return source MAC address
     */
    public static byte[] extractEtherType(final byte[] payload) {
        return Arrays.copyOfRange(payload, ETHER_TYPE_START_POSITION, ETHER_TYPE_END_POSITION);
    }
  
	public static byte[] extractSrcPort(final byte[] payload) {
        return Arrays.copyOfRange(payload, SRC_PORT_START_POSITION, SRC_PORT_END_POSITION);
    }

	public static byte[] extractDstPort(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_PORT_START_POSITION, DST_PORT_END_POSITION);
    }

	public static byte[] extractPacketType(final byte[] payload) {
		return Arrays.copyOfRange(payload, CTRL_FLAG_START_POSITION, CTRL_FLAG_END_POSITION);
	}

    /**
     * @param bts
     * @return wrapping string value, baked upon binary MAC address
     */
    public static String byteToHexStr(final byte[] bts, String delimit) {
        StringBuffer macStr = new StringBuffer();

        for (int i = 0; i < bts.length; i++) {
            String str = Integer.toHexString(bts[i] & 0xFF);
            if( str.length()<=1 ){
                macStr.append("0");
            }
            macStr.append(str);

            if( i < bts.length - 1 ) { //not last delimit string
                macStr.append(delimit);
            }
        } // end of for !!

        return macStr.toString();
    }

    public ArrayList<String> getListNode() {
		ArrayList<String> result = new ArrayList<String>();
        String user = "admin";
  		String password = "admin";
		//inline will store the JSON data streamed in string format
		String inline = "";
		//	String authStr = "admin" + ":" + "admin";
      	//  String encodedAuthStr = Base64.encodeBase64String(authStr.getBytes());
		try {

        // Create URL = base URL + container
        URL url = new URL("http://127.0.0.1:8181/restconf/operational/opendaylight-inventory:nodes/");

        // Create authentication string and encode it to Base64
        String authStr = user + ":" + password;
        String encodedAuthStr = Base64.encodeBase64String(authStr.getBytes());

        // Create Http connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set connection properties
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic " + encodedAuthStr);
        connection.setRequestProperty("Accept", "application/json");

        // Get the response from connection's inputStream
        InputStream content = (InputStream) connection.getInputStream();

        BufferedReader in = new BufferedReader(new InputStreamReader(content));
        String line = "";
        while ((line = in.readLine()) != null) {
			inline = inline + line;
        }
			//Type caste the parsed json data in json object
			JSONObject jobj = new JSONObject(inline);
			JSONObject jobj1 = (JSONObject)jobj.get("nodes");
			//System.out.println(jobj1);
			JSONArray jobj2 = (JSONArray)jobj1.get("node");
			for (int i = 0; i < jobj2.length(); i++) {  // **line 2**
   				JSONObject childJSONObject = (JSONObject)jobj2.get(i);
  				String id = childJSONObject.get("id").toString();
				//System.out.println(name);
				result.add(id);
			}		

    	} catch (Exception e) {
        	e.printStackTrace();
    	}
		return result;		
    }

}

