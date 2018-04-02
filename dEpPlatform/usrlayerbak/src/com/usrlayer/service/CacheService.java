package com.usrlayer.service;

import io.netty.channel.Channel;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netCore.core.pool.TaskPoolFactory;
import com.netCore.queue.RepeatConQueue;
import com.netCore.queue.RepeatMessage;
import com.usrlayer.cache.ElectricPileCache;
import com.usrlayer.cache.EpGunCache;
import com.usrlayer.cache.UserCache;
import com.usrlayer.cache.UserRealInfo;
import com.usrlayer.config.GameBaseConfig;
import com.usrlayer.constant.BaseConstant;
import com.usrlayer.net.client.EpGateNetConnect;
import com.usrlayer.task.CheckEpGateNetTimeOutTask;
import com.usrlayer.utils.DateUtil;

public class CacheService {
	
	private static final Logger logger = LoggerFactory.getLogger(CacheService.class + BaseConstant.SPLIT + GameBaseConfig.layerName);
	
	public static void startEpGateCommTimer(long initDelay) {
		CheckEpGateNetTimeOutTask checkTask =  new CheckEpGateNetTimeOutTask();
				
		TaskPoolFactory.scheduleAtFixedRate("CHECK_EPGATENET_TIMEOUT_TASK", checkTask, initDelay, 10, TimeUnit.SECONDS);
	}

	private static int usrGateId;
	public static int getUsrGateId() {
		return usrGateId;
	}

	public static void setUsrGateId(int usrGateId) {
		CacheService.usrGateId = usrGateId;
	}

	/**
	 * EpGate缓存
	 */
	private static Map<Integer, EpGateNetConnect> epGateCommClents = new ConcurrentHashMap<Integer, EpGateNetConnect>();
	public static Map<Integer, EpGateNetConnect> getMapEpGate()
	{
		return epGateCommClents;
	}
	
	public static EpGateNetConnect getEpGate(int pkGateId )
	{
		return epGateCommClents.get(pkGateId);
	}
	public static void addEpGate(int pkGateId, EpGateNetConnect epGateClient)
	{
		epGateCommClents.put(pkGateId,epGateClient);
	}
	public static void removeEpGate(int pkGateId)
	{
		epGateCommClents.remove(pkGateId);
	}
	public static EpGateNetConnect getEpGate(String epCode )
	{
		ElectricPileCache epCache = getEpCache(epCode);
		if(epCache==null|| epCache.getGateid()==0)
			return null;
		
		return epGateCommClents.get(epCache.getGateid());
	}

	private static Map<Channel ,EpGateNetConnect> epGateChannel = new ConcurrentHashMap<Channel, EpGateNetConnect>();
	public static EpGateNetConnect getEpGateByCh(Channel channel)
	{
		return epGateChannel.get(channel);
	}
	public static void addEpGateByCh(Channel channel, EpGateNetConnect epGateClient)
	{
		epGateChannel.put(channel, epGateClient);
	}
	public static void removeEpGateByCh(Channel channel)
	{
		epGateChannel.remove(channel);
	}
	
	private static RepeatConQueue epGateReSendMsgQue = new RepeatConQueue();
	
	public static void putEpGateRepeatMsg(RepeatMessage mes)
	{
		logger.debug("putEpGateRepeatMsg,key:{}",mes.getKey());
		logger.debug("putEpGateRepeatMsg epGateReSendMsgQue,{}",epGateReSendMsgQue.count());
		epGateReSendMsgQue.put(mes);
		logger.debug("putEpGateRepeatMsg epGateReSendMsgQue,{}",epGateReSendMsgQue.count());
	}
	public static void putEpSendMsg(RepeatMessage mes)
	{
		logger.debug("putEpSendMsg,key:{}",mes.getKey());
		logger.debug("putEpSendMsg repeatMsgQue,{}",epGateReSendMsgQue.count());
		epGateReSendMsgQue.putSend(mes);
		logger.debug("putEpSendMsg repeatMsgQue,{}",epGateReSendMsgQue.count());
	}
	public static void removeEpRepeatMsg(String key)
	{
		logger.debug("removeEpRepeatMsg,key:{}",key);
		logger.debug("removeEpRepeatMsg start repeatMsgQue,{}",epGateReSendMsgQue.count());
		epGateReSendMsgQue.remove(key);
		logger.debug("removeEpRepeatMsg end repeatMsgQue,{}",epGateReSendMsgQue.count());
	}
	
