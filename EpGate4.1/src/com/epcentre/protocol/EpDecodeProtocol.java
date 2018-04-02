package com.epcentre.protocol;

import io.netty.channel.Channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.EpCommClient;
import com.epcentre.cache.EpGunCache;
import com.epcentre.cache.RealChargeInfo;
import com.epcentre.constant.EpProtocolConstant;
import com.epcentre.protocol.ep.ApciHeader;
import com.epcentre.protocol.ep.AsduHeader;
import com.epcentre.service.EpCommClientService;
import com.epcentre.service.EpConcentratorService;
import com.epcentre.service.EpGunService;
import com.epcentre.service.EpService;
import com.epcentre.utils.StringUtil;

public class EpDecodeProtocol {
	
	private static final Logger logger = LoggerFactory.getLogger(EpDecodeProtocol.class);
	
	
	public static void handleAcRealInfo(int  commVersion,int record_type,byte[] msg) 			
			throws IOException 
	{
	    switch (record_type) 
	    {
	        case 1: 
		        {
			       handleWholeAcRealInfo1(commVersion, msg);
		        }  
		        break;
	        case 3: 
	        {
		       handleWholeAcRealInfo3(commVersion, msg);
	        }  
	        break;
	       
	        default:
	        	break;

	     }
	}
	
	
	
	public static void handleWholeAcRealInfo1(int  commVersion,byte[] msg) throws IOException {
		
		if(commVersion>=3)
		{
			if(msg.length<56)
			{
				logger.debug("handleWholeAcRealInfo1,realData,msg.length<56,commVersion:{},msg:{}",commVersion,WmIce104Util.ConvertHex(msg,1));
				return;
				
			}
		}
		else
		{
			if(msg.length<40)
			{
				logger.debug("handleWholeAcRealInfo1,realData,msg.length<40,commVersion:{},msg:{}",commVersion,WmIce104Util.ConvertHex(msg,1));
				return;
				
			}
		}
		
		InputStream  in=new ByteArrayInputStream(msg);
		
		StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL+AsduHeader.H_LEN+1);
		//1	终端机器编码	BCD码	8Byte	16位编码
		String epCode = StreamUtil.readBCDWithLength(in, 8);
		//2
		int epGunNo=(int) StreamUtil.read(in);
		
		Map<Integer, SingleInfo> pointMapOneYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapTwoYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapYc = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapVarYc = new ConcurrentHashMap<Integer,SingleInfo>();
		
