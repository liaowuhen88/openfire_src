package org.plugin.com.muclist.dao;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.spi.LocalMUCRoom;
import org.jivesoftware.openfire.muc.spi.MultiUserChatServiceImpl;
import org.jivesoftware.openfire.session.Session;
import org.plugin.com.muclist.MyIQHander;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.List;
import java.util.Map;


/**
* http://blog.csdn.net/zaoan_wx
* 在用户登录时获取用户所在会议室的信息
*/
public class MUCListt implements  SessionEventListener {

	private XMPPServer server;
	private MultiUserChatServiceImpl mucService;
	
	private IQRouter router;


	@Override
	public void sessionCreated(Session session) {
		JID userJid = session.getAddress();
		joinRooms(userJid);
	}

	@Override
	public void sessionDestroyed(Session session) {
	}

	@Override
	public void resourceBound(Session session) {
	}

	@Override
	public void anonymousSessionCreated(Session session) {
	}

	@Override
	public void anonymousSessionDestroyed(Session session) {
	}

	public MUCListt(){
		server = XMPPServer.getInstance();
		SessionEventDispatcher.addListener(this);
		System.out.println("Join room plugin is running!");
		
		IQHandler myHandler = new MyIQHander();
		IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
		iqRouter.addHandler(myHandler);
		
		router = XMPPServer.getInstance().getIQRouter();
	}

	public void joinRooms(JID userJid) {
		List<Map<String, String>> data = MUCDao.getMUCInfo(userJid.toBareJID());

		if (data == null || data.isEmpty()) {
			return;
		}
		Map<String, String> map = null;
		
		/**
		 * 构建iq的扩展包，用于发送用户所在房间的名称。
		 */
		Document document = DocumentHelper.createDocument();
		Element iqe = document.addElement("iq");
		iqe.addAttribute("type", "result");
		iqe.addAttribute("to", userJid.toFullJID());
		iqe.addAttribute("id", "YANG");
		
		Namespace namespace = new Namespace("", "YANG");
		Element muc = iqe.addElement("muc");
		muc.add(namespace);
		
		
		for (int i = 0, len = data.size(); i < len; i++) {
			map = data.get(i);

			String serviceID = map.get("serviceID");
			mucService = (MultiUserChatServiceImpl) server
					.getMultiUserChatManager().getMultiUserChatService(
							Long.parseLong(serviceID));
			String roomName = map.get("name");
			LocalMUCRoom room = (LocalMUCRoom) mucService.getChatRoom(roomName);

			//增加room和account信息
			Element roome = muc.addElement("room");
			roome.setText(room.getJID().toBareJID());
			roome.addAttribute("account", userJid.toFullJID());
		}
		//最后发送出去
		IQ iq = new IQ(iqe);
		System.out.println("iq " + iq.toXML());
		router.route(iq);
	}

	
}