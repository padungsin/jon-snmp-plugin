package net.krungsri.jon.plugin.server.alert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SmsSender extends AlertSender {

	private final Log log = LogFactory.getLog(SmsSender.class);

	// private static int TIMEOUT = 1500;
	// private static String TARGET_IP = "10.101.32.240";
	// private static int TARGET_PORT = 162;
	// private static String COMMUNITY = "public";
	//

	@Override
	public SenderResult send(Alert alert) {

		Resource res = alert.getAlertDefinition().getResource();
		Integer alertId = alert.getId();

		StringBuilder b = new StringBuilder();
		b.append("Alert: " + alert.getAlertDefinition().getName());
		// b.append("[id=").append(alertId).append("]: ");
		b.append(" | Resource: " + res.getName());
		// b.append("[id=").append(res.getId()).append("]");
		b.append(" | Message: " + alertParameters.getSimpleValue("message", ""));
		b.append(" | Brought by RHQ");

		try {
			sendAlertSms(b.toString());
		} catch (Exception e) {
			SenderResult senderResult = new SenderResult();
			senderResult.addFailureMessage("Failed to send SMS: " + e.getMessage());
			return senderResult;
		}

		SenderResult senderResult = new SenderResult();
		String tel = alertParameters.getSimpleValue("tel", null);
		senderResult.addSuccessMessage("SMS to " + tel + " sent");
		return senderResult;
	}

	/**
	 * Do the actual SMS sending. For this to succeed, it needs an auth token.
	 * 
	 * @param token
	 *            valid access token
	 * @param message
	 *            the message to send
	 */
	private void sendAlertSms(String message) throws Exception {
		String targetIp = preferences.getSimpleValue("targetIp", "10.101.32.240");
		int targetPort = Integer.parseInt(preferences.getSimpleValue("targetPort", "162"));
		String community = preferences.getSimpleValue("community", "public");
		int timeout = Integer.parseInt(preferences.getSimpleValue("timeout", "1500"));
		String oidString = preferences.getSimpleValue("oid", ".83.105.116.101.115.99.111.112.101.1");

		String tel = alertParameters.getSimpleValue("tel", null);
		String variable = "<SMS><TO>" + tel + "</TO><MSG>" + message + "</MSG></SMS>";

		// Setup destination
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(community)); // <= CHANGE public to
															// SNMP community
															// string of CA
															// UNICENTER
		UdpAddress targetAddress = new UdpAddress(targetIp + "/" + targetPort);
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(timeout);
		target.setVersion(SnmpConstants.version1);

		// Create PDU
		PDUv1 pdu = new PDUv1();
		pdu.setType(PDU.V1TRAP);
		pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
		pdu.setSpecificTrap(1);
		pdu.setAgentAddress(new IpAddress(targetIp));
		OID oid = new OID(oidString); // Sitescope, ID=1
		pdu.setEnterprise(oid);
		pdu.setTimestamp(120);
		VariableBinding vb = new VariableBinding(oid);
		// vb.setVariable(new
		// OctetString("<SMS><TO>66890130707,66898126783</TO><MSG>SNMPv1 Message
		// trap test by Guy sent at " + new Date()+ "</MSG></SMS>"));
		vb.setVariable(new OctetString(variable));
		pdu.add(vb);

		// send
		try {
			DefaultUdpTransportMapping udpTransportMap = new DefaultUdpTransportMapping();
			udpTransportMap.listen();
			Snmp snmp = new Snmp(udpTransportMap);
			ResponseEvent response = snmp.send(pdu, target);
			snmp.close();
			log.debug("pdu: " + pdu);
			log.debug("response: " + response);
			snmp.close();
		} catch (Exception e) {
			log.debug(e);
		}

		//
		// String gateway = preferences.getSimpleValue("gateway", "THSMS");
		// String tel = alertParameters.getSimpleValue("tel", null);
		//
		// if (tel == null)
		// throw new IllegalArgumentException("No telephone number given");
		//
		// String login = preferences.getSimpleValue("login", null);
		// String password = preferences.getSimpleValue("password", null);
		//
		// String[] params = new String[6];
		// params[0] = gateway;
		// params[1] = login;
		// params[2] = password;
		// params[3] = tel;
		// params[4] = message;
		//
		// try {
		// SMSSender smsSender = SMSSenderFactory.createSender(params);
		// smsSender.send();
		// } catch (Exception e) {
		// log.warn("Error while sending SMS " + e.getMessage());
		// log.warn(e);
		// throw new RuntimeException(e.getMessage());
		//
		// }

	}

}