		//3//0:关,1:开
		int linked_status = (int) StreamUtil.read(in); //
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_LINKED_CAR, linked_status, "", 0);
		
		//4	工作状态	11:M_ME_NB_1	BIN码	1Byte	0:离线,1:故障,2待机;3工作,4欠压故障;5,过压故障,6过电流故障
		int working_status = (int)StreamUtil.read(in); //
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_WORKSTATUS, working_status, "", 0);
		
		
		//5.收枪成功
		short gun_close_status = (short)StreamUtil.read(in); //
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_SIT, gun_close_status, "", 0);
		
		//6.充电枪盖关闭状态
		short gun_lid_status = (short)StreamUtil.read(in); //
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_LID, gun_lid_status, "", 0);
		
		//7.车与桩建立通信信号
		short gun2car_comm_status=(short)StreamUtil.read(in); //
		//5	交流输入过压告警	1:M_SP_NA_1	BIN码	1Byte	布尔型,变化上传
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_COMM_WITH_CAR, gun2car_comm_status, "", 0);
		
		//8
		int value = (int) StreamUtil.read(in); //0:不过压，1:过压
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_IN_VOL_WARN, value, "", 0);
		
		//9	交流输入欠压告警	1:M_SP_NA_1	BIN码	1Byte	布尔型,变化上传
		value = (int) StreamUtil.read(in);//0:不欠压，1:欠压
		if(value==1)
			value=2;
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_IN_VOL_WARN, value, "", 0);
		
		
		//10	交流电流过负荷告警	1:M_SP_NA_1	BIN码	1Byte	布尔型,变化上传
		int loaded_warn = (int) StreamUtil.read(in);//0:不过负荷，1:过负荷
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_CURRENT_LOAD_WARN, loaded_warn, "", 0);
		
		
		//11	充电输出电压	11:M_ME_NB_1	BIN码	2Byte	精确到小数点后一位
		int nVol=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_VOL, nVol, "", 0);
		
		
		//12	充电输出电流	11:M_ME_NB_1	BIN码	2Byte	精确到小数点后二位
		int nCurrent=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_CURRENT, nCurrent, "", 0);
		
		
		
		
		//13	输出继电器状态	1:M_SP_NA_1	BIN码	1Byte	布尔型,变化上传://0:关,1:开
		int out_relay_status = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_OUT_RELAY_STATUS, out_relay_status, "", 0);
		
		
		//14	有功总电度	132:M_MD_NA_1	BIN码	4Byte	精确到小数点后二位
		int nDbNum= StreamUtil.readInt(in);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_ACTIVE_TOTAL_METERNUM, nDbNum, "", 0);
	
		//15	累计充电时间	11:M_ME_NB_1	BIN码	2Byte	单位:min
		int total_cd_time = (int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_TOTAL_TIME, total_cd_time, "", 0);
		
		
		if(commVersion >=3)
		{
			//车占位
			short car_place_status= (short)StreamUtil.read(in);
				
			RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CAR_PLACE, car_place_status, "", 0);
			
			//
			int chargeCost= StreamUtil.readInt(in);
			
				
			RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_COST, chargeCost, "", 0);
			//18
			int chargePrice= StreamUtil.readInt(in)*10;
			
			RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_PRICE, chargePrice, "", 0);
			
			int chargedMeterNum= StreamUtil.readInt(in);
			
			RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_METER_NUM, chargedMeterNum, "", 0);
			
			int carPlaceLock= (int)StreamUtil.read(in);
			
			RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_CAR_PLACE_LOCK, carPlaceLock, "", 0);
			
		}
		else
		{
			RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CAR_PLACE, 0, "", 0);
			RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_CAR_PLACE_LOCK, 0, "", 0);
			RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_COST, 0, "", 0);
			RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_PRICE, 0, "", 0);
			RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_METER_NUM, 0, "", 0);
			
		}
		
		EpGunCache gunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		if(gunCache == null)
		{
			logger.error("handleWholeAcRealInfo1,receive realData,epCode:{},epGunNo:{},gunCache is null",epCode, epGunNo);
		}
		else
		{
			logger.debug("handleWholeAcRealInfo1,receive realData,epCode:{},epGunNo:{}",epCode, epGunNo);
			gunCache.onRealDataChange(pointMapYc,11);
			gunCache.onRealDataChange(pointMapOneYx,1);
			gunCache.onRealDataChange(pointMapTwoYx,3);
			gunCache.onRealDataChange(pointMapVarYc,132);
		}
	}
	public static void handleWholeAcRealInfo3(int  commVersion,byte[] msg) throws IOException {
		
		
		InputStream  in=new ByteArrayInputStream(msg);
		StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL+AsduHeader.H_LEN+1);
		
		//1  充电机编号
		String epCode = StreamUtil.readBCDWithLength(in, 8);
		
		int epGunNo=(int) StreamUtil.read(in);
		
		Map<Integer, SingleInfo> pointMapOneYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapTwoYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapYc = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapVarYc = new ConcurrentHashMap<Integer,SingleInfo>();
		
		
		//3  充电机输出电压//11：M_ME_NB_1  BIN 码  2Byte
		int nVol=(int) StreamUtil.readUB2(in);
		//logger.debug("field 3:{}",nVol);
	
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_VOL, nVol, "", 0);
		
		//4  充电机输出电流
		//11：M_ME_NB_1  BIN 码  2Byte
		//精确到小数点后二位
		int nCurrent=(int) StreamUtil.readUB2(in);
		//logger.debug("field 4:{}",nCurrent);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_CURRENT, nCurrent, "", 0);
		

		/*5  充电机状态
		11：M_ME_NB_1  压缩 BCD 码  2Byte
		变化上传，0001- 告警 0002-待机 0003- 工作  0004- 离线
		0005-完成*/
		int value=(int) StreamUtil.read(in);
		//logger.debug("field 5:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_WORKSTATUS, value, "", 0);
		
		/*6  地锁
		11：M_ME_NB_1  压缩 BCD 码  2Byte
		变化上传，0001- 告警 0002-待机 0003- 工作  0004- 离线
		0005-完成*/
		value=(int) StreamUtil.read(in);
		//logger.debug("field 6:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_CAR_PLACE_LOCK, value, "", 0);
		/*7 有功总电度
		132：M_MD_NA_1  BIN 码  4Byte
		精确到小数点后一位*/
		value=(int) StreamUtil.readInt(in);
		//logger.debug("field 7:{}",value);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_ACTIVE_TOTAL_METERNUM, value, "", 0);
		
		//8.已充金额 BIN 码 4Byte 
		int chargeCost= StreamUtil.readInt(in);	
		//logger.debug("field 8:{}",chargeCost);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_COST, chargeCost, "", 0);
		
		//9.电价BIN 码 4Byte 
		int chargePrice= StreamUtil.readInt(in)*10;
		//logger.debug("field 9:{}",chargePrice);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_PRICE, chargePrice, "", 0);
		
		/*10已充总度数 BIN 码 4Byte*/
		int chargedMeterNum= StreamUtil.readInt(in);
		//logger.debug("field 10:{}",chargedMeterNum);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_METER_NUM, chargedMeterNum, "", 0);
		
		//11  累计充电时间
		//11：M_ME_NB_1  BIN 码  2Byte
		//单位：min
		value=(int) StreamUtil.readUB2(in);
		//logger.debug("field 11:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_TOTAL_TIME, value, "", 0);
		
		
		
		
		int value8bit = (int) StreamUtil.read(in)&0xff;
		//logger.debug("field value8bit:{}",value8bit);
		/*13  是否连接电池
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传*/
		value = value8bit%2;
		//logger.debug("field 13:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_LINKED_CAR, value, "", 0);
		/*	14枪座状态
			1：M_SP_NA_1  BIN 码  1Byte
			布尔型,  变化上传； 0 正常， 1
			异常*/
		value = (value8bit>>>1)%2;
		//logger.debug("field 14:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_SIT, value, "", 0);
		
		/*15充电枪盖状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>2)%2;
		//logger.debug("field 15:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_LID, value, "", 0);
		
		/*16车与桩建立通信信号
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>3)%2;
		//logger.debug("field 16:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_COMM_WITH_CAR, value, "", 0);
		
		/*17车位占用状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>4)%2;
		//logger.debug("field 17:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CAR_PLACE, value, "", 0);
	
		
		value8bit = (int) StreamUtil.read(in)&0xff;
		//logger.debug("field value8bit:{}",value8bit);
		/*18读卡器通讯异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = value8bit%2;
		//logger.debug("field 18:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CARD_READER_FAULT, value, "", 0);
		
		/*19急停按钮故障
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>1)%2;
		//logger.debug("field 19:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_URGENT_STOP_FAULT, value, "", 0);
		/*20避雷器故障
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>2)%2;
		//logger.debug("field 20:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_ARRESTER_EXCEPTION, value, "", 0);
		/*21绝缘检测故障
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>3)%2;
		//logger.debug("field 21:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_INSULATION_EXCEPTION, value, "", 0);
		
		/*22充电枪未连接告警
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>4)%2;
		//logger.debug("field 22:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_UNCONNECT_WARN, value, "", 0);
		/*23交易记录已满告警
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>5)%2;
		//logger.debug("field 23:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_TRANSRECORD_FULL_WARN, value, "", 0);
		/*24电度表异常
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>6)%2;
		//logger.debug("field 24:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_METER_ERROR, value, "", 0);
		/*25交流输入电压过压/欠压
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value8bit = (int) StreamUtil.read(in)&0xff;
		value = value8bit%4;
		//logger.debug("field 25:{}",value);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YX_2_AC_IN_VOL_WARN, value, "", 0);
		//
		/*26充电机过温故障
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>2)%4;
		//logger.debug("field 26:{}",value);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YX_2_CHARGE_OVER_TEMP, value, "", 0);
		
		/*27交流电流过负荷告警
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>4)%4;
		//logger.debug("field 27:{}",value);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YX_2_AC_CURRENT_LOAD_WARN, value, "", 0);
		
		/*28输出继电器状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>6)%4;
		//logger.debug("field 28:{}",value);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YX_2_OUT_RELAY_STATUS, value, "", 0);
	
		EpGunCache gunCache = EpGunService.getEpGunCache(epCode, epGunNo);	
		if(gunCache == null)
		{
			logger.error("handleWholeAcRealInfo3,receive realData,epcode{},gunno{},gunCache=NULL ",epCode,epGunNo);
			return;
		}
		logger.debug("handleWholeAcRealInfo3,receive realData,epCode:{}, epGunNo:{}",epCode, epGunNo);
		gunCache.onRealDataChange(pointMapYc,11);
		gunCache.onRealDataChange(pointMapOneYx,1);
		gunCache.onRealDataChange(pointMapTwoYx,3);
		gunCache.onRealDataChange(pointMapVarYc,132);
	}
	
    
  
		@SuppressWarnings("unchecked")
	public static void handleWholeDcRealInfo(int commVersion,int record_type,byte[] msg) throws IOException
	{
		if(record_type==2)
			handleWholeDCRealInfo2(commVersion, msg);
		else
			handleWholeDCRealInfo4(commVersion, msg);
			
	}
	@SuppressWarnings("unchecked")
	public static void handleWholeDCRealInfo4(int commVersion,byte[] msg) throws IOException
	{
		if(commVersion>=3)
		{
			if(msg.length<56)
			{
				return;
				
			}
		}
			
		InputStream  in=new ByteArrayInputStream(msg);
		StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL+AsduHeader.H_LEN+1);
		
		//1  充电机编号
		String epCode = StreamUtil.readBCDWithLength(in, 8);
		
		int epGunNo=(int) StreamUtil.read(in);
		
		Map<Integer, SingleInfo> pointMapOneYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapTwoYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapYc = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapVarYc = new ConcurrentHashMap<Integer,SingleInfo>();
		
		
		//3  充电机输出电压//11：M_ME_NB_1  BIN 码  2Byte
		int nVol=(int) StreamUtil.readUB2(in);
		//logger.debug("field 3:{}",nVol);
	
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_VOL, nVol, "", 0);
		
		//4  充电机输出电流
		//11：M_ME_NB_1  BIN 码  2Byte
		//精确到小数点后二位
		int nCurrent=(int) StreamUtil.readUB2(in);
		//logger.debug("field 4:{}",nCurrent);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_CURRENT, nCurrent, "", 0);
		

		/*5  充电机状态
		11：M_ME_NB_1  压缩 BCD 码  2Byte
		变化上传，0001- 告警 0002-待机 0003- 工作  0004- 离线
		0005-完成*/
		int value=(int) StreamUtil.read(in);
		//logger.debug("field 5:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_WORKSTATUS, value, "", 0);
		
		/*6  地锁
		11：M_ME_NB_1  压缩 BCD 码  2Byte
		变化上传，0001- 告警 0002-待机 0003- 工作  0004- 离线
		0005-完成*/
		value=(int) StreamUtil.read(in);
		//logger.debug("field 6:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_CAR_PLACE_LOCK, value, "", 0);
		/*7 有功总电度
		132：M_MD_NA_1  BIN 码  4Byte
		精确到小数点后一位*/
		value=(int) StreamUtil.readInt(in);
		//logger.debug("field 7:{}",value);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_ACTIVE_TOTAL_METERNUM, value, "", 0);
		
		//8.已充金额 BIN 码 4Byte 
		int chargeCost= StreamUtil.readInt(in);	
		//logger.debug("field 8:{}",chargeCost);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_COST, chargeCost, "", 0);
		
		//9.电价BIN 码 4Byte 
		int chargePrice= StreamUtil.readInt(in)*10;
		//logger.debug("field 9:{}",chargePrice);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_PRICE, chargePrice, "", 0);
		
		/*10已充总度数 BIN 码 4Byte*/
		int chargedMeterNum= StreamUtil.readInt(in);
		//logger.debug("field 10:{}",chargedMeterNum);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_METER_NUM, chargedMeterNum, "", 0);
		
		//11  累计充电时间
		//11：M_ME_NB_1  BIN 码  2Byte
		//单位：min
		value=(int) StreamUtil.readUB2(in);
		//logger.debug("field 11:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_TOTAL_TIME, value, "", 0);
		
		//12 剩余时间充电时间
				//11：M_ME_NB_1  BIN 码  2Byte
		//单位：min
		value=(int) StreamUtil.readUB2(in);
		
		//logger.debug("field 12:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_REMAIN_TIME, value, "", 0);
		
		int value8bit = (int) StreamUtil.read(in)&0xff;
		//logger.debug("field value8bit:{}",value8bit);
		/*13  是否连接电池
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传*/
		value = value8bit%2;
		//logger.debug("field 13:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_LINKED_CAR, value, "", 0);
		/*	14枪座状态
			1：M_SP_NA_1  BIN 码  1Byte
			布尔型,  变化上传； 0 正常， 1
			异常*/
		value = (value8bit>>>1)%2;
		//logger.debug("field 14:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_SIT, value, "", 0);
		
		/*15充电枪盖状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>2)%2;
		//logger.debug("field 15:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_LID, value, "", 0);
		
		/*16车与桩建立通信信号
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>3)%2;
		//logger.debug("field 16:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_COMM_WITH_CAR, value, "", 0);
		
		/*17车位占用状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>4)%2;
		//logger.debug("field 17:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CAR_PLACE, value, "", 0);
	
		/*18读卡器通讯异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value8bit = (int) StreamUtil.read(in)&0xff;
		//logger.debug("field value8bit:{}",value8bit);
		value = value8bit%2;
		//logger.debug("field 18:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CARD_READER_FAULT, value, "", 0);
		
		/*19急停按钮故障
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>1)%2;
		//logger.debug("field 19:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_URGENT_STOP_FAULT, value, "", 0);
		/*20避雷器故障
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>2)%2;
		//logger.debug("field 20:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_ARRESTER_EXCEPTION, value, "", 0);
		/*21绝缘检测故障
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>3)%2;
		//logger.debug("field 21:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_INSULATION_EXCEPTION, value, "", 0);
		
		/*22充电枪未连接告警
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>4)%2;
		//logger.debug("field 22:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_UNCONNECT_WARN, value, "", 0);
		/*23交易记录已满告警
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>5)%2;
		//logger.debug("field 23:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_TRANSRECORD_FULL_WARN, value, "", 0);
		/*24电度表异常
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>6)%2;
		//logger.debug("field 24:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_METER_ERROR, value, "", 0);
		/*25交流输入电压过压/欠压
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value8bit = (int) StreamUtil.read(in)&0xff;
		value = value8bit%4;
		//logger.debug("field 25:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_IN_VOL_WARN, value, "", 0);
		//
		/*26充电机过温故障
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>2)%4;
		//logger.debug("field 26:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_CHARGE_OVER_TEMP, value, "", 0);
		
		/*27交流电流过负荷告警
		 1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>4)%4;
		//logger.debug("field 27:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_CURRENT_LOAD_WARN, value, "", 0);
		
		/*28输出继电器状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>6)%4;
		//logger.debug("field 28:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_OUT_RELAY_STATUS, value, "", 0);
		
		//29  SOC
				//11：M_ME_NB_1  BIN 码  2Byte
				//整型
		value=(int)StreamUtil.read(in)&0xff;
		//logger.debug("field 29:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_SOC, value, "", 0);
		
		
		//30  电池组最低温度
		//11：M_ME_NB_1  BIN 码  2Byte
		//精确到小数点后一位
		value=(int) StreamUtil.readUB2(in);
		//logger.debug("field 30:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_BATTRY_LOWEST_TEMP, value, "", 0);
		
		//31  电池组最高温度
		//11：M_ME_NB_1  BIN 码  2Byte
		value=(int) StreamUtil.readUB2(in);
		//logger.debug("field 31:{}",value);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_BATTRY_HIGHEST_TEMP, value, "", 0);
		
		//32电池反接故障
		value8bit = (int) StreamUtil.read(in)&0xff;
		value = value8bit%2;
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_BATTRY_ERROR_LINK, value, "", 0);
		
		//logger.debug("field 32:{}",value);
		
		//33烟雾报警故障
		value = (value8bit>>>1)%2;
		//logger.debug("field 33:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_FOGS_WARN, value, "", 0);
		
		
		
		//
		
		/*34  BMS 通信异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (value8bit>>>2)%2;
		//logger.debug("field 34:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_BMS_ERROR, value, "", 0);
		
		/*35直流电度表异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传；0：不过
		压，1 过压*/
		value = (value8bit>>>3)%2;
		//logger.debug("field 35:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_DCMETER_ERROR, value, "", 0);
		
		value = (value8bit>>>4)%2;
		//logger.debug("field 35:{}",value);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_DC_OUT_OVER_CURRENT_WARN, value, "", 0);
		
		value8bit = (int) StreamUtil.read(in)&0xff;
		//充电模式
		value = value8bit%4;
		//logger.debug("field 36:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_DC_SUPPLY_CHARGE_STYLE, value, "", 0);
		//整车动力蓄电池SOC告警
		value = (value8bit>>>2)%4;
		//logger.debug("field 37:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_BATTRY_SOC_WARN, value, "", 0);
		//蓄电池模块采样点过温告警
		value = (value8bit>>>4)%4;
		//logger.debug("field 38:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_BATTRY_SAMPLE_OVER_TEMP, value, "", 0);
		//输出连接器过温
		value = (value8bit>>>6)%4;
		//logger.debug("field 39:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_OUT_LINKER_OVER_TEMP, value, "", 0);
		//整车动力蓄电池组输出连接器连接状态
		value8bit = (int) StreamUtil.read(in)&0xff;
		value = value8bit%4;
		//logger.debug("field 40:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_OUT_LINKER_STATUS, value, "", 0);
		//整车蓄电池充电过流告警
		value = (value8bit>>>2)%4;
		//logger.debug("field 41:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_BATTRY_CHARGE_OVER_CURRENT, value, "", 0);
		//直流母线输出过压/欠压
		value = (value8bit>>>4)%4;
		//logger.debug("field 42:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_DC_OUT_VOL_WARN, value, "", 0);
		
		value = (value8bit>>>6)%4;
		//logger.debug("field 42:{}",value);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_BMS_VOL_WARN, value, "", 0);
		
		EpGunCache gunCache = EpGunService.getEpGunCache(epCode, epGunNo);	
		if(gunCache == null)
		{
			logger.error("handleWholeDcRealInfo4,receive realData,epcode{},gunno{} gunCache=NULL",epCode,epGunNo);
			return;
		}
		logger.debug("handleWholeDcRealInfo4,receive realData,epCode:{}, epGunNo:{}",epCode, epGunNo);
		gunCache.onRealDataChange(pointMapYc,11);
		gunCache.onRealDataChange(pointMapOneYx,1);
		gunCache.onRealDataChange(pointMapTwoYx,3);
		gunCache.onRealDataChange(pointMapVarYc,132);
			
	}
	@SuppressWarnings("unchecked")
	public static void handleWholeDCRealInfo2(int commVersion,byte[] msg) throws IOException
	{
		if(commVersion>=3)
		{
			if(msg.length<56)
			{
				return;
				
			}
		}
		
		InputStream  in=new ByteArrayInputStream(msg);
		StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL+AsduHeader.H_LEN+1);
          
	
		//1  充电机编号
		String epCode = StreamUtil.readBCDWithLength(in, 8);
		
		int epGunNo=(int) StreamUtil.read(in);
		
		Map<Integer, SingleInfo> pointMapOneYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapTwoYx = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapYc = new ConcurrentHashMap<Integer,SingleInfo>();
		Map<Integer, SingleInfo> pointMapVarYc = new ConcurrentHashMap<Integer,SingleInfo>();
		
		
		//2  充电机输出电压//11：M_ME_NB_1  BIN 码  2Byte
		int nVol=(int) StreamUtil.readUB2(in);
	
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_VOL, nVol, "", 0);
		
		//3  充电机输出电流
		//11：M_ME_NB_1  BIN 码  2Byte
		//精确到小数点后二位
		int nCurrent=(int) StreamUtil.readUB2(in);
		
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_OUT_CURRENT, nCurrent, "", 0);
		
		//4  SOC
		//11：M_ME_NB_1  BIN 码  2Byte
		//整型
		int nSoc=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_SOC, nSoc, "", 0);
		
		
		//5  电池组最低温度
		//11：M_ME_NB_1  BIN 码  2Byte
		//精确到小数点后一位
		int value=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_BATTRY_LOWEST_TEMP, value, "", 0);
		
		//6  电池组最高温度
		//11：M_ME_NB_1  BIN 码  2Byte
		value=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_BATTRY_HIGHEST_TEMP, value, "", 0);
		
		
		//7  累计充电时间
		//11：M_ME_NB_1  BIN 码  2Byte
		//单位：min
		value=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_TOTAL_TIME, value, "", 0);
		
		/*8  充电机状态
		11：M_ME_NB_1  压缩 BCD 码  2Byte
		变化上传，0001- 告警 0002-待机 0003- 工作  0004- 离线
		0005-完成*/
		value=(int) StreamUtil.read(in);
		
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_WORKSTATUS, value, "", 0);
		
		
		/*9  BMS 通信异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_BMS_ERROR, value, "", 0);
		
		/*10  直流母线输出过压告警
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传；0：不过
		压，1 过压*/
		value = (int) StreamUtil.read(in);
		
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_IN_VOL_WARN, value, "", 0);
		
		/*11  直流母线输出欠压告警
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传；0：不欠
		压，1 欠压*/
		
		value = (int) StreamUtil.read(in);
		if(value==1)
			value=2;
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_AC_IN_VOL_WARN, value, "", 0);
		/*12  蓄电池充电过流告警
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传；0：不过
		流，1 过流*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_BATTRY_CHARGE_OVER_CURRENT, value, "", 0);
		
		/*13  蓄电池模块采样点过温告警
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传；0：不过
		温，1 过温*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapTwoYx, EpProtocolConstant.YX_2_BATTRY_SAMPLE_OVER_TEMP, value, "", 0);
		
		
		/*14  有功总电度
		132：M_MD_NA_1  BIN 码  4Byte
		精确到小数点后一位*/
		value=(int) StreamUtil.readInt(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_VAR_ACTIVE_TOTAL_METERNUM, value, "", 0);
		
		
		/*15  是否连接电池
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_LINKED_CAR, value, "", 0);
		//chargeInfo.setConnect_battry(value);
		
		/*16  单体电池最高电压
		11：M_ME_NB_1  BIN 码  2Byte
		精确到小数点后三位*/
		value=(int) StreamUtil.readUB2(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_SIGNLE_BATTRY_HIGH_VOL_GROUP, value, "", 0);
		
		
		/*17  单体电池最低电压
		11：M_ME_NB_1  BIN 码  2Byte
		精确到小数点后三位*/
		value=(int) StreamUtil.readUB2(in);
		//RealChargeInfo.AddPoint(pointMap, EpProtocolConstant.YC_S, value, "", 0);
		
		/*18枪座状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_SIT, value, "", 0);
		/*19充电枪盖状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_GUN_LID, value, "", 0);
		
		/*20车与桩建立通信信号
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_COMM_WITH_CAR, value, "", 0);
		
		/*21车位占用状态
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CAR_PLACE, value, "", 0);
		/*22交易记录已满告警
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_TRANSRECORD_FULL_WARN, value, "", 0);
		/*23读卡器通讯异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_CARD_READER_FAULT, value, "", 0);
		/*24电度表异常
		1：M_SP_NA_1  BIN 码  1Byte
		布尔型,  变化上传； 0 正常， 1
		异常*/
		value = (int) StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapOneYx, EpProtocolConstant.YX_1_METER_ERROR, value, "", 0);
		
	
		//25已充金额 BIN 码 4Byte 
		int chargeCost= StreamUtil.readInt(in);	
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_COST, chargeCost, "", 0);
		
		//26电价BIN 码 4Byte 
		int chargePrice= StreamUtil.readInt(in)*10;
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_PRICE, chargePrice, "", 0);
		
		//27已充总度数 BIN 码 4Byte
		int chargedMeterNum= StreamUtil.readInt(in);
		RealChargeInfo.AddPoint(pointMapVarYc, EpProtocolConstant.YC_VAR_CHARGED_METER_NUM, chargedMeterNum, "", 0);
		
		//28 车位地锁状态 BIN 码 1Byte
		int carPlaceLock= (int)StreamUtil.read(in);
		RealChargeInfo.AddPoint(pointMapYc, EpProtocolConstant.YC_CAR_PLACE_LOCK, carPlaceLock, "", 0);
		
			
		EpGunCache gunCache = EpGunService.getEpGunCache(epCode, epGunNo);	
		if(gunCache == null)
		{
			logger.error("handleWholeDcRealInfo2,receive realData,epcode{},gunno{} gunCache=NULL",epCode,epGunNo);
			return;
		}
		logger.debug("handleWholeDcRealInfo2,receive realData,epCode:{}, epGunNo:{}",epCode, epGunNo);
		
		gunCache.onRealDataChange(pointMapYc,11);
		gunCache.onRealDataChange(pointMapOneYx,1);
		gunCache.onRealDataChange(pointMapTwoYx,3);
		gunCache.onRealDataChange(pointMapVarYc,132);
		
	}
	@SuppressWarnings("unchecked")
	public static void handleOneBitYx(EpCommClient epCommClient, byte[] msg ) throws IOException
	{
		
		if (null == epCommClient)
		{
			logger.error("receive realData dataType:1=oneBitYx,fail--did not find EpCommClient");
		}
		if(epCommClient.getStatus() !=2) {
			logger.error("receive realData dataType:1=oneBitYx,fail--is not init,CommStatus:{}",epCommClient.getStatus());
			// 没有发协议侦的客户端都关闭
			epCommClient.close();
			EpCommClientService.removeEpCommClient(epCommClient);
			
			return;
		}
	
		InputStream  in=new ByteArrayInputStream(msg);
		byte[] NRs =StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL);
		
		
		byte[] asduBytes= StreamUtil.readWithLength(in,6);
		AsduHeader asduHeader = new AsduHeader(asduBytes);
		
		if(epCommClient.getVersion()< EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
		    byte[] time = StreamUtil.readWithLength(in,7);
		}
		
		int nLimit = asduHeader.getLimit()&0xff;
		int vsq = nLimit>> 7;
		int vCount = nLimit - (vsq<<7);
	
		Vector singleInfos = new Vector(vCount);
			
		String epCode = epCommClient.getIdentity();
		int gunno = 0;
		int commVersion =  epCommClient.getVersion();
		if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
			 epCode = StreamUtil.readBCDWithLength(in, 8);
			 gunno = StreamUtil.read(in);
		}
		
		logger.debug("receive realData dataType:1=oneBitYx,epCode:{},epGunNo:{},Identity:{},vCount:{}",
				new Object[]{epCode,gunno,epCommClient.getIdentity(),vCount});

		if(vsq == 0) //后面地址不是连续的
		{
			for(int i=0;i< vCount ;i++)
			{
				byte[] infoAddress=StreamUtil.readWithLength(in,3);
				int address = WmIce104Util.bytes2int(infoAddress);
				int value = (int)StreamUtil.read(in);
				logger.debug("receive realData dataType:1=oneBitYx,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==0",
						new Object[]{epCode,gunno,epCommClient.getIdentity(),address,value});
				SingleInfo loopSingleInfo= new SingleInfo();
				loopSingleInfo.setAddress(address);
				loopSingleInfo.setIntValue(value);
				singleInfos.add(loopSingleInfo);
			}
		}
		else
		{
			byte[] infoAddress=StreamUtil.readWithLength(in,3);
			int address = WmIce104Util.bytes2int(infoAddress);
			for(int i=0;i< vCount ;i++)
			{
				int value = (int)StreamUtil.read(in);
				SingleInfo loopSingleInfo= new SingleInfo();
				loopSingleInfo.setAddress(address+i);
				logger.debug("receive realData dataType:1=oneBitYx,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==1",
						new Object[]{epCode,gunno,epCommClient.getIdentity(),address+i,value});
				loopSingleInfo.setIntValue(value);
				singleInfos.add(loopSingleInfo);
			}
			
		}
		if(epCommClient.getMode() ==2)//集中器
		{
			if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
			    EpConcentratorService.handleOneBitYxInfo_v4(epCode,gunno,epCommClient.getIdentity(), singleInfos);
			}
			else
				EpConcentratorService.handleOneBitYxInfo(epCommClient.getIdentity(), singleInfos);
		}
		else
		{
		    
		    if(epCommClient.getVersion()>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
		    	 EpService.handleOneBitYxInfo_v4(epCode,gunno,singleInfos);
			}
		    else
			    EpService.handleOneBitYxInfo(epCode, singleInfos);
			
		}
		
	}
	@SuppressWarnings("unchecked")
	public static void handleTwoBitYx(EpCommClient epCommClient, byte[] msg) throws IOException
	{
		
		if (null == epCommClient)
		{
			logger.error("receive realData dataType:2=twoBitYx,fail--did not find EpCommClient");
		}
		if(epCommClient.getStatus() !=2) {
			logger.error("receive realData dataType:2=twoBitYx,Identity:{},fail--is not init,commStatus:{}",epCommClient.getIdentity(),epCommClient.getStatus());
			// 没有发协议侦的客户端都关闭
			
			return;
		}
		
		InputStream  in=new ByteArrayInputStream(msg);
		byte[] NRs =StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL);
		
		
		byte[] asduBytes= StreamUtil.readWithLength(in,6);
		AsduHeader asduHeader = new AsduHeader(asduBytes);
		
		int commVersion = epCommClient.getVersion();
		if(commVersion< EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
		    byte[] time = StreamUtil.readWithLength(in,7);
		}
		
		int nLimit = asduHeader.getLimit()&0xff;
		int vsq = nLimit>> 7;
		int vCount = nLimit - (vsq<<7);
	
		Vector singleInfos = new Vector(vCount);
		
		
		String epCode = epCommClient.getIdentity();
		int gunno = 0;
		if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
			 epCode = StreamUtil.readBCDWithLength(in, 8);
			 gunno = StreamUtil.read(in);
		}
		
		logger.debug("receive realData dataType:2=twoBitYx,epCode:{},epGunNo:{},Identity:{},vCount:{}",
				new Object[]{epCode,gunno,epCommClient.getIdentity(),vCount});
		
		if(vsq == 0) //后面地址不是连续的
		{
			for(int i=0;i< vCount ;i++)
			{
				byte[] infoAddress=StreamUtil.readWithLength(in,3);
				int address = WmIce104Util.bytes2int(infoAddress);
				int value = (int)StreamUtil.read(in);

				logger.debug("receive realData dataType:2=twoBitYx,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==0",
						new Object[]{epCode,gunno,epCommClient.getIdentity(),address,value});
				
				SingleInfo loopSingleInfo= new SingleInfo();
				loopSingleInfo.setAddress(address);
				loopSingleInfo.setIntValue(value);
				singleInfos.add(loopSingleInfo);
			}
		}
		else
		{
			byte[] infoAddress=StreamUtil.readWithLength(in,3);
			int address = WmIce104Util.bytes2int(infoAddress);
			for(int i=0;i< vCount ;i++)
			{
				int value = (int)StreamUtil.read(in);
				SingleInfo loopSingleInfo= new SingleInfo();
				loopSingleInfo.setAddress(address+i);
				
				logger.debug("receive realData dataType:2=twoBitYx,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==1",
						new Object[]{epCode,gunno,epCommClient.getIdentity(),address+i,value});
				
				loopSingleInfo.setIntValue(value);
				singleInfos.add(loopSingleInfo);
			}
			
		}
		if(epCommClient.getMode() ==2)//集中器
		{
			if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
			    EpConcentratorService.handleTwoBitYxInfo_v4(epCode,gunno,epCommClient.getIdentity(), singleInfos);
			}
			else
				EpConcentratorService.handleTwoBitYxInfo(epCommClient.getIdentity(), singleInfos);
		}
		else
		{
		    epCode = epCommClient.getIdentity();
		    if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
		    	 EpService.handleTwoBitYxInfo_v4(epCode,gunno,singleInfos);
			}
		    else
			   EpService.handleTwoBitYxInfo(epCode, singleInfos);
			
		}
		
	}
	public static void handleYc(EpCommClient epCommClient, byte[] msg) throws IOException
	{
		
		if (null == epCommClient)
		{
			logger.error("receive realData dataType:3=yc,fail--did not find EpCommClient");
		}
		if(epCommClient.getStatus() !=2) {
			logger.error("receive realData dataType:3=yc,Identity:{},fail--is not init,commStatus:{}",epCommClient.getIdentity(),epCommClient.getStatus());
			// 没有发协议侦的客户端都关闭
			
			return;
		}
		
		InputStream  in=new ByteArrayInputStream(msg);
		StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL);
		byte[] asduBytes= StreamUtil.readWithLength(in,6);
		AsduHeader asduHeader = new AsduHeader(asduBytes);
		int commVersion = epCommClient.getVersion();
		if(commVersion< EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
		   byte[] time = StreamUtil.readWithLength(in,7);
		}
		
		int nLimit = asduHeader.getLimit() &0xff;
		int vsq = nLimit>> 7;
		int vCount = nLimit - (vsq<<7);
		
		Vector singleInfos = new Vector(vCount);
		
		String epCode = epCommClient.getIdentity();
		int gunno = 0;
		if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
			 epCode = StreamUtil.readBCDWithLength(in, 8);
			 gunno = StreamUtil.read(in);
		}
		
		logger.debug("receive realData dataType:3=yc,epCode:{},epGunNo:{},Identity:{},vCount:{}",
				new Object[]{epCode,gunno,epCommClient.getIdentity(),vCount});
		
		if(vsq == 0) //后面地址不是连续的
		{
			for(int i=0;i< vCount ;i++)
			{
				byte[] infoAddress=StreamUtil.readWithLength(in,3);
				int value = (int) StreamUtil.readUB2(in);
				byte qdsDesc = StreamUtil.read(in);
				
				int address = WmIce104Util.bytes2int(infoAddress);
				
				logger.debug("receive realData dataType:3=yc,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==0",
						new Object[]{epCode,gunno,epCommClient.getIdentity(),address,value});
				
				SingleInfo loopSingleInfo= new SingleInfo();
				loopSingleInfo.setAddress(address);
				loopSingleInfo.setIntValue(value);
				loopSingleInfo.setQdsDesc(qdsDesc);
				singleInfos.add(loopSingleInfo);
			}
		}
		else
		{
			
			byte[] infoAddress=StreamUtil.readWithLength(in,3);
			int address = WmIce104Util.bytes2int(infoAddress);
			for(int i=0;i< vCount ;i++)
			{
				SingleInfo loopSingleInfo= new SingleInfo();
				
				loopSingleInfo.setAddress(address+i);
				
				int value = StreamUtil.readUB2(in);
				logger.debug("receive realData dataType:3=yc,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==1",
						new Object[]{epCode,gunno,epCommClient.getIdentity(),address+i,value});
				
				loopSingleInfo.setIntValue(value);
				byte qdsDesc = StreamUtil.read(in);
				loopSingleInfo.setQdsDesc(qdsDesc);
				
				singleInfos.add(loopSingleInfo);
			}
			
		}
		if(epCommClient.getMode() ==2)//集中器
		{
			if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
			    EpConcentratorService.handleYcInfo_v4(epCode,gunno,epCommClient.getIdentity(), singleInfos);
			}
			else
				EpConcentratorService.handleYcInfo(epCommClient.getIdentity(), singleInfos);
		}
		else
		{
		    epCode = epCommClient.getIdentity();
		    if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
		    	 EpService.handleYcInfo_v4(epCode,gunno,singleInfos);
			}
		    else
			     EpService.handleYcInfo(epCode, singleInfos);
			
		}
		
		
	}
	@SuppressWarnings("unchecked")
	public static void handleVarYc(EpCommClient epCommClient, byte[] msg)
	{
		
		if (null == epCommClient)
		{
			logger.error("receive realData dataType:4=varYc,fail--did not find EpCommClient");
		}
		if(epCommClient.getStatus() !=2) {
			logger.error("receive realData dataType:4=varYc,Identity:{},fail--is not init,commStatus:{}",epCommClient.getIdentity(),epCommClient.getStatus());
			// 没有发协议侦的客户端都关闭
			
			return;
		}
	
		try{
		InputStream  in=new ByteArrayInputStream(msg);
		StreamUtil.readWithLength(in,ApciHeader.NUM_CTRL);
		byte[] asduBytes= StreamUtil.readWithLength(in,6);
		AsduHeader asduHeader = new AsduHeader(asduBytes);
		int commVersion= epCommClient.getVersion();
		if(commVersion< EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
		    byte[] time = StreamUtil.readWithLength(in,7);
		}
		
		int nLimit = asduHeader.getLimit()&0xff;
		int vsq = nLimit>> 7;
		int vCount = nLimit - (vsq<<7);
		
		String epCode = epCommClient.getIdentity();
		int gunno = 0;
		if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
		{
			 epCode = StreamUtil.readBCDWithLength(in, 8);
			 gunno = StreamUtil.read(in);
		}
		
		logger.debug("receive realData dataType:4=varYc,epCode:{},epGunNo:{},Identity:{},vCount:{}",
				new Object[]{epCode,gunno,epCommClient.getIdentity(),vCount});
		
		Vector singleInfos = new Vector(vCount);
		
		if(vsq == 0) //后面地址不是连续的
		{
			for(int i=0;i< vCount ;i++)
			{
				byte[] infoAddress=StreamUtil.readWithLength(in,3);

				int address = WmIce104Util.bytes2int(infoAddress);
				
				int infoLen = (int)StreamUtil.read(in)&0xff;
			
				SingleInfo loopSingleInfo= new SingleInfo();
				if(infoLen ==4)
				{
					int value = StreamUtil.readInt(in);
					loopSingleInfo.setIntValue(value);
					logger.debug("receive realData dataType:4=varYc,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==0",
							new Object[]{epCode,gunno,epCommClient.getIdentity(),address,value});
				}
				else
				{
					byte [] val =StreamUtil.readWithLength(in,infoLen);
					byte [] val1=WmIce104Util.removeFFAndO(val);
					String strValue ="";
					if(val1!=null)
						strValue = StringUtil.getByteString(val1);
				
					loopSingleInfo.setStrValue(strValue);
					logger.debug("handleVarYc,receive realData dataType:4=varYc,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==0",
							new Object[]{epCode,gunno,epCommClient.getIdentity(),address,strValue});
				}
				byte qdsDesc = StreamUtil.read(in);			
				loopSingleInfo.setAddress(address);		
				loopSingleInfo.setQdsDesc(qdsDesc);
				singleInfos.add(loopSingleInfo);
		
			}
		}
		else
		{
			byte[] infoAddress=StreamUtil.readWithLength(in,3);
			int address = WmIce104Util.bytes2int(infoAddress);
			
			for(int i=0;i< vCount ;i++)
			{
				int infoLen = (int)StreamUtil.read(in)&0xff;		
				SingleInfo loopSingleInfo= new SingleInfo();
				if(infoLen ==4)
				{
					int value = StreamUtil.readInt(in);
					loopSingleInfo.setIntValue(value);

					logger.debug("receive realData dataType:4=varYc,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==1",
							new Object[]{epCode,gunno,epCommClient.getIdentity(),address+i,value});
				}
				else
				{
					byte [] val =StreamUtil.readWithLength(in,infoLen);

					byte [] val1=WmIce104Util.removeFFAndO(val);
					String strValue ="";
					if(val1!=null)
						strValue = StringUtil.getByteString(val1);
					
					
					loopSingleInfo.setStrValue(strValue);
					logger.debug("receive realData dataType:4=varYc,epCode:{},epGunNo:{},Identity:{},address:{},value:{},vsq==1",
							new Object[]{epCode,gunno,epCommClient.getIdentity(),address+i,strValue});
				}
				byte qdsDesc = StreamUtil.read(in);
			
				loopSingleInfo.setAddress(address+i);
				
				loopSingleInfo.setQdsDesc(qdsDesc);
				singleInfos.add(loopSingleInfo);
			}
		}
		
		if(epCommClient.getMode() ==2)//集中器
		{
			if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
			    EpConcentratorService.handleVarYcInfo_v4(epCode,gunno,epCommClient.getIdentity(), singleInfos);
			}
			else
				EpConcentratorService.handleVarYcInfo(epCommClient.getIdentity(), singleInfos);
		}
		else
		{
		    epCode = epCommClient.getIdentity();
		    if(commVersion>= EpProtocolConstant.PROTOCOL_VERSION_V4) 
			{
		    	 EpService.handleVarYcInfo_v4(epCode,gunno,singleInfos);
			}
		    else
			     EpService.handleVarYcInfo(epCode, singleInfos);
			
		  }
		}
		catch(IOException e)
		{
			logger.error("handleVarYc exception,e.StackTrace:{}",e.getStackTrace());
		}
	}
	
	
}

