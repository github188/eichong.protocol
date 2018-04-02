package com.epcentre.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.constant.EpProtocolConstant;




public class AnalyzeProtocol{
	
	private static final Logger logger = LoggerFactory
			.getLogger(AnalyzeProtocol.class);
	
	public static  byte[] Package(byte[] data,byte cos,short cmdtype) {
		
		AnalyzeHeader Header = new AnalyzeHeader();
		
		Header.setLength(3 + data.length);

		ByteArrayOutputStream bmsg = new ByteArrayOutputStream( AnalyzeConstant.ANALYZE_SENDBUFFER);
		
		try {
			bmsg.write(Header.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		bmsg.write(cos);
		
		byte cmdtypeL = (byte)(cmdtype&0x00ff);		
		bmsg.write(cmdtypeL);
		
		byte cmdtypeH = (byte)((cmdtype>>8)&0x00ff);
		bmsg.write(cmdtypeH);
		
		try {
			bmsg.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return bmsg.toByteArray();
	}

    public static  byte[] do_one_bit_yx(String epCode,int epGunNo,int currentType,Map<Integer, SingleInfo> pointMap) { 		
	
    	
    	ByteArrayOutputStream bout = new ByteArrayOutputStream(AnalyzeConstant.ANALYZE_SENDBUFFER);
		assert(epCode.length()==EpProtocolConstant.LEN_CODE);
		
		try {
		
		//1	终端机器编码//	BCD码	8Byte	16位编码 //
		bout.write(WmIce104Util.str2Bcd(epCode));
		//2	枪口	bin码	1Byte	
		bout.write(epGunNo);
		bout.write((byte)currentType);
		bout.write((byte)pointMap.size());
        Iterator iter = pointMap.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			SingleInfo info=(SingleInfo) entry.getValue();
			int pointAddr = ((Integer)entry.getKey()).intValue();
			
			bout.write(WmIce104Util.short2Bytes((short)pointAddr));
			bout.write((byte)info.getIntValue());
			
			logger.debug("do_one_bit_yx,address:{},value:{}",pointAddr,info.getIntValue());

		} 
		}catch (IOException e) {
			e.printStackTrace();
		}
		return Package(bout.toByteArray(),(byte)0,AnalyzeConstant.REAL_ONE_BIT_YX);
		
    }
    public static  byte[] do_two_bit_yx(String epCode,int epGunNo,int currentType,Map<Integer, SingleInfo> pointMap) { 		
	
    	
    	ByteArrayOutputStream bout = new ByteArrayOutputStream(AnalyzeConstant.ANALYZE_SENDBUFFER);
		assert(epCode.length()==EpProtocolConstant.LEN_CODE);
		
		try {
		
		//1	终端机器编码//	BCD码	8Byte	16位编码 //
		bout.write(WmIce104Util.str2Bcd(epCode));
		//2	枪口	bin码	1Byte	
		bout.write(epGunNo);
		bout.write((byte)currentType);
		bout.write((byte)pointMap.size());
		
        Iterator iter = pointMap.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			//写地址
			SingleInfo info=(SingleInfo) entry.getValue();
			int pointAddr = ((Integer)entry.getKey()).intValue();
			bout.write(WmIce104Util.short2Bytes((short)pointAddr));
			bout.write((byte)info.getIntValue());
			
			logger.debug("do_two_bit_yx,address:{},value:{}",pointAddr,info.getIntValue());

		} 
		}catch (IOException e) {
			e.printStackTrace();
		}
		return Package(bout.toByteArray(),(byte)0,AnalyzeConstant.REAL_TWO_BIT_YX);
		
    }
    public static  byte[] do_yc(String epCode,int epGunNo,int currentType,Map<Integer, SingleInfo> pointMap) { 		
	
    	
    	ByteArrayOutputStream bout = new ByteArrayOutputStream(AnalyzeConstant.ANALYZE_SENDBUFFER);
		assert(epCode.length()==EpProtocolConstant.LEN_CODE);
		
		try {
		
		//1	终端机器编码//	BCD码	8Byte	16位编码 //
		bout.write(WmIce104Util.str2Bcd(epCode));
		//2	枪口	bin码	1Byte	
		bout.write(epGunNo);
		bout.write((byte)currentType);
		bout.write((byte)pointMap.size());
		
        Iterator iter = pointMap.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			
			SingleInfo info=(SingleInfo) entry.getValue();
			int pointAddr = ((Integer)entry.getKey()).intValue();
			bout.write(WmIce104Util.short2Bytes((short)pointAddr));
			
			
			bout.write(WmIce104Util.short2Bytes((short)info.getIntValue()));
			
			logger.debug("do_yc,address:{},value:{}",pointAddr,info.getIntValue());

		} 
		}catch (IOException e) {
			e.printStackTrace();
		}
		return Package(bout.toByteArray(),(byte)0,AnalyzeConstant.REAL_YC);
		
    }
    public static  byte[] do_var_yc(String epCode,int epGunNo,int currentType,Map<Integer, SingleInfo> pointMap) { 		
	
    	
    	ByteArrayOutputStream bout = new ByteArrayOutputStream(AnalyzeConstant.ANALYZE_SENDBUFFER);
		assert(epCode.length()==EpProtocolConstant.LEN_CODE);
		
		try {
		
		//1	终端机器编码//	BCD码	8Byte	16位编码 //
			
		bout.write(WmIce104Util.str2Bcd(epCode));
		//2	枪口	bin码	1Byte	
		bout.write((byte)epGunNo);
		bout.write((byte)currentType);
		bout.write((byte)pointMap.size());
        Iterator iter = pointMap.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			
			SingleInfo info=(SingleInfo) entry.getValue();
			int pointAddr= info.getAddress();
			bout.write(WmIce104Util.short2Bytes((short)pointAddr));
			
			String strVaule = info.getStrValue();
			
			if(strVaule==null || strVaule.length()==0)
			{
				bout.write((byte)4);
				bout.write(WmIce104Util.int2Bytes(info.getIntValue()));
			}
			else
			{
				int strLen= info.getStrValue().length();
				
				bout.write((byte)strLen);
				bout.write(strVaule.getBytes());
			}
			
			logger.debug("do_var_yc,address:{},value:{}",pointAddr,info.getIntValue());

		} 
		}catch (IOException e) {
			e.printStackTrace();
		}
		return Package(bout.toByteArray(),(byte)0,AnalyzeConstant.REAL_VAR_YC);
		
    }
    
    
    //心跳
    public static  byte[] do_heart() {

        AnalyzeHeader Header = new AnalyzeHeader();
		
		Header.setLength( 3 );

		ByteArrayOutputStream bmsg = new ByteArrayOutputStream( AnalyzeConstant.ANALYZE_SENDBUFFER);
		
		try {
			bmsg.write(Header.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		bmsg.write(1);
		byte cmdtypeL = 01;		
		bmsg.write(cmdtypeL);
		
		byte cmdtypeH = 00;
		bmsg.write(cmdtypeH);

		return bmsg.toByteArray();
	}

}
		
