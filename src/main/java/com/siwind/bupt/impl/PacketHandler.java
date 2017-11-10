/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.util.Arrays;

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

	 /**
     * start position of control flag in array
     */
    private static final int CTRL_FLAG_START_POSITION = 50;

    /**
     * end position of control flag in array
     */
    private static final int CTRL_FLAG_END_POSITION = 52;


    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);

    public PacketHandler() {
        LOG.info("[Siwind] PacketHandler Initiated. ");
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        // TODO Auto-generated method stub

        // read src MAC and dst MAC
        byte[] dstMacRaw = extractDstMac(notification.getPayload());
        byte[] srcMacRaw = extractSrcMac(notification.getPayload());
        byte[] ethType   = extractEtherType(notification.getPayload());
		byte[] pktType   = extractPacketType(notification.getPayload());

        String dstMac = byteToHexStr(dstMacRaw, ":");
        String srcMac = byteToHexStr(srcMacRaw, ":");
        String ethStr = byteToHexStr(ethType, "");
		String pktStr = byteToHexStr(pktType, "");

 		System.out.println("from: " + srcMac + " to: " + dstMac + " Packet Type: " + pktStr);
        LOG.info("[Siwind] Received packet from MAC {} to MAC {}, EtherType=0x{} ", srcMac, dstMac, ethStr);
		if (Arrays.equals(ETH_TYPE_IPV4, etherType)) {
			if (pktType == SYN_PACKET) {
				//search in pending list
				//if found get number of current request
				if (numOfReq > REQ_LIMIT) {
					//add rule to block host
				}
				else {
					//Store data to pendding list
					//add temporary rule
				}
			}
			else if (pktType == RST_PACKET) {
				//search in pending list
				//if found get number of current request
				if (numOfReq > REQ_LIMIT) {
					//add rule to block host
				}
				else {
					//find record and update to RST
				}
			}
			else if (pktType == SYN_ACK_PACKET) {	
					//find record and update to SYN_ACK
			}
			else if (pktType == ACK_PACKET) {
				//find and remove in pendding list
				//add flow 
			}
            NodeConnectorRef previousPort = mac2portMapping.put(srcMac, notification.getIngress());
            if (previousPort != null && !notification.getIngress().equals(previousPort)) {
                NodeConnectorKey previousPortKey = InstanceIdentifierUtils.getNodeConnectorKey(previousPort.getValue());
                LOG.debug("mac2port mapping changed by mac {}: {} -> {}", srcMac, previousPortKey, ingressKey.getId());
            }
            // if dst MAC mapped:
            NodeConnectorRef destNodeConnector = mac2portMapping.get(dstMac);
            if (destNodeConnector != null) {
                synchronized (coveredMacPaths) {
                    if (!destNodeConnector.equals(notification.getIngress())) {
                        // add flow
                        addBridgeFlow(srcMac, dstMac, destNodeConnector);
                        addBridgeFlow(dstMac, srcMac, notification.getIngress());
                    } else {
                        LOG.debug("useless rule ignoring - both MACs are behind the same port");
                    }
                }
                LOG.debug("packetIn-directing.. to {}",
                        InstanceIdentifierUtils.getNodeConnectorKey(destNodeConnector.getValue()).getId());
                sendPacketOut(notification.getPayload(), notification.getIngress(), destNodeConnector);
            } else {
                // flood
                LOG.debug("packetIn-still flooding.. ");
                flood(notification.getPayload(), notification.getIngress());
            }
        } else {
            // non IPv4 package
            flood(notification.getPayload(), notification.getIngress());
		}
    }

    /**
     * @param payload
     * @return destination MAC address
     */
    public static byte[] extractDstMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_MAC_START_POSITION, DST_MAC_END_POSITION);
    }

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

/**
     * @param srcMac
     * @param dstMac
     * @param destNodeConnector
     */
    private void addBridgeFlow(MacAddress srcMac, MacAddress dstMac, NodeConnectorRef destNodeConnector) {
        synchronized (coveredMacPaths) {
            String macPath = srcMac.toString() + dstMac.toString();
            if (!coveredMacPaths.contains(macPath)) {
                LOG.debug("covering mac path: {} by [{}]", macPath,
                        destNodeConnector.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId());

                coveredMacPaths.add(macPath);
                FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
                FlowKey flowKey = new FlowKey(flowId);
                /**
                 * Path to the flow we want to program.
                 */
                InstanceIdentifier<Flow> flowPath = InstanceIdentifierUtils.createFlowPath(tablePath, flowKey);

                Short tableId = InstanceIdentifierUtils.getTableId(tablePath);
                FlowBuilder srcToDstFlow = FlowUtils.createDirectMacToMacFlow(tableId, DIRECT_FLOW_PRIORITY, srcMac,
                        dstMac, destNodeConnector);
                srcToDstFlow.setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())));

                dataStoreAccessor.writeFlowToConfig(flowPath, srcToDstFlow.build());
            }
        }
	}

private void flood(byte[] payload, NodeConnectorRef ingress) {
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(nodeConnectorId("0xfffffffb"));
        InstanceIdentifier<?> nodeConnectorPath = InstanceIdentifierUtils.createNodeConnectorPath(nodePath, nodeConnectorKey);
        NodeConnectorRef egressConnectorRef = new NodeConnectorRef(nodeConnectorPath);

        sendPacketOut(payload, ingress, egressConnectorRef);
    }

    private NodeConnectorId nodeConnectorId(String connectorId) {
        NodeKey nodeKey = nodePath.firstKeyOf(Node.class, NodeKey.class);
        StringBuilder stringId = new StringBuilder(nodeKey.getId().getValue()).append(":").append(connectorId);
        return new NodeConnectorId(stringId.toString());
    }

    private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
        InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder()
                .setPayload(payload)
                .setNode(new NodeRef(egressNodePath))
                .setEgress(egress)
                .setIngress(ingress)
                .build();
        packetProcessingService.transmitPacket(input);
	}

}

