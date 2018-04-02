package com.websocket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netCore.server.impl.AbstractGameServer;

public class GateServer extends AbstractGameServer{
	private static final Logger logger = LoggerFactory.getLogger(GateServer.class.getName());
	
	private static GateServer gameServer;
	
	private static Object lock = new Object();
	
	public GateServer(){
		//super();

		try {
			new WebSocketServer(8080).run();
		} catch (Exception e) {
			String errMsg = "【Gate服务器】缺少【内部监控】访问配置...服务器强行退出！";
			logger.error(errMsg);
			throw new RuntimeException(errMsg);
		}
	}
	
	/**
	 * 创建服务端服务器
	 * @author 
	 * 2014-11-28
	 * @return
	 */
	public static GateServer getInstance(){
		synchronized(lock){
			if(gameServer==null){
				gameServer = new GateServer();
			}
		}
		return gameServer;
	}
	
	public void init(){
		super.init();
		
		logger.info("初始化服务成功...");
	}
	
	@Override
	public void start() {
		logger.info("start");
	
		super.start();
		
	}

	@Override
	public void stop() {
		
		//1、停止 netty服务器、停止 netty客户端、关闭线程池、关闭任务池
		super.stop();
	}
	
	@Override
	public void startTimerServer() {
		
		super.startTimerServer();
		
		logger.info("所有定时任务启动成功!");
	}
}