	//电桩
	private static Map<String,ElectricPileCache> mapEpCache = new ConcurrentHashMap<String, ElectricPileCache>();
	public static Map<String,ElectricPileCache> getMapEpCache()
	{
		return mapEpCache;
	}
	public static int getCurrentEpCount()
	{
		return mapEpCache.size();
	}
	public  static void addEpCache(ElectricPileCache epCache){
		if(epCache!=null)
		{
			String epCode= epCache.getCode();
	
			mapEpCache.put(epCode,epCache);
		}
	}
    public static ElectricPileCache getEpCache(String epCode){
		ElectricPileCache electricUser = mapEpCache.get(epCode);
		return electricUser;
	}
	
    private static Map<String, EpGunCache> mapGun = new ConcurrentHashMap<String,EpGunCache>();
	public static Map<String, EpGunCache> getMapGun()
	{
		return mapGun;
	}
	public static EpGunCache getEpGunCache(String epCode,int epGunNo)
	{
		String combEpGunNo = epCode+ epGunNo;
		return mapGun.get(combEpGunNo);
	}
	public static void putEpGunCache(String epCode,int epGunNo,EpGunCache cache)
	{
		if(cache !=null && epCode !=null)
		{
			String combEpGunNo = epCode+ epGunNo;
			mapGun.put(combEpGunNo, cache);
			
		}
	}
	
	private static Map<String, UserCache> epUserInfoMap = new ConcurrentHashMap<String,UserCache>();
	private static Map<Integer, UserCache> epUserInfo2Map = new ConcurrentHashMap<Integer,UserCache>();
	public static Map<String, UserCache> getMapEpUserInfo()
	{
		return epUserInfoMap;
	}
	public static Map<Integer, UserCache> getMapEpUserInfo2()
	{
		return epUserInfo2Map;
	}
	
	public static void putUserCache(UserCache userCache)
	{
		if(userCache!=null)
		{
			epUserInfoMap.put(userCache.getAccount(), userCache);
			epUserInfo2Map.put(userCache.getId(), userCache);
		}
		
	}
	public static UserCache convertToCache(UserRealInfo userRealInfo)
	{
		if(userRealInfo == null)
			return null;
		
		String userAccount=  userRealInfo.getAccount();
		int userId = userRealInfo.getId();
		
		UserCache u= new UserCache(userId,userAccount,userRealInfo.getLevel());
		
		if(epUserInfoMap.get(userAccount)==null)
		{
			epUserInfoMap.put(userAccount, u);
		}
		if(epUserInfo2Map.get(userId)==null)
		{
			epUserInfo2Map.put(userId, u);
		}
		
		return u;
	}

	public static void checkEpGateTimeOut()
	{
		Iterator<Entry<Integer ,EpGateNetConnect>> iter = epGateCommClents.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			EpGateNetConnect commClient=(EpGateNetConnect) entry.getValue();
			if(commClient==null)
			{
				logger.error("EpGateNetConnect checkTimeOut commClient==null");
				continue ;
			}
		
			//需要连接
			boolean bNeedReConnect=false;
			//检查
			long connectDiff = DateUtil.getCurrentSeconds() - commClient.getLastUseTime();
			//检查连接，是否需要重连
			int commStatus = commClient.getStatus();
			if(commStatus == 0 || commStatus == 1)
			{
				int times = (commClient.getConnectTimes() / 6) + 1;
				if (connectDiff>(times * GameBaseConfig.reconnectInterval))
				{
					bNeedReConnect  = true;
				}
			}
			else
			{
				if(connectDiff>GameBaseConfig.netKeepLiveInterval)
				{
					bNeedReConnect  = true;
				}
				
			}
		
			if(bNeedReConnect)
			{
				commClient.reconnection();
			}
			
			long now = DateUtil.getCurrentSeconds();
			//1.检查连接是否活动,不活动的话发送心跳侦
			long activeDiff = now - commClient.getLastSendTime();
			if (activeDiff >= GameBaseConfig.heartInterval) {
				commClient.setLastSendTime(now);
				if(commClient.getChannel() != null)
				{
					EpGateService.sendHeart(commClient.getChannel());
				}
			}
		}
	}
}
