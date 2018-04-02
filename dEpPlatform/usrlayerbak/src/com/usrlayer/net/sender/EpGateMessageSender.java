package com.usrlayer.net.sender;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netCore.queue.RepeatMessage;
import com.usrlayer.service.CacheService;
import com.usrlayer.utils.DateUtil;

public class EpGateMessageSender {
	
	private static final Logger logger = LoggerFactory.getLogger(EpGateMessageSender.class);
	
	
	public static ChannelFuture  sendMessage(Channel channel,Object object) {
		
		if(channel == null)
		{
			
			return null;
		}
		
		if (!channel.isWritable()) {
			
			return null;
		}
		
		channel.writeAndFlush(object);
		
		return null;
	}

	public static ChannelFuture sendRepeatMessage(Channel channel,byte[] msg,String repeatMsgKey) {
		
		if(channel == null)
		{
			logger.info("sendRepeatMessage repeatMsgKey {} fail channel == null",repeatMsgKey);
			
			return null;
		}
		
		if (!channel.isWritable()) {
			logger.info("sendRepeatMessage repeatMsgKey {} fail channel:{} is not Writable,",repeatMsgKey,channel);
			return null;
		}
		
		channel.writeAndFlush(msg);
		
		//TODO:时间配置，是否重发需要参数化
		//if(GameConfig.resendEpMsgFlag ==1 )
		{
			//RepeatMessage repeatMsg= 
			//		new RepeatMessage(channel,3,GameConfig.resendEpMsgTime,repeatMsgKey,object);
			RepeatMessage repeatMsg= 
					new RepeatMessage(channel,3,30,repeatMsgKey,msg);
			
			repeatMsg.setLastSendTime( DateUtil.getCurrentSeconds());
			CacheService.putEpGateRepeatMsg(repeatMsg);
		}
		
		return null;
	}
	

}
