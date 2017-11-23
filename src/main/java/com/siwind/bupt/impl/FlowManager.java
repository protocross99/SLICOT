/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;

import com.siwind.bupt.impl.ODL;

public class FlowManager {

	// REST NBI Hydrogen for FlowGrogrammerNorthbound. More details at
	// http://opendaylight.nbi.sdngeeks.com/
	private static String FLOW_PROGRAMMER_REST_API = "restconf/config/opendaylight-inventory:nodes";

	// HTTP statuses for checking the call output
	private static final int NO_CONTENT = 204;
	private static final int CREATED = 201;
	private static final int OK = 200;

	public static boolean installFlow(String nodeId, String flowName, String dstMac, String srcMac, int tableId, String flowId) {

		HttpURLConnection connection = null;
		int callStatus = 0;
		// Creating the actual URL to call
		String baseURL =  ODL.URL + FLOW_PROGRAMMER_REST_API + "/node/" + nodeId + "/table/" + tableId + "/flow/" + flowId;

		try {
			// Create URL = base URL + container
			URL url = new URL(baseURL);

			// Create authentication string and encode it to Base64
			String authStr = ODL.USERNAME + ":" + ODL.PASSWORD;
			String encodedAuthStr = Base64.encodeBase64String(authStr.getBytes());
//			String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><flow xmlns=\"urn:opendaylight:flow:inventory\"><instructions><instruction><order>0</order><apply-actions><action><order>0</order><dec-mpls-ttl/></action></apply-actions></instruction></instructions><table_id>0</table_id><id>17</id><match><ethernet-match><ethernet-type><type>2048</type></ethernet-type><ethernet-destination><address>ff:ff:ff:00:ff:00</address></ethernet-destination><ethernet-source><address>00:00:00:00:23:ae</address></ethernet-source></ethernet-match></match><hard-timeout>0</hard-timeout><cookie>4</cookie><idle-timeout>0</idle-timeout><flow-name>FooXf4</flow-name><priority>2</priority></flow>";
 			
			String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
			+"<flow xmlns=\"urn:opendaylight:flow:inventory\">"
			+"<instructions>"
				+"<instruction>"
					+"<order>0</order>"
					+"<apply-actions>"
						+"<action>"
						+"<order>0</order>"
							+"<output-action>"
                        	+"<output-node-connector>NORMAL</output-node-connector>"
                        	+"<max-length>65535</max-length>"
                    		+"</output-action>"
						+"</action>"
					+"</apply-actions>"
				+"</instruction>"
			+"</instructions>"
			+"<table_id>" + tableId + "</table_id>"
			+"<id>" + flowId + "</id>"
			+"<match>"
				+"<ethernet-match>"
					+"<ethernet-type>"
						+"<type>2048</type>"
					+"</ethernet-type>"
					+"<ethernet-destination>"
						+"<address>" + dstMac + "</address>"
					+"</ethernet-destination>"
					+"<ethernet-source>"
						+"<address>" + srcMac + "</address>"
					+"</ethernet-source>"
				+"</ethernet-match>"
			+"</match>"
			+"<hard-timeout>0</hard-timeout>"
			+"<cookie>4</cookie>"
			+"<idle-timeout>0</idle-timeout>"
			+"<flow-name>" + flowName + "</flow-name>"
			+"<priority>20</priority>"
		+"</flow>";
			// Create Http connection
			connection = (HttpURLConnection) url.openConnection();

			// Set connection properties
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Authorization", "Basic "
					+ encodedAuthStr);
			connection.setRequestProperty("Content-Type", "application/xml");
			connection.setRequestProperty("Accept","application/xml");
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Set Post Data
			/*
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            		wr.writeBytes(xmlString);
            		wr.flush();
            		wr.close();
			*/
			OutputStream os = connection.getOutputStream();
            os.write(xmlString.getBytes());
			os.flush();
            os.close();

			// Getting the response code
			callStatus = connection.getResponseCode();

		} catch (Exception e) {
			System.err.println("Unexpected error while flow installation.. "
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
		}

		if (callStatus == CREATED) {
			//System.out.println("Flow installed Successfully on node: " + nodeId);
			return true;
		} else {
			//System.err.println("Failed to install flow on node:  " + nodeId + " due to " + callStatus);
			return false;
		}
	};

