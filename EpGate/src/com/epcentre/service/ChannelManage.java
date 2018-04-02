package com.epcentre.service;

import io.netty.channel.Channel;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.NetObject;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.NetUtils;

public class ChannelManage {
	
	private static final Logger logger = LoggerFactory.getLogger("ChannelManage");
	
	private  Map<Channel,NetObject> mapCh2Client = new ConcurrentHashMap<Channel, NetObject>();
	private ConcurrentHashMap<String,NetObject>  mapClients=new ConcurrentHashMap<String,NetObject>();

	public String getCacheSize()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("ChannelManage:\n" );
        
        sb.append("通道对象:").append(mapCh2Client.size()).append("\n");
        sb.append("身份通道对象:").append(mapClients.size()).append("\n\n");
        
        return sb.toString();
		
	}
	public int addConnect(NetObject client)
	{
		 if(client==null)
		 {
			logger.error("addInitClient commClient==null");
			return -1;
		 }
		Channel ch = client.getChannel();
		if( ch ==null)
		{
			logger.error("addInitCommClient! Channel of commClient is null");
			return -2;
		}
		mapCh2Client.put(ch, client);
		return 1;
	}
	public int addClient(NetObject client)
	{
		if(client==null)
		{
			return -1;
		}
		Channel ch = client.getChannel();
		if( ch ==null)
		{
			return -2;
		}
		String identity = client.getIdentity();
		if(identity==null || identity.length()<=0)
		{
			return -3;
		}
		NetObject initClient = mapCh2Client.get(ch);
		if(initClient==null)
		{
			return -4;
		}
		
		mapClients.put(identity, client);
		return 1;
	}
	
	public void handleOldClient(NetObject newClient,String identity)
	{
		NetObject  oldClient = mapClients.get(identity);
		if(oldClient==null )
			return ;
		
		Channel oldCh = oldClient.getChannel();
		Channel newCh = newClient.getChannel();
		
		if(oldCh != newCh && 
				oldCh!=null)
		{
			logger.info("handleOldClient,identity:{}",identity);
			oldCh.close();
			remove(oldCh);
		}
		
	}
	
	
	
	
	public NetObject get(Channel ch)
	{
		 return mapCh2Client.get(ch);
	}
	public NetObject get(String phoneIdentity)
	{
		 return mapClients.get(phoneIdentity);
	}
	public void remove(NetObject  client)
	{
		remove(client.getChannel());
		remove(client.getIdentity());
	}
	public int remove(Channel ch)
	{
		logger.debug("remove!ch:{}",ch);
		if(ch==null )
		{
			//logger.error("removeChannel error!invalid ch ==null");
			return 0;
		}
		
		mapCh2Client.remove(ch);
		return 1;
	}
	/**
	 * 
	 * @param key
	 * @return
	 */
	public int remove(String key)
	{
		logger.debug("remove!key:{}",key);
		if(key==null  || key.length()<=0)
		{
			//logger.error("remove error!invalid key=null or empty");
			return 0;
		}
		mapClients.remove(key);
		return 1;
	}
	private int checkSignleTimeOut(long diff,int status,long initTimeout,long  timeout)
	{
		if(status < 2  )//只有链接的，没有初始化，10秒钟算超时
		{	
			if( diff > initTimeout) return 1;
		}
		else
		{
			if(diff> timeout) return 2;
		}
		return 0;
		
	}
	@SuppressWarnings("rawtypes")
	public String checkTimeOut(long initTimeout,long  timeout)
	{
		
		int count1=0;
		int count2=0;
		
		int chCount1 = mapCh2Client.size();
		int keyCount1 = mapClients.size();
		
		int count=0;
		
		long st = DateUtil.getCurrentSeconds();
	
		Iterator iter = mapCh2Client.entrySet().iterator();
		while (iter.hasNext()) 
		{
			count++;
			if(count>10)
			{
				NetUtils.sleep(1);
				count=0;
			}
			
			Map.Entry entry = (Map.Entry) iter.next();
			if(entry==null)
			{
				break;	
			}
			
			NetObject client=(NetObject) entry.getValue();
			
			if(client==null)
			{
				continue;	
			}
			long now = DateUtil.getCurrentSeconds();
			long lastTime = client.getLastUseTime();
			long diff = now - lastTime;
			
			int ret = checkSignleTimeOut(diff,client.getStatus(),initTimeout,timeout);
			if(ret==2)
			{
				String clientIdentity = client.getIdentity();
				client.handleNetTimeOut();
				remove(clientIdentity);
			}
			if(ret >0)
			{
				count1 += 1;
				
				if(ret==2)count2 += 1;
				
				logger.info("epChannelHandler,ep comm timeout close clientIdentity:{},status:{},diff:{},lastTime:{},channel:{}",
						new Object[]{client.getIdentity(),client.getStatus(),diff,lastTime,client.getChannel()});
				
				client.close();
				
				iter.remove();
				
			}
		}
		long et = DateUtil.getCurrentSeconds();
		
		int chCount2 = mapCh2Client.size();
		int keyCount2 = mapClients.size();
		
		return MessageFormat.format("cost {0} seconds,ch {1}---{2}---{3}  key:{4}---{5}---{6}", 
				et-st,chCount1,count1,chCount2,keyCount1,count2,keyCount2);
	}

}
