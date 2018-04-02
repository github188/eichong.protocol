package com.epcentre.server;

import io.netty.channel.Channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.EpCommClient;
import com.epcentre.cache.EpConcentratorCache;
import com.epcentre.cache.EpGunCache;
import com.epcentre.cache.MsgWhiteList;
import com.epcentre.constant.EpProtocolConstant;
import com.epcentre.model.RateInfo;
import com.epcentre.protocol.ChargeCmdResp;
import com.epcentre.protocol.ChargeEvent;
import com.epcentre.protocol.EpBespResp;
import com.epcentre.protocol.EpCancelBespResp;
import com.epcentre.protocol.EpDecodeProtocol;
import com.epcentre.protocol.EpEncodeProtocol;
import com.epcentre.protocol.EqVersionInfo;
import com.epcentre.protocol.Iec104Constant;
import com.epcentre.protocol.NoCardConsumeRecord;
import com.epcentre.protocol.StreamUtil;
import com.epcentre.protocol.UtilProtocol;
import com.epcentre.protocol.WmIce104Util;
import com.epcentre.protocol.ep.ApciHeader;
import com.epcentre.protocol.ep.AsduHeader;
import com.epcentre.sender.EpMessageSender;
import com.epcentre.service.EpBespokeService;
import com.epcentre.service.EpChargeService;
import com.epcentre.service.EpCommClientService;
import com.epcentre.service.EpConcentratorService;
import com.epcentre.service.EpGunService;
import com.epcentre.service.EpService;
import com.epcentre.service.EqVersionService;
import com.epcentre.service.RateService;
import com.epcentre.service.StatService;
import com.epcentre.service.UserService;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.FileUtils;
import com.epcentre.utils.StringUtil;

/**
 * 接受电桩客户端数据并处理
 * 
 * @author 2014-12-1 下午2:58:22
 */