	public static boolean deleteFlow(String nodeId, int tableId, String flowId) {

		HttpURLConnection connection = null;
		int callStatus = 0;
		String baseURL =  ODL.URL + FLOW_PROGRAMMER_REST_API + "/node/" + nodeId + "/table/" + tableId + "/flow/" + flowId;
		//String baseURL = "http://127.0.0.1:8181/restconf/config/opendaylight-inventory:nodes/node/" + nodeId + "/table/" + tableId + "/flow/" + flowId;

		try {

			// Create URL = base URL + container
			URL url = new URL(baseURL);

			// Create authentication string and encode it to Base64
			String authStr = ODL.USERNAME + ":" + ODL.PASSWORD;
			String encodedAuthStr = Base64.encodeBase64String(authStr
					.getBytes());

			// Create Http connection
			connection = (HttpURLConnection) url.openConnection();

			// Set connection properties
			connection.setRequestMethod("DELETE");
			connection.setRequestProperty("Authorization", "Basic "
					+ encodedAuthStr);
			connection.setRequestProperty("Content-Type", "application/xml");

			callStatus = connection.getResponseCode();

		} catch (Exception e) {
			System.err.println("Unexpected error while flow deletion.."
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
		}

		if (callStatus == NO_CONTENT) {
			//System.out.println("Flow deleted Successfully");
			return true;
		} else {
			//System.err.println("Failed to delete the flow..." + callStatus);
			return false;
		}
	};

	public static boolean blockHost(String nodeId, String flowName, String srcMac, int tableId, String flowId) {

		HttpURLConnection connection = null;
		int callStatus = 0;
		// Creating the actual URL to call
		String baseURL =  ODL.URL + FLOW_PROGRAMMER_REST_API + "/node/" + nodeId + "/table/" + tableId + "/flow/" + flowId;

		try {
			// Create URL = base URL + container
			URL url = new URL(baseURL);

			// Create authentication string and encode it to Base64
			String authStr = ODL.USERNAME + ":" + ODL.PASSWORD;
			String encodedAuthStr = Base64.encodeBase64String(authStr.getBytes());
			String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                                +"<flow xmlns=\"urn:opendaylight:flow:inventory\">"
                                    +"<instructions>"
                                        +"<instruction>"
                                            +"<order>0</order>"
                                            +"<apply-actions>"
                                                +"<action>"
                                                    +"<order>0</order>"
                                                    +"<drop-action/>"
                                                +"</action>"
                                            +"</apply-actions>"
                                        +"</instruction>"
                                    +"</instructions>"
                                    +"<table_id>" + tableId + "</table_id>"
                                    +"<id>" + flowId + "</id>"
                                    +"<match>"
                                        +"<ethernet-match>"
                                            +"<ethernet-source>"
                                                +"<address>" + srcMac + "</address>"
                                            +"</ethernet-source>"
                                        +"</ethernet-match>"
                                    +"</match>"
                                    +"<hard-timeout>600</hard-timeout>"
                                    +"<cookie>3</cookie>"
                                    +"<idle-timeout>300</idle-timeout>"
                                    +"<flow-name>" + flowName + "</flow-name>"
                                    +"<priority>20</priority>"
                                +"</flow>";
			// Create Http connection
			connection = (HttpURLConnection) url.openConnection();

			// Set connection properties
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Authorization", "Basic "
					+ encodedAuthStr);
			connection.setRequestProperty("Content-Type", "application/xml");
			connection.setRequestProperty("Accept","application/xml");
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Set Post Data
			/*
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            		wr.writeBytes(xmlString);
            		wr.flush();
            		wr.close();
			*/
			OutputStream os = connection.getOutputStream();
            os.write(xmlString.getBytes());
			os.flush();
            os.close();

			// Getting the response code
			callStatus = connection.getResponseCode();

		} catch (Exception e) {
			System.err.println("Unexpected error while flow installation.. "
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
		}

		if (callStatus == CREATED) {
			//System.out.println("Flow installed Successfully");
			return true;
		} else {
			//System.err.println("Failed to install flow.. " + callStatus);
			return false;
		}
	}

	public static boolean installFlowToController(String nodeId, String flowName, String dstMac, String srcMac, int tableId, String flowId) {

		HttpURLConnection connection = null;
		int callStatus = 0;
		// Creating the actual URL to call
		String baseURL =  ODL.URL + FLOW_PROGRAMMER_REST_API + "/node/" + nodeId + "/table/" + tableId + "/flow/" + flowId;

		try {
			// Create URL = base URL + container
			URL url = new URL(baseURL);

			// Create authentication string and encode it to Base64
			String authStr = ODL.USERNAME + ":" + ODL.PASSWORD;
			String encodedAuthStr = Base64.encodeBase64String(authStr.getBytes());
			String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
+"<flow xmlns=\"urn:opendaylight:flow:inventory\">"
    +"<flow-name>" + flowName + "</flow-name>"
    +"<id>" + flowId + "</id>"
    +"<cookie>4</cookie>"
    +"<table_id>" + tableId + "</table_id>"
    +"<priority>10</priority>"
    +"<hard-timeout>0</hard-timeout>"
    +"<idle-timeout>0</idle-timeout>"
    +"<instructions>"
        +"<instruction>"
            +"<order>0</order>"
            +"<apply-actions>"
                +"<action>"
                    +"<order>0</order>"
                    +"<output-action>"
                        +"<output-node-connector>CONTROLLER</output-node-connector>"
						+"<max-length>65535</max-length>"
                    +"</output-action>"
                +"</action>"
            +"</apply-actions>"
        +"</instruction>"
    +"</instructions>"
    +"<match>"
        +"<ethernet-match>"
            +"<ethernet-type>"
                +"<type>2048</type>"
            +"</ethernet-type>"
            +"<ethernet-destination>"
                +"<address>" + dstMac + "</address>"
            +"</ethernet-destination>"
            +"<ethernet-source>"
                +"<address>" + srcMac + "</address>"
            +"</ethernet-source>"
        +"</ethernet-match>"
    +"</match>"
+"</flow>";

			// Create Http connection
			connection = (HttpURLConnection) url.openConnection();

			// Set connection properties
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Authorization", "Basic "
					+ encodedAuthStr);
			connection.setRequestProperty("Content-Type", "application/xml");
			connection.setRequestProperty("Accept","application/xml");
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Set Post Data
			/*
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            		wr.writeBytes(xmlString);
            		wr.flush();
            		wr.close();
			*/
			OutputStream os = connection.getOutputStream();
            os.write(xmlString.getBytes());
			os.flush();
            os.close();

			// Getting the response code
			callStatus = connection.getResponseCode();

		} catch (Exception e) {
			System.err.println("Unexpected error while flow installation.. "
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
		}

		if (callStatus == CREATED) {
			//System.out.println("Flow installed Successfully");
			return true;
		} else {
			//System.err.println("Failed to install flow.. " + callStatus);
			return false;
		}
	};

}
