package com.epcentre.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.AnalyzeCommClient;
import com.epcentre.service.AnalyzeService;
import com.epcentre.utils.DateUtil;
import com.netCore.model.conf.ClientConfig;
import com.netCore.netty.client.AbstractNettyClient;
import com.netCore.util.IPUtil;


public class AnalyzeNettyClient extends AbstractNettyClient{
	
	private static final Logger logger = LoggerFactory.getLogger(AnalyzeNettyClient.class);

	public AnalyzeNettyClient(ClientConfig clientConfig,ByteToMessageDecoder decoder, MessageToByteEncoder<?> encoder) {
		super(clientConfig, decoder, encoder);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx) {
		//logger.info("server close...");
		try {
			
			AnalyzeService.getCommClient().setStatus(0);
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}

	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx) {
		logger.info("AnalyzeNettyClient server conn...");
	    //服务服连接成功
		Channel channel = ctx.channel();
		
		AnalyzeCommClient commClient = AnalyzeService.getCommClient();
		if(commClient==null)//初始化就构造好的
		{
			
			logger.error("commClient==null");
			return ;
		}
		
		AnalyzeNettyClient nettyClient = commClient.getNettyClient();
		if(nettyClient != this)
		{
			logger.info("server close pre...");
			commClient.close();
			commClient.setNettyClient(this);
		}
		commClient.clearConnecTtimes();
		commClient.setStatus(2);
		commClient.setChannel(channel);
		commClient.setLastUseTime(DateUtil.getCurrentSeconds());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		
		logger.info("server exception...");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Object obj) {
		//logger.info("server receive...");
		Channel channel = ctx.channel();
		String name = IPUtil.getNameByChannel(channel);
		
		PhoneMessage message = (PhoneMessage) obj;
		AnalyzeMessageHandler.handleMessage(channel, message);
	}
	
	@Override
	public void stop() {
		super.stop();
		logger.info("server stop...");
		
	}

	@Override
	public void regiest(Channel channel) {
		
	
	}

}