public class EpMessageHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(EpMessageHandler.class);

	/**
	 * 接受电桩发送的消息进行处理
	 * 
	 * @author lwz 2015-3-19
	 * @param channel
	 * @param message
	 */
	public static void handleMessage(Channel channel, EpMessage message) {

		short nFrameType = message.getFrameType();
		byte[] msg = message.getBytes();
		
		// 根据帧类型处理相关的逻辑
		if (nFrameType == 1) {
			try {
				Iec104ProcessProtocolFrame(channel, msg);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (nFrameType == 2)// I Frame
		{
			Iec104ProcessFormatI(channel, msg);
		} else if (nFrameType == 3)// S Frame
		{
			
		} else if (nFrameType == 4)// U Frame
		{
			try {
				Iec104ProcessFormatU(channel, msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {

			logger.info("nFrameType error:{}" , nFrameType );


		}
		
		message = null;
	}
	

	public static void Iec104ProcessProtocolFrame(Channel channel, byte[] msg)
			throws IOException {

        int msgLen = msg.length;
			//离散桩
		if(msgLen != 15 && msgLen != 16)
		{
			//2.无效桩，强制关闭
			channel.close();
			return;
		}
		boolean initSuccess = false;
		String commClientIdentity="";
		byte[] retMsg= msg;
		try 
		{
			InputStream in = new ByteArrayInputStream(msg);
			
			StreamUtil.readWithLength(in, ApciHeader.NUM_HEAD + ApciHeader.NUM_LEN_FIELD + 1);
			
		    int commVersion = (int) StreamUtil.read(in); //
		    byte boot=0;
		    if(msgLen == 16)
		    {
		    	boot= StreamUtil.read(in);
		    }
		    if(boot>0)
		    {
		    	logger.info("Iec104ProcessProtocolFrame,boot>0,boot:{}",boot);
		    	//2.无效桩boot不正常，强制关闭
				channel.close();
				return;
		    }
		    String epCode = StreamUtil.readBCDWithLength(in, 8);
	                          
		    String station= StreamUtil.readBCDWithLength(in, 1);
		    
	        short nStationId = (short)Integer.parseInt(station);
	        
	        logger.debug("protocol frame epCode:{},nStationId:{}",epCode,nStationId);
	     
	        String epCodeZero=StringUtil.repeat("0", 15);
	    	if (nStationId > 0 && epCode.compareTo(epCodeZero)==0 )
	    	{
	    		 commClientIdentity=""+nStationId;
	    		 logger.debug("Iec104ProcessProtocolFrame protocol frame commClientIdentity:{}",commClientIdentity);
	    		  if(EpConcentratorService.initStationConnect(commVersion,nStationId,channel))
	    		  {
	    			  StatService.addCommConcentrator();

	    			  logger.debug("EpStationService.initStationConnect :{},success!\n",nStationId);

	    			  initSuccess = true;
	    		  }
	    		 
	    	 }
	    	 else  
	    	 {
	    		 logger.debug("Iec104ProcessProtocolFrame 1");
	    		  commClientIdentity=epCode;
	    		  logger.debug("protocol frame commClientIdentity:{}",commClientIdentity);
	    		  if(EpService.initDiscreteEpConnect(commVersion, epCode, channel))
	    		  {
	    			  StatService.addCommDiscreteEp();
	    			  logger.debug("protocol frame initSuccess commClientIdentity:{}",commClientIdentity);
	    			  initSuccess = true;
	    		  }
	    		  else
	    		  {
	    			  logger.debug("initDiscreteEpConnect epCode:{} fail",epCode);
	    		  }
	    	}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.debug("Iec104ProcessProtocolFrame 2,commClientIdentity:{}",commClientIdentity);
		 // 反馈协议侦
  	  if(initSuccess)
  	  {
  		logger.info("Iec104ProcessProtocolFrame.initConnet success!commClientIdentity:{}",commClientIdentity);
  		  
  		  if (MsgWhiteList.isOpen() && MsgWhiteList.find(commClientIdentity)) {
		      	     FileUtils.CreateCommMsgLogFile(commClientIdentity + ".log");
		      	     logger.debug("FileUtils.CreateCommMsgLogFile:{}",commClientIdentity);
	       }
  		  
  		  InnerApiMessageSender.sendMessage(channel, retMsg);
  		  // 启动侦
  		  //ApciHeader apciHeader = new ApciHeader();
  		  //apciHeader.setLength(4);
  		  //apciHeader.setUFrameType((byte) 0x07);
  		  byte startData[]= EpEncodeProtocol.do_startup();
  		  InnerApiMessageSender.sendMessage(channel, startData);
  		  
  		logger.info("Iec104ProcessProtocolFrame startup:{}",commClientIdentity);
  	  }
  	  else
  	  {
  		logger.info("Iec104ProcessProtocolFrame.initConnet fail!\n");
  	  }
  	  
	}

	public static void Iec104ProcessFormatU(Channel channel, byte[] msg)
			throws IOException {

		byte UCommand = msg[0];
		EpCommClient commClient = EpCommClientService.getCommClientByChannel(channel);
		if (null == commClient || commClient.getStatus() !=2) {
			logger.error("Iec104ProcessFormatU force close Channel:" + channel	+ "\n");
			// 没有发协议侦的客户端都关闭
			channel.close();
			return;
		}

		long now = DateUtil.getCurrentSeconds();

		commClient.setLastUseTime(now);
	

		if ((UCommand & Iec104Constant.WM_104_CD_STARTDT_CONFIRM) == Iec104Constant.WM_104_CD_STARTDT_CONFIRM) {

			byte[] bSetTimes = EpEncodeProtocol.do_set_time((short)0,0,0,0,commClient.getCommVersion());
			InnerApiMessageSender.sendMessage(channel, bSetTimes);

			// 下发黑名单
			// Vector<BlankUser> vBlankUsers =new Vector<BlankUser>(12);
			// BlankUser bu1=new BlankUser("1234567890123456",(byte)1);
			// vBlankUsers.add(bu1);
			// do_blank_list("3120141218010159",vBlankUsers);
	
	       byte[] bAllCall = EpEncodeProtocol.Package_all_call((short)6,0,0,0,commClient.getCommVersion());
			InnerApiMessageSender.sendMessage(channel, bAllCall);
		}
		else if ((UCommand & Iec104Constant.WM_104_CD_TESTFR) == Iec104Constant.WM_104_CD_TESTFR) {// add
																										// by
																										// hly

			byte[] testdata = EpEncodeProtocol.do_test_confirm();
			InnerApiMessageSender.sendMessage(channel, testdata);
		}
	
	}
	public static void Process130Record(EpCommClient epCommClient,Channel channel, int record_type,byte[] msg) {
		
		logger.info("Iec104ProcessFormatI 130 record_type:{},Identity:{}",record_type,epCommClient.getIdentity());
		
		InputStream in = new ByteArrayInputStream(msg);
		try
		{
		StreamUtil.readWithLength(in, ApciHeader.NUM_CTRL+ AsduHeader.H_LEN + 1);
		

		switch (record_type) {
		case Iec104Constant.M_CONSUME_MODEL_REQ: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			
			RateService.handleConsumeModelReq(epCommClient,epCode,time);
		}
			break;

		case Iec104Constant.M_CONSUME_MODE_RET:// 计费模型结果上行数据
		{
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			
		}
			break;
		case Iec104Constant.M_BUSINESS_TIME_RET:
			// 私有充电桩下发充电桩运营时间上行结果数据
			break;

		case Iec104Constant.M_NOCARD_PW_AUTH: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);

			int epGunNo = StreamUtil.read(in);
			String Account = StreamUtil.readBCD_div_ff_WithLength(in, 6);
			byte[] bPw = StreamUtil.readWithLength(in, 32);
			//logger.info("no card pw auth,params---epCode," + epCode	+ ",epGunNo" + epGunNo + ",Account" + Account);
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			handleNoCardAuthByPw(epCommClient,epCode, epGunNo, Account, bPw,time);
		}
		
			break;
		
		case Iec104Constant.M_BESPOKE_RET: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			logger.debug("M_BESPOKE_RET 1 msg:{}",WmIce104Util.ConvertHex(msg, 1));
			int epGunNo = StreamUtil.read(in);
			logger.debug("M_BESPOKE_RET gun:{}",epCode+epGunNo);
			int nRedo = StreamUtil.read(in);
			String bespokeNo = StreamUtil.readBCDWithLength(in,6);
			
			logger.debug("M_BESPOKE_RET bespokeNo:{}",bespokeNo);
			
			int successFlag = StreamUtil.read(in);
			int errorCode = StreamUtil.readUB2(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			
			logger.debug("M_BESPOKE_RET successFlag:{},errorCode:{}",successFlag,errorCode);
		
			EpBespResp bespResp = new EpBespResp(epCode,epGunNo,nRedo,bespokeNo,successFlag,errorCode);
			
			EpBespokeService.handleEpBespRet(epCommClient,bespResp,time);
			
		}
			break;

		case Iec104Constant.M_CANCEL_BESPOKE_RET: {
			
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);

			int epGunNo = StreamUtil.read(in);

			String Account = StreamUtil
					.readBCD_div_ff_WithLength(in, 6);
			short LockFlag = StreamUtil.read(in);
			
			String bespokeNo = StreamUtil.readBCDWithLength(in,6);
			
			// 6 执行取消预约的结果 BIN码 1Byte 1:表示取消预约成功
			short SuccessFlag = StreamUtil.read(in);

			// 7 时间 CP56Time2a 7Byte CP56Time2a

			byte[] sTime = StreamUtil.readWithLength(in, 7);
			long et = WmIce104Util.getP56Time2aTime(sTime);
			
			int errorCode = StreamUtil.readUB2(in);
	
			// {{电桩没对时，以服务器时间为准
			//java.util.Date dtEt = new Date();
			//et = dtEt.getTime() / 1000;
			// }}
			
			//System.out.print("onEpCancelBespRet,epCode:"+epCode+",epGunNo:"+epGunNo+"\n");
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			
			EpCancelBespResp cancelBespResp = new EpCancelBespResp(epCode, epGunNo,
					bespokeNo, LockFlag, SuccessFlag, et,errorCode);

			EpBespokeService.onEpCancelBespRet(epCommClient,cancelBespResp,cmdTimes);
		}
			break;
		case Iec104Constant.M_NOCARD_YZM_AUTH: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			String YZCode = StreamUtil.readBCD_div_ff_WithLength(in, 8);
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			// handleNoCardAuthByYZM(channel,epCode,epGunNo,YZCode);
		}
			break;
		case Iec104Constant.M_START_CHARGE_EVENT:
		{
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			String serialNo = StreamUtil.readBCDWithLength(in, 16);
			
			int meterNum= StreamUtil.readInt(in);
			
			byte[] bStartTimes = StreamUtil.readCP56Time2a(in);
			
			int remainTime =  StreamUtil.readInt(in);
			
			int retFlag = StreamUtil.read(in);
			
			long st = WmIce104Util.getP56Time2aTime(bStartTimes);
			
			logger.debug("Iec104Constant.M_START_CHARGE_EVENT: st:{}",st);
			int errorCode = StreamUtil.readUB2(in);
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);

			ChargeEvent chargeEvent = 
					new ChargeEvent(epCode,epGunNo,serialNo,meterNum,(int)st,remainTime,retFlag,errorCode);

			EpChargeService.handleStartElectricizeEventV3(epCommClient, chargeEvent,time);
		
			
		}
		break;
		case Iec104Constant.M_STOP_ELECTRICIZE_EVENT: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			// 2 表低示数 BIN码 4Byte 精确到小数点后三位，单位度
			int db_value = StreamUtil.readInt(in);
			String SerialNo = StreamUtil.readBCDWithLength(in, 16);

			// 4 充电开始时间 CP56Time2a
			byte[] bEndTimes = StreamUtil.readCP56Time2a(in);

			long et = WmIce104Util.getP56Time2aTime(bEndTimes);

			// 5 充电枪编号 BIN码 1Byte
			int epGunNo = StreamUtil.read(in);

			short nStopRet = (short)StreamUtil.readUB2(in);

			byte flag = StreamUtil.read(in);

			byte offstates = StreamUtil.read(in);
			byte successflag = StreamUtil.read(in);
			
			//int errorCode = StreamUtil.readUB2(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);

			
			int error = EpChargeService.handleStopElectricizeEvent(epCommClient, epCode,
					epGunNo, SerialNo, et, nStopRet, db_value, flag,
					offstates, successflag, bEndTimes,cmdTimes);
			if(error>0)
			{
				logger.info("stop electricize_event fail---epCode:" + epCode
						+ ",epGunNo:" + epGunNo + ",SerialNo:" + SerialNo
						+ ",db_value:" + db_value + ",nStopRet:" + nStopRet+ ",error:" + error);
				
			}
			else
			{
				logger.info("stop electricize_event success---epCode:" + epCode
						+ ",epGunNo:" + epGunNo + ",SerialNo:" + SerialNo
						+ ",db_value:" + db_value + ",nStopRet:" + nStopRet);
			}
		}
			break;

		case Iec104Constant.M_START_ELECTRICIZE_RET: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			
			int ret = StreamUtil.read(in);
			StreamUtil.readInt(in);
			short errorCause = (short)StreamUtil.readUB2(in);
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			ChargeCmdResp chargeCmdResp = new ChargeCmdResp(epCode,epGunNo,ret,errorCause);
			
			EpChargeService.handEpStartChargeResp(epCommClient,chargeCmdResp,cmdTimes);
			
		}
			break;
		case Iec104Constant.M_STOP_ELECTRICIZE_RET: {
		
		}
			break;
		case Iec104Constant.M_CONSUME_RECORD:
		{
			
			NoCardConsumeRecord consumeRecord = new NoCardConsumeRecord();
			
			logger.info("Iec104Constant.M_CONSUME_RECORD:"+WmIce104Util.ConvertHex(msg, 1));
			
			
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			
			consumeRecord.setEpCode(epCode);
			consumeRecord.setEpGunNo(epGunNo);
			

			// 2 交易流水号 BCD码 10Byte 16位交易代码
			consumeRecord.setSerialNo(StreamUtil.readBCDWithLength(
									in,
									com.epcentre.constant.EpProtocolConstant.LEN_BCD_ELECTRICIZE_SERIALNO));
			
			int accountType = (int)StreamUtil.read(in);
			
			consumeRecord.setAccountType(accountType);
			
			int userOrgin = (int)StreamUtil.readUB2(in);
			
			consumeRecord.setUserOrgin(userOrgin);
			
			// 3 用户编号 BCD码 8Byte 16位设备编码
			String Account = "";
			if(accountType == 1 )
			{
				byte[] bAccount = StreamUtil.readWithLength(in, 6);

				StreamUtil.readWithLength(in, 26);
				
				Account = WmIce104Util.bcd2StrDividFF(bAccount);
			}
			else
			{
				byte[] bAccount = StreamUtil.readWithLength(in, 32);
				
				//System.out.print("hex bAccount:"+WmIce104Util.ConvertHex(bAccount, 1));
				Account = StringUtil.getCString(bAccount);
			}

			consumeRecord.setEpUserAccount(Account);

			// 4 离线交易类型 BIN码 1Byte 0:
			consumeRecord.setTransType((int)StreamUtil.read(in));

			// 5 开始时间 BIN码 7Byte CP56Time2a
			byte[] bStartTime = StreamUtil.readCP56Time2a(in);
			consumeRecord.setStartTime(WmIce104Util
					.getP56Time2aTime(bStartTime));
			// 6 结束时间 BIN码 7Byte CP56Time2a
			byte[] bEndTime = StreamUtil.readCP56Time2a(in);
			consumeRecord.setEndTime(WmIce104Util
					.getP56Time2aTime(bEndTime));
			
			// 7 尖度量
			consumeRecord.setjDl(StreamUtil.readInt(in));
			// 8 尖金额
			consumeRecord.setjAmt(StreamUtil.readInt(in));
			
			// 9 峰度量
			consumeRecord.setfDl(StreamUtil.readInt(in));
			// 10 峰金额
			consumeRecord.setfAmt(StreamUtil.readInt(in));
			
			// 11平度量
			consumeRecord.setpDl(StreamUtil.readInt(in));
			// 12 平金额
			consumeRecord.setpAmt(StreamUtil.readInt(in));
			
			// 13谷度量
			consumeRecord.setgDl(StreamUtil.readInt(in));
			// 14 谷金额
			consumeRecord.setgAmt(StreamUtil.readInt(in));
			
			// 15总电量
			consumeRecord.setTotalDl(StreamUtil.readInt(in));
			
			// 16总充电金额
			consumeRecord.setTotalChargeAmt(StreamUtil.readInt(in));
			
			
			// 17服务费
			consumeRecord.setServiceAmt(StreamUtil.readInt(in));
			
			// 18开始充电总电量
			consumeRecord.setStartMeterNum(StreamUtil.readInt(in));
			// 19结束充电总电量
			consumeRecord.setEndMeterNum(StreamUtil.readInt(in));
			//20停止充电原因
			consumeRecord.setStopCause((int)StreamUtil.read(in));
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			
			EpChargeService.handleConsumeRecord(epCommClient,consumeRecord,cmdTimes);
			//保存最后接收的一条消费记录到文件
			if (MsgWhiteList.isOpen()&& MsgWhiteList.find(epCode)) 
			{
				FileUtils.writeLog(epCode + ".log",consumeRecord.toString());
				logger.debug("FileUtils.appendLog:{}",epCode);
			}
			
		}
		break;
		
		case Iec104Constant.M_QUERY_CONSUME_RECORD_RET:
		{
			logger.info("Iec104Constant.M_QUERY_CONSUME_RECORD:"+WmIce104Util.ConvertHex(msg, 1));
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			int entryNum = StreamUtil.readInt(in);
			logger.debug("Iec104Constant.M_QUERY_CONSUME_RECORD:epcode:{},entryNum:{}",epCode,entryNum);
			if(entryNum<=0)
			{
				logger.info("Iec104Constant.M_QUERY_CONSUME_RECORD:entryNum<=0,epcode:{},gunno:{}",epCode,epGunNo);
				return;
			}
			
			for(int i=0;i<entryNum;i++)	
			{
			
			NoCardConsumeRecord consumeRecord = new NoCardConsumeRecord();
			
			consumeRecord.setEpCode(epCode);
			consumeRecord.setEpGunNo(epGunNo);
			

			// 2 交易流水号 BCD码 10Byte 16位交易代码
			consumeRecord.setSerialNo(StreamUtil.readBCDWithLength2(
									in,
									com.epcentre.constant.EpProtocolConstant.LEN_BCD_ELECTRICIZE_SERIALNO));
			
			int accountType = (int)StreamUtil.read(in);
			
			consumeRecord.setAccountType(accountType);
			
			int userOrgin = (int)StreamUtil.readUB2(in);
			
			consumeRecord.setUserOrgin(userOrgin);
			
			// 3 用户编号 BCD码 8Byte 16位设备编码
			String Account = "";
			if(accountType == 1 )
			{
				byte[] bAccount = StreamUtil.readWithLength(in, 6);

				StreamUtil.readWithLength(in, 26);
				
				Account = WmIce104Util.bcd2StrDividFF(bAccount);
			}
			else
			{
				byte[] bAccount = StreamUtil.readWithLength(in, 32);
				
				//System.out.print("hex bAccount:"+WmIce104Util.ConvertHex(bAccount, 1));
				Account = StringUtil.getCString(bAccount);
			}

			consumeRecord.setEpUserAccount(Account);

			// 4 离线交易类型 BIN码 1Byte 0:
			consumeRecord.setTransType((int)StreamUtil.read(in));

			// 5 开始时间 BIN码 7Byte CP56Time2a
			byte[] bStartTime = StreamUtil.readCP56Time2a(in);
			consumeRecord.setStartTime(WmIce104Util
					.getP56Time2aTime(bStartTime));
			// 6 结束时间 BIN码 7Byte CP56Time2a
			byte[] bEndTime = StreamUtil.readCP56Time2a(in);
			consumeRecord.setEndTime(WmIce104Util
					.getP56Time2aTime(bEndTime));
			
			// 7 尖度量
			consumeRecord.setjDl(StreamUtil.readInt(in));
			// 8 尖金额
			consumeRecord.setjAmt(StreamUtil.readInt(in));
			
			// 9 峰度量
			consumeRecord.setfDl(StreamUtil.readInt(in));
			// 10 峰金额
			consumeRecord.setfAmt(StreamUtil.readInt(in));
			
			// 11平度量
			consumeRecord.setpDl(StreamUtil.readInt(in));
			// 12 平金额
			consumeRecord.setpAmt(StreamUtil.readInt(in));
			
			// 13谷度量
			consumeRecord.setgDl(StreamUtil.readInt(in));
			// 14 谷金额
			consumeRecord.setgAmt(StreamUtil.readInt(in));
			
			// 15总电量
			consumeRecord.setTotalDl(StreamUtil.readInt(in));
			
			// 16总充电金额
			consumeRecord.setTotalChargeAmt(StreamUtil.readInt(in));
			
			// 17服务费
			consumeRecord.setServiceAmt(StreamUtil.readInt(in));
			
			// 18开始充电总电量
			consumeRecord.setStartMeterNum(StreamUtil.readInt(in));
			// 19结束充电总电量
			consumeRecord.setEndMeterNum(StreamUtil.readInt(in));
			//20停止充电原因
			consumeRecord.setStopCause((int)StreamUtil.read(in));
			EpChargeService.handleHisConsumeRecord(epCommClient,consumeRecord);
			
		  }
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
		  byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);	
		  
		  EpService.queryConsumeRecord(epCode,epGunNo,entryNum);	
			
			
		}
		break;
	
		
		case Iec104Constant.M_BALANCE_WARNING: {
			logger.info("balance warning\n");
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = 1;

			String userAccount = StreamUtil.readBCDWithLength(in,
					com.epcentre.constant.EpProtocolConstant.LEN_BCD_ACCOUNT);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			  byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);	

			handleBalanceWarning(epCode, epGunNo, userAccount,cmdTimes);
		}
			break;

		case Iec104Constant.M_EP_HEX_FILE_SUMARY_REQ: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			//站地址
			short stationAddr = (short) StreamUtil.readUB2(in);
			//硬件型号
			byte[] hardwarecode = StreamUtil.readWithLength(in, 10);
			
		//	String hardwareNumber = StringUtil.getCString(hardwarecode);
			
			String hardwareNumber = StringUtil.getAscii(hardwarecode);
			if(hardwareNumber.compareTo(" ") ==0)
			{
				logger.error("handleEpHexFileSumaryReq fail,hardwareNumber error:",WmIce104Util.ConvertHex(msg, 1));
			    break;
			}
			//硬件主版本号
			int hardwareM = StreamUtil.read(in);	
			//硬件子版本号
			int hardwareA = StreamUtil.read(in);
            //分段字节大小
			int len =  StreamUtil.readUB2(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);	
			
			
			logger.debug("handleEpHexFileSumaryReq,epCode" + epCode+",stationAddr:"+ stationAddr+",hardwareNumber:"
			+hardwareNumber + ",hardwareVersion:V" +hardwareM+"."+hardwareA);
            
			EqVersionService.handleEpHexFileSumaryReq(channel, epCode,stationAddr, hardwareNumber,hardwareM,
					hardwareA,len,cmdTimes);
			
		}
			break;

		case Iec104Constant.M_EP_HEX_FILE_DOWN_REQ: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			//站地址
            short stationAddr = (short) StreamUtil.readUB2(in);
			
			EqVersionInfo versionInfo = new EqVersionInfo();
			//固件型号
			byte[] firmwarecode = StreamUtil.readWithLength(in, 8);
			
			//String firmwareNumber =StringUtil.getCString(firmwarecode);
			
			String firmwareNumber = StringUtil.getAscii(firmwarecode);
			if(firmwareNumber.compareTo(" ") ==0)
			{
				logger.error("handleEpHexFileDownReq fail,firmwareNumber error:",WmIce104Util.ConvertHex(msg, 1));
			    break;
			}
			versionInfo.setSoftNumber(firmwareNumber);
			//固件主版本号
			int firmwareM = StreamUtil.read(in);
			versionInfo.setSoftM(firmwareM);
			//固件副版本号
			int firmwareA = StreamUtil.read(in);
			versionInfo.setSoftA(firmwareA);
			//固件编译版本号
			int firmwareC = StreamUtil.readUB2(in);
			versionInfo.setSoftC(firmwareC);
			versionInfo.setSoftVersion(firmwareM+"."+firmwareA+"."+firmwareC);
			//段索引
			int SectionIndex =  StreamUtil.readUB2(in);
			
			//分段字节大小
			int len =  StreamUtil.readUB2(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			
			logger.debug("handleEpHexFileDownReq,epCode" + epCode+",stationAddr:"+ stationAddr+","
					+firmwareNumber + ",V" +firmwareM+"."+firmwareA+"."+firmwareC+ ",SectionIndex:" + SectionIndex );

			EqVersionService.handleEpHexFileDownReq(epCommClient, epCode,stationAddr,SectionIndex,versionInfo,len,cmdTimes);

		}
		break;

		case Iec104Constant.M_EP_STAT_RET: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNum = StreamUtil.read(in);
			int totalTime = 0;
			int totalCount = 0;
			int totalDl = 0;
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			  
			for (int i = 0; i < epGunNum; i++) {
				totalTime = totalTime + StreamUtil.readInt(in);
				totalCount = totalCount + StreamUtil.readInt(in);
				totalDl = totalDl + StreamUtil.readInt(in);
			}

			handleStatReq(epCommClient, epCode, totalTime, totalCount,
					totalDl,cmdTimes);
		}
			break;
		case Iec104Constant.M_COMM_SIGNAL_RET: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			short stationAddr = (short)StreamUtil.readUB2(in);
			int signal = StreamUtil.read(in);
			String epSystemTime="";
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				byte[] systime=StreamUtil.readCP56Time2a(in);
				epSystemTime= DateUtil.StringYourDate(DateUtil.toDate(WmIce104Util.getP56Time2aTime(systime)*1000));
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			    logger.info("M_COMM_SIGNAL_RET,epCode:{},epSystemTime:{}",epCode,epSystemTime);
			}
			
			if(stationAddr>0)
		    {}
			else
			{
				EpService.handleCommSignal(epCode, signal);
			}
		
		}
			break;
		case Iec104Constant.M_DC_SELF_CHECK_FINISHED: {

		}
			break;
		case Iec104Constant.M_EP_IDENTYCODE: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			int hour=0;
			int min = 0;
			int sec =0;
			//if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
	
			EpService.handleEpIdentyCodeQuery(epCode, epGunNo, hour,min,sec);
		}
			break;
		case Iec104Constant.M_LOCK_GUN_FAIL_WARNING: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			//枪口编号
			int epGunNo = StreamUtil.read(in);
			//失败原因 
			byte error = StreamUtil.read(in);

		}
			break;
		case Iec104Constant.M_EP_REPORT_DEVICE: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);

			int isSupportGunLock = StreamUtil.read(in);
			int isSupportGunSit = StreamUtil.read(in);
			int isSupportBmsComm = StreamUtil.read(in);
			int isSupportCarPlace = StreamUtil.read(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);

			handleEpDevices(epCommClient, epCode, epGunNo, isSupportGunLock,
					isSupportGunSit, isSupportBmsComm,
					isSupportCarPlace);
		}
			break;
		/*case Iec104Constant.M_OPEN_EPGUN_LOCK_EQUIP_RET: {
			int epGunNo = StreamUtil.read(in);
			int nRet = StreamUtil.read(in);
			int gunLockStatus = StreamUtil.read(in);

			logger.info("M_OPEN_EPGUN_LOCK_EQUIP_RET\r\n");

			handleOpenGunLock(channel, epCode, epGunNo, nRet,
					gunLockStatus);
		}
			break;*/
		case Iec104Constant.C_CARD_FRONZE_AMT: {
			
			//1	终端机器编码	BCD码	8Byte	16位编码	
			//2	充电桩接口标识	BIN码	1Byte		
			//3	内卡号	BIN码	32Byte	芯片卡号,位数不足用0x00补齐	
			//4	预冻金额	BIN码	4Byte	精确到小数点后2位，倍数100	

			logger.debug("C_CARD_FRONZE_AMT:{}",WmIce104Util.ConvertHex(msg,1));

			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			int epGunNo = StreamUtil.read(in);
			//3	内卡号	ASCII码	32Byte	长度不够,用0x00在尾部补齐	
			byte[] bCardInnerNo = StreamUtil.readWithLength(in,32);
			//logger.info("bCardInnerNo,bCardInnerNo:{}",WmIce104Util.ConvertHex(bCardInnerNo, 1));
			String  cardInnerNo = StringUtil.getCString(bCardInnerNo);
			
			int nFronzeAmt =  StreamUtil.readInt(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			
			EpChargeService.handleCardFronzeAmt(epCommClient, epCode,epGunNo,cardInnerNo,nFronzeAmt,cmdTimes);
			
		}
		break;
		case Iec104Constant.M_CARD_AUTH: {
			
			logger.debug("M_CARD_AUTH:{}"+WmIce104Util.ConvertHex(msg,1));
			
			//1	终端机器编码	BCD码	8Byte	16位编码	
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			//2	充电桩接口标识	BIN码	1Byte	从1开始	
			int epGunNo = StreamUtil.read(in);
			//3	内卡号	ASCII码	32Byte	长度不够,用0x00在尾部补齐	
			byte[] bCardInnerNo = StreamUtil.readWithLength(in,32);
			
			String  cardInnerNo = StringUtil.getCString(bCardInnerNo);
			
			
			byte[] bCardPasswordMd5 = StreamUtil.readWithLength(in,32);
			
			String cardPasswordMd5 = new String(bCardPasswordMd5);
			
			int userOrigin = StreamUtil.read(in);
			
			int hour=0;
			int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
				
			byte []cmdTimes = WmIce104Util.timeToByte(hour,min,sec);
			
			UserService.handleUserCardAuth(epCommClient,epCode,epGunNo,cardInnerNo,cardPasswordMd5,userOrigin,cmdTimes);
		}
			break;
		case Iec104Constant.M_DEVICE_VERSION_RET: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			//站地址
			short stationAddr = (short) StreamUtil.readUB2(in);
			//产品型号
			byte[] productcode = StreamUtil.readWithLength(in, 20);
			String productModule = StringUtil.getCString(productcode);
            
		    //硬件/固件数量
			int num = StreamUtil.read(in);
			Vector vecInfo = new Vector(num);
			for(int i=0;i<num;i++)
			{
				EqVersionInfo versionInfo = new EqVersionInfo();
				//硬件1型号
				byte[] hardwarecode = StreamUtil.readWithLength(in, 10);
				//String hardwareNumber = StringUtil.getCString(hardwarecode);
				String hardwareNumber = StringUtil.getAscii(hardwarecode);
				if(hardwareNumber.compareTo(" ") ==0)
				{
					logger.error("handleVersionAck,hardwareNumber error:",WmIce104Util.ConvertHex(msg, 1));
				    continue;
				}
				versionInfo.setHardwareNumber(hardwareNumber);
                //硬件1主版本号
			    int hardwareM = StreamUtil.read(in);
			    versionInfo.setHardwareM(hardwareM);
			    //硬件1子版本号
			    int hardwareA = StreamUtil.read(in);
			    versionInfo.setHardwareA(hardwareA);
			    versionInfo.setHardwareVersion(hardwareM+"."+hardwareA);
			    logger.debug("M_DEVICE_VERSION_RET: harmwareNumber:{},hardwareVer:{}",hardwareNumber,versionInfo.getHardwareVersion());
			    //固件1名称
			    byte[] firmwarecode = StreamUtil.readWithLength(in, 8);
			    String firmwareNumber = StringUtil.getCString(firmwarecode);
			    versionInfo.setSoftNumber(firmwareNumber);
			    //固件1主版本号
			    int firmwareM = StreamUtil.read(in);
			    versionInfo.setSoftM(firmwareM);
			    
			    //固件1副版本号
			    int firmwareA = StreamUtil.read(in);
			    versionInfo.setSoftA(firmwareA);
			    //固件1编译版本号

			    int firmwareC = StreamUtil.readUB2(in);
			    versionInfo.setSoftC(firmwareC);

			    
			    versionInfo.setSoftVersion(firmwareM+"."+firmwareA+"."+firmwareC);
			 
			    
			    vecInfo.add(versionInfo);
			}
			EqVersionService.handleVersionAck(epCommClient,epCode,stationAddr,vecInfo); 

		}
		 break;
		case Iec104Constant.M_HEX_FILE_UPDATE_RET: {
			// 1 终端机器编码 BCD码 8Byte 16位编码
			String epCode = StreamUtil.readBCDWithLength(in, 8);
			//站地址
			short stationAddr = (short) StreamUtil.readUB2(in);
			//硬件1型号
			byte[] hardwarecode = StreamUtil.readWithLength(in, 10);
			//String hardwareNumber = StringUtil.getCString(hardwarecode);
			
			String hardwareNumber = StringUtil.getAscii(hardwarecode);
			if(hardwareNumber.compareTo(" ") ==0)
			{
				logger.error("handleUpdateAck fail, hardwareNumber error:",WmIce104Util.ConvertHex(msg, 1));
			    break;
			}
			
			EqVersionInfo versionInfo = new EqVersionInfo();
			versionInfo.setHardwareNumber(hardwareNumber);

			//硬件主版本号
			int hardwareM = StreamUtil.read(in);
			//硬件子版本号
			int hardwareA = StreamUtil.read(in);
			
			versionInfo.setHardwareVersion(hardwareM+"."+hardwareA);
			
			byte[] softcode = StreamUtil.readWithLength(in, 8);
			String softNumber = StringUtil.getCString(softcode);
			versionInfo.setSoftNumber(softNumber);
			
			//固件1主版本号
			int firmwareM = StreamUtil.read(in);
			//固件1副版本号
			int firmwareA = StreamUtil.read(in);
			//固件1编译版本号
			int firmwareC = StreamUtil.readUB2(in);
			//更新成功标识
			int updateFlag = StreamUtil.read(in);
			
			if(updateFlag !=1)
			{
				logger.info("updateAck fail epCode:"+epCode+",stationAddr:"+stationAddr+",hardwareNumber:"+
						hardwareNumber+",hardwareVersion:"+versionInfo.getHardwareVersion()+
						",updateFlag:"+updateFlag);
				
			}
			else
			{
				logger.info("updateAck success epCode:"+epCode+",stationAddr:"+stationAddr+",hardwareNumber:"+
						hardwareNumber+",hardwareVersion:"+versionInfo.getHardwareVersion()+
						",updateFlag:"+updateFlag);
			    versionInfo.setSoftVersion(firmwareM+"."+firmwareA+"."+firmwareC);
			    EqVersionService.handleUpdateAck(stationAddr,epCode,versionInfo); 
	              
			}
		}
		break;
		case Iec104Constant.M_CONCENTROTER_SET_EP_RET: {
             
			int stationAddr = (int)StreamUtil.readUB2(in);
			int successFlag = (int)StreamUtil.read(in);
			int error = (int)StreamUtil.read(in);
		    
		    int hour=0;
		    int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			logger.info("M_CONCENTROTER_SET_EP_RET,stationAddr:{},successFlag:{}",stationAddr,successFlag);
	        
		}
		 break;
		case Iec104Constant.M_CONCENTROTER_GET_EP_RET: {
            
			int stationAddr = (int)StreamUtil.readUB2(in);
			int epCount = (int)StreamUtil.readUB2(in);
			String epCodes="";
			for(int i=0;i<epCount;i++)
			{
				String epCode = StreamUtil.readBCDWithLength(in, 8);
				epCodes +=epCode+",";
			}
		    
		    int hour=0;
		    int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			logger.info("M_CONCENTROTER_GET_EP_RET,stationAddr:{},epCodes:{}",stationAddr,epCodes);
	        
		}
		 break;
       case Iec104Constant.M_GET_CONSUME_MODEL_RET: {
            //1.电桩编号
    	   String epCode = StreamUtil.readBCDWithLength(in, 8);
    	   RateInfo rateInfo = new RateInfo();
    	   //2.费率ID
    	   rateInfo.setId((int)StreamUtil.read(in));
    	   //3.生效日期
    	   byte[] timeData=StreamUtil.readCP56Time2a(in);
    	   String beginTime= DateUtil.StringYourDate(DateUtil.toDate(WmIce104Util.getP56Time2aTime(timeData)*1000));
    	   //4.失效日期
    	   timeData=StreamUtil.readCP56Time2a(in);
    	   String endTime= DateUtil.StringYourDate(DateUtil.toDate(WmIce104Util.getP56Time2aTime(timeData)*1000));
      	    //5预冻结金额
    	   int nFroneAmt = (int)StreamUtil.readUB2(in);
    	   rateInfo.setFreezingMoney(UtilProtocol.intToBigDecimal2(nFroneAmt));
    	   //6最小冻结金额
    	   nFroneAmt = (int)StreamUtil.readUB2(in);
    	   rateInfo.setMinFreezingMoney( UtilProtocol.intToBigDecimal2(nFroneAmt));
    	   int num = (int)StreamUtil.read(in);
    	   String quantumDate="";
    	   for(int i=0;i<num;i++)
    	   {
    		   HashMap<String,Object>  dataMap=new HashMap<String,Object>();
    		   dataMap.put("st", (int)StreamUtil.readInt(in));
    		   dataMap.put("et", (int)StreamUtil.readInt(in));
    		   dataMap.put("mark", (int)StreamUtil.read(in));
    		   JSONObject jsonObject = JSONObject.fromObject(dataMap);
    		   quantumDate=quantumDate+jsonObject.toString()+",";
    	   }
    	   rateInfo.setQuantumDate(quantumDate);
    	   rateInfo.setJ_Rate(UtilProtocol.intToBigDecimal3((int)StreamUtil.readInt(in)));
    	   rateInfo.setF_Rate(UtilProtocol.intToBigDecimal3((int)StreamUtil.readInt(in)));
    	   rateInfo.setP_Rate(UtilProtocol.intToBigDecimal3((int)StreamUtil.readInt(in)));
    	   rateInfo.setG_Rate(UtilProtocol.intToBigDecimal3((int)StreamUtil.readInt(in)));
    	   rateInfo.setBespokeRate(UtilProtocol.intToBigDecimal3((int)StreamUtil.readInt(in)));
    	   rateInfo.setServiceRate(UtilProtocol.intToBigDecimal3((int)StreamUtil.readInt(in)));
    	   rateInfo.setWarnAmt(UtilProtocol.intToBigDecimal2((int)StreamUtil.readInt(in)));
    	   
		    int hour=0;
		    int min = 0;
			int sec =0;
			if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
			{
				hour = StreamUtil.read(in);
			    min = StreamUtil.read(in);
			    sec = StreamUtil.read(in);
			}
			
			byte []time = WmIce104Util.timeToByte(hour,min,sec);
			logger.info("M_GET_CONSUME_MODEL_RET,epCodes:{},rateInfo:{}",epCode,rateInfo.toString());
	        
		}
		 break;
		default:
			break;
		}
		}
		catch(Exception e)
		{
			logger.error("Iec104ProcessFormatI force close Channel:{}",channel);
			
		}
		in=null;
	}
	public static void Iec104ProcessFormatI(Channel channel, byte[] msg) {
		
		EpCommClient epCommClient = EpCommClientService.getCommClientByChannel(channel);
		if (null == epCommClient || epCommClient.getStatus() !=2) {
			logger.error("Iec104ProcessFormatI force close Channel:{}",channel);
			// 没有发协议侦的客户端都关闭
			channel.close();
			return;
		}
		if(epCommClient.getCommVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4)
		{
			short calcCrc = WmIce104Util.CRCSum(msg);
		    short revCrc = (short)((msg[msg.length-2]&0x0FF+(msg[msg.length-1]&0x0FF)*0x100)*2);
		    if(calcCrc != revCrc)
		    {
			   logger.error("Iec104ProcessFormatI,crc error, Identity:{}", epCommClient.getIdentity());
			   return;
		    }
		}
		long now = DateUtil.getCurrentSeconds();
		epCommClient.setLastUseTime(now);

		// add by hly
		int revInum = epCommClient.getRevINum();
		revInum = (revInum + 1) & 0x07FFF;
		epCommClient.setRevINum(revInum);

		boolean logMsg = true;
		
		byte bbyte = msg[ApciHeader.NUM_CTRL];
		short type = (short) (bbyte & 0xFF);
		try {

			switch (bbyte) {
			case Iec104Constant.C_IC_NA:// 总召唤确认帧
          	{
          		EpConcentratorCache stationClient=EpConcentratorService.getConCentrator(channel);	
   	    	    int stationAddr=0;
   	    	    String epCode="0000000000000000";
   			    if(stationClient != null )
   		        {
   				   stationAddr = stationClient.getPkId();
   		        }
   			    else
   			     {
   			    	epCode = epCommClient.getIdentity();
   			 	
   			    }
   			     if(epCode.length()<16)
   			    	break;
   			   //发送查询版本信息
   			     byte[] data= EpEncodeProtocol.do_eqversion_req(epCode, (short)stationAddr);
   			     byte[] cmdTimes = WmIce104Util.timeToByte();
   			     EpMessageSender.sendMessage(epCommClient,0,0,Iec104Constant.C_DEVICE_VERSION_REQ, data,cmdTimes,epCommClient.getCommVersion());
   		       
          	}
          	break;
			case Iec104Constant.M_SP_NA:// 1
              	{
              		byte[] sdata = EpEncodeProtocol.do_sframe(revInum);// add by												
    				InnerApiMessageSender.sendMessage(channel, sdata);// add by hly
              		logger.debug("Iec104ProcessFormatI 1 Identity:{}", epCommClient.getIdentity());
				//信息体地址
				EpDecodeProtocol.handleOneBitYx(channel, msg);
			}
				break;
			case Iec104Constant.M_DP_NA:// 1
          	{
          		byte[] sdata = EpEncodeProtocol.do_sframe(revInum);// add by												
				InnerApiMessageSender.sendMessage(channel, sdata);// add by hly
          		logger.debug("Iec104ProcessFormatI 3 Identity:{}", epCommClient.getIdentity());
			//信息体地址
			    EpDecodeProtocol.handleTwoBitYx(channel, msg);
		    }
			break;
			case Iec104Constant.M_ME_NB:// 11
			{
				byte[] sdata = EpEncodeProtocol.do_sframe(revInum);// add by												
				InnerApiMessageSender.sendMessage(channel, sdata);// add by hly
				
				logger.debug("Iec104ProcessFormatI 11 Identity:{}", epCommClient.getIdentity());
				EpDecodeProtocol.handleYc(channel, msg);
			}
			break;
				
            case Iec104Constant.M_MD_NA:
			{
				byte[] sdata = EpEncodeProtocol.do_sframe(revInum);// add by												
				InnerApiMessageSender.sendMessage(channel, sdata);// add by hly
				logger.debug("Iec104ProcessFormatI 132 Identity:{}", epCommClient.getIdentity());
				
				//信息体地址
				EpDecodeProtocol.handleVarYc(channel, msg);
			}
				break;
			case Iec104Constant.M_IT_NA:// 15
				break;
			case Iec104Constant.M_JC_NA:// 实时数据
			{
				byte[] sdata = EpEncodeProtocol.do_sframe(revInum);// add by												
				InnerApiMessageSender.sendMessage(channel, sdata);// add by hly

				int record_type = msg[ApciHeader.NUM_CTRL + AsduHeader.H_LEN];
				
				logger.debug("Iec104ProcessFormatI 134 record_type:{},Identity:{}", record_type ,epCommClient.getIdentity());
				
				if(record_type ==1 || record_type ==3)
				{
					EpDecodeProtocol.handleAcRealInfo(epCommClient.getCommVersion(),record_type,msg);
				}
				else
				{
					EpDecodeProtocol.handleWholeDcRealInfo(epCommClient.getCommVersion(),record_type,msg);		
				}

			}
			break;

			case Iec104Constant.M_RE_NA:// 130
				
				
				int record_type = (short)msg[ApciHeader.NUM_CTRL + AsduHeader.H_LEN]&0xff;

				Process130Record(epCommClient,channel,record_type,msg);
				
			default:
				break;
			}



		}

		catch (IOException e) {
			e.printStackTrace();
			
			
			logger.info("Exception,ch:{},msg:{}",channel,WmIce104Util.ConvertHex(msg,0));
		}

	}

	public static void handleStatReq(EpCommClient epCommClient, String epCode,
			int totalTime, int totalCount, int totalDl,byte []cmdTimes) {
	
		Map<String, Object> statMap = new ConcurrentHashMap<String, Object>();

		statMap.put("totalTime",totalTime);
		statMap.put("totalCount",totalCount);
		statMap.put("totalDl",totalDl);
		
		EpGunCache epGunCache = EpGunService.getEpGunCache(epCode,1);
		if(epGunCache != null)
		{
		   //epGunCache.handleEvent(EventConstant.EVENT_EP_STAT,0,0,null,statMap);
		}

	}

	public static void handleEpDevices(EpCommClient epCommClient, String epCode,
			int epGunNo, int isSupportGunLock, int isSupportGunSit,
			int isSupportBmsComm, int isSupportCarPlace) {

	}

	public static void handleOpenGunLock(EpCommClient epCommClient, String epCode,
			int epGunNo, int nRet, int gunLockStatus) {
	}

	

	public static void handleNoCardAuthByPw(EpCommClient CommClient, String epCode,
			int epGunNo, String account, byte[] pwMd5,byte []time) {
		/*try {

			int ret = UserService.checkUser(epCode, account, pwMd5);
			byte bSuccess = (byte) 0;
			if (ret == 0) {
				bSuccess = (byte) 1;
			}
			String strSuccessDesc = "";
			switch (ret) {
			case 0:
				strSuccessDesc = "0000";
				break;
			case 1:
				strSuccessDesc = "0001";
				break;
			case 2:
				strSuccessDesc = "0002";
				break;
			case 3:
				strSuccessDesc = "0003";
				break;
			case 4:
				strSuccessDesc = "0004";
				break;
			case 5:
				strSuccessDesc = "0005";
				break;
			case 6:
				strSuccessDesc = "0006";
				break;
			case 8:
				strSuccessDesc = "0008";
				break;
			default:
				strSuccessDesc = "1000";
				break;
			}
			
			//TblUserInfo u = UserService.getMemUserInfo(account);
			//java.math.BigDecimal Dec2 = new BigDecimal("100");
			int blance = (int) (u.getBalance().multiply(Dec2).doubleValue());

		byte[] yzmdata = CDZServerProtocol.do_nocard_auth_by_yzm((short)0,0,0,0,epCode,
					epGunNo, bSuccess, strSuccessDesc, blance, account);

			InnerApiMessageSender.sendMessage(CommClient.getChannel(), yzmdata);

		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	public static void handleBlankListReq(EpCommClient CommClient,String epCode) {
		/*try {
			Vector<BlankUser> vBlankUsers = new Vector<BlankUser>(12);
			// BlankUser bu1=new BlankUser("1234567890123456",(byte)1);
			// vBlankUsers.add(bu1);

				byte[] msg = CDZServerProtocol.do_blank_list((short)0,0,0,0,"3120141218010159",
					vBlankUsers);
			EpMessageSender.sendMessage(CommClient.getChannel(), msg);

		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}


	

	

	

	public static void handleBalanceWarning(String epCode, int epGunNo,
			String Account,byte []cmdTimes) {
	
		/*ChargeCache electricCacheObj = EpChargeService.getChargeCache( epCode,epGunNo);
		if (electricCacheObj != null) {
			byte[] msg = AppServerProtocol.BalanceWarning(epCode, Account);

			Channel appClientCh = AppClientService.getAppChannel(electricCacheObj.getClientIp());
			// 将需要转发的数据转发给app后台服务器	
			InnerApiMessageSender.gateSendToGame(appClientCh,AppManageCmd.G2A_ELECTRIC_BALANCE_WARNING,(Integer) 0, msg);
		} */
	}

}
