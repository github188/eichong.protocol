package com.webgate.test;


import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webgate.cache.BespCache;
import com.webgate.cache.ElectricPileCache;
import com.webgate.cache.EpGunCache;
import com.webgate.cache.RateInfoCache;
import com.webgate.cache.UserCache;
import com.webgate.config.Global;
import com.webgate.protocol.UtilProtocol;
import com.webgate.protocol.WmIce104Util;
import com.webgate.service.CacheService;
import com.webgate.service.EpBespokeService;
import com.webgate.service.EpChargeService;
import com.webgate.service.EpService;
import com.webgate.service.PhoneService;
import com.webgate.service.RateService;
import com.webgate.service.UserService;
import com.webgate.utils.FileUtils;
import com.webgate.utils.StringUtil;

public class ImitateConsumeService {
	
	private static final Logger logger = LoggerFactory
			.getLogger(ImitateConsumeService.class);
	
	public static Map<String,String> userKeyMap = new ConcurrentHashMap<String, String>();

	
	public static String getConnetUserKey(String userName)
	{
		String userKey=null;
		userKey = userKeyMap.get(userName);
		
		return userKey;
	}
	public static void putConnetUserKey(String userName,String userKey)
	{
		userKeyMap.put(userName, userKey);
	}
	static public int checkSign(Map<String, List<String>> params)
	{
			int errorCode=0; 
			Object oUser = params.get("user");
			Object oSign = params.get("sign");
			if(oUser == null || oSign == null)
			{
				errorCode=1;
				return errorCode;
			}
			
			String src="";
			String sign="";
			String userName="";
			
			Collection<String> keyset= params.keySet();
			List<String> list = new ArrayList<String>(keyset);  
			   
			//对key键值按字典升序排序  
			Collections.sort(list);  
			            
		    for (int i = 0; i < list.size(); i++) {
		    	 String key = list.get(i);
		    	 if(key.compareTo("sign")==0)
		    	 {
		    		 sign = (String)params.get(key).get(0);
		    	 }
		    	 else
		    	 {
		    		 if(key.compareTo("user")==0)
		    		 {
		    			 userName= (String) params.get(key).get(0);
		    		 }
		    		 if(src.length()!=0)
		    			 src =src +"&";
		    		 src +=list.get(i)+"="+params.get(key).get(0);
		    	 } 
		     }
		    
		    String userKey = getConnetUserKey(userName);
		    if(userKey ==null)
		    {
		    	return 2;
		    }
		    src += userKey ;
		   
		    String calcSign = WmIce104Util.MD5Encode(src.getBytes());
		    if(calcSign.compareTo(sign)!=0)
		    {
		    	return 3;
		    }
		    	
		    
		    return 0;
	}
	
	static public String testStartBespoke(Map<String, List<String>> params)
	{
		boolean paramValid=false;
    	if (params.isEmpty())
        {
        	return "paramValid is invalid";
        }
        String epCode =null;
        short time =0;
        int epGunNo=0;
        String phone = null;
        int redo = 0;
        String ip =null;
        String pokeCode=null;
        int payMode=1;
        
        List<String> vals = params.get("code");
        List<String> vals1 = params.get("phone");
        List<String> vals2 = params.get("gunno");
        List<String> vals3 = params.get("time");
        List<String> vals4 = params.get("redo");
        List<String> vals5 = params.get("bespno");
        List<String> vals6 = params.get("payMode");
        if(vals!=null && vals.size()>=1 && vals1!=null && vals1.size()>=1
        		&&vals2!=null && vals2.size()>=1&&vals3!=null && vals3.size()>=1
        		&&vals4!=null && vals4.size()>=1
        		&&vals6!=null && vals6.size()>=1)
        {
        	int error = checkSign(params);
        	if(error>0)
				return "checkSign fail";
			
        	epCode =  vals.get(0);
        	time = (short)(Integer.parseInt(vals3.get(0)));
        	epGunNo = Integer.parseInt(vals2.get(0));
        	phone = vals1.get(0);
        	redo = Integer.parseInt(vals4.get(0));
        	
        	pokeCode= vals5.get(0);
        	payMode = Integer.parseInt(vals6.get(0));
        	
        	
        	paramValid = true;
        }
        if(!paramValid)
        	 return "paramValid is invalid";
       
        /*if(epCode.length() != EpProtocolConstant.LEN_CODE )
        {
        	return "paramValid is invalid";
        }*/
       
    	
        EpGunCache epGunCache= CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache==null)
        {
        	logger.info("testStartBespoke not find EpGunCache!epCode:{},epGunNo:{}",epCode, epGunNo);
        	return "not find EpGunCache";
        	
        }
        BespCache bespCacheObj =epGunCache.getBespCache();
    	if(bespCacheObj != null)
    	{
    		pokeCode = bespCacheObj.getBespNo();
    	}
        
    	 java.util.Date dt = new Date();
    	 if(pokeCode.compareTo("0")==0)
         {
     		pokeCode = String.valueOf(dt.getTime() / 1000);
        
 	         pokeCode = pokeCode.substring(2, pokeCode.length());
 	         pokeCode += (int) (Math.random() * 9000 + 1000);
         }
	    long bespSt = dt.getTime() / 1000;
	    
	    
	    int epid = epGunCache.getPkEpId();
	    int gunid= epGunCache.getPkEpGunId();
	    int useid=0;
		
			
		UserCache u= UserService.getUserCache(phone);
		if(null != u)
		{
			useid = u.getId();
		}
		else
		{
			return "无效用户";
		}
		
	    int errorCode = EpBespokeService.apiBespoke(epCode,
		           1, epid, pokeCode, time, bespSt, redo,
		           useid, phone, (long) gunid, payMode,1000,0,"");
	    
	    if(errorCode>0)
		{
			return "errorCode:"+ errorCode;
		}
		return "send bespoke cmd success";
	}
	
	static public String testStartBespoke2(Map<String, List<String>> params)
	{
		boolean paramValid=false;
    	if (params.isEmpty())
        {
        	return "paramValid is invalid";
        }
        String epCode =null;
        short time =0;
        int epGunNo=0;
        String phone = null;
        int redo = 0;
        String ip =null;
        String bespNo =null;
       
        
        List<String> vals = params.get("code");
        List<String> vals1 = params.get("phone");
        List<String> vals2 = params.get("gunno");
        List<String> vals3 = params.get("time");
        List<String> vals4 = params.get("redo");
        List<String> vals5 = params.get("bespno");
        if(vals!=null && vals.size()>=1 && vals1!=null && vals1.size()>=1
        		&&vals2!=null && vals2.size()>=1&&vals3!=null && vals3.size()>=1
        		&&vals4!=null && vals4.size()>=1
        		&&vals5!=null && vals5.size()>=1
        		)
        {
        	int error = checkSign(params);
        	if(error>0)
				return "checkSign fail";
			
        	epCode =  vals.get(0);
        	time = (short)(Integer.parseInt(vals3.get(0)));
        	epGunNo = Integer.parseInt(vals2.get(0));
        	phone = vals1.get(0);
        	redo = Integer.parseInt(vals4.get(0));
        	
        	bespNo= vals5.get(0);
        	if(redo==1 && (bespNo.compareTo("0")==0))
        		return "预约编号空";
        	
        	
        	
        	paramValid = true;
        }
        if(!paramValid)
        	 return "paramValid is invalid";
       
       /* if(epCode.length() != EpProtocolConstant.LEN_CODE )
        {
        	return "paramValid is invalid";
        }*/
        ElectricPileCache epClient= CacheService.getEpCache(epCode);
		if(	epClient ==null )
		{
			return "not find epClient";
		}
		
		return "send bespoke cmd success!";
	}
	
	
	
	static public String testStopBespoke(Map<String, List<String>> params)
	{
	
		boolean paramValid=false;
		if (params.isEmpty())
	    {
	    	return "params.isEmpty()";
	    }
	    List<String> vals = params.get("code");
	    List<String> vals1 = params.get("gunno");
	   
	    String epCode = null;
	    int epGunNo =0;
	   
	    if(vals!=null && vals.size()>=1 && vals1!=null && vals1.size()>=1)
	    {
	    	epCode =  vals.get(0);
	    	epGunNo =  Integer.parseInt(vals1.get(0));
	    	paramValid=true;
	    }
	    if(!paramValid)
	    {
	    	 return "paramValid is invalid";
	    	
	    }
	    /*if(epCode.length() != EpProtocolConstant.LEN_CODE )
	    {
	    	return "paramValid is invalid";
	    }*/
	    int error = checkSign(params);
	    if(error>0)
			return "checkSign fail";
		
		EpGunCache epGunCache= CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache==null)
        {
        	logger.info("testStopBespoke not find EpGunCache!epCode:{},epGunNo:{}",epCode, epGunNo);
        	return "not find EpGunCache";
        	
        }
		
    	BespCache bespCacheObj =epGunCache.getBespCache();
    	int errorCode = 0;
    	if(bespCacheObj != null)
    	{
    		errorCode = EpBespokeService.apiStopBespoke(bespCacheObj.getBespId(), bespCacheObj.getBespNo(), epCode,1,99,"");
    	}
    	else
    		return "bespCacheObj is null";
    	
    	if(errorCode>0)
    	{
    		return "errorCode:"+ errorCode;
    	}
    	 logger.info("testStopBespoke,bespno:{}",bespCacheObj.getBespNo());
    	 
    	return "send stopBespoke cmd success!";
	   
	}
	
	static public String testStopBespoke2( Map<String, List<String>> params)
	{
	
		boolean paramValid=false;
		if (params.isEmpty())
	    {
	    	return "params.isEmpty()";
	    }
	    List<String> vals = params.get("code");
	    List<String> vals1 = params.get("gunno");
	    List<String> vals2 = params.get("bespno");
	   
	    String epCode = null;
	    int epGunNo =0;
	    
	    String bespNo = null;
	   
	    if(vals!=null && vals.size()>=1 && vals1!=null && vals1.size()>=1&& vals2!=null&& vals2.size()>=1)
	    {
	    	epCode =  vals.get(0);
	    	epGunNo =  Integer.parseInt(vals1.get(0));
	    	bespNo =  vals2.get(0);
	    	
	    	paramValid=true;
	    }
	    if(!paramValid)
	    {
	    	 return "paramValid is invalid";
	    	
	    }
	    /*if(epCode.length() != EpProtocolConstant.LEN_CODE )
	    {
	    	return "paramValid is invalid";
	    }*/
	    int error = checkSign(params);
	    if(error>0)
			return "checkSign fail";
	    
	    
	    ElectricPileCache epClient= CacheService.getEpCache(epCode);
		if(	epClient ==null )
		{
			return "not find epClient";
		}
		

        return "send stopbespoke cmd success!";
	   
	}

	static public String testCallEp(Map<String, List<String>> params)
	{
	
		boolean paramValid=false;
		if (params.isEmpty())
	    {
	    	return "params.isEmpty()";
	    }
	    List<String> vals = params.get("code");
	   
	    List<String> vals1 = params.get("type");
	    List<String> vals2 = params.get("time");
	    List<String> vals3 = params.get("account");
	   
	    String epCode = null;
	    int type =0;
	    int time =0;
	    String account="";
	   
	    if(vals!=null && vals.size()>=1 
	    		&& vals1!=null && vals1.size()>=1
	    		&& vals2!=null && vals2.size()>=1
	    		&& vals3!=null && vals3.size()>=1)
	    {
	    	epCode =  vals.get(0);
	    	type =  Integer.parseInt(vals1.get(0));
	    	time =  Integer.parseInt(vals2.get(0));
	    	account =  vals3.get(0);
	    	paramValid=true;
	    }
	    if(!paramValid)
	    {
	    	 return "paramValid is invalid1";
	    	
	    }
	   /* if(epCode.length() != EpProtocolConstant.LEN_CODE )
	    {
	    	return "paramValid is invalid,epCode.length() != EpProtocolConstant.LEN_CODE";
	    }*/
	    int error = checkSign(params);
	    if(error>0)
			return "checkSign fail";
		
	    UserCache u = UserService.getUserCache( account );
		
		if(u == null)
			return "无效用户";
		
	 
		int errorCode =EpService.doNearCallEpAction(epCode,type,time,u.getId(),(float)0.0,(float)0.0);
		 return "errorCode:"+ errorCode;
	}
	
	static public String testStartCharge( Map<String, List<String>> params)
	{
		boolean paramValid=false;
    	int epGunNo=-1;
    	String epCode=null;
    	String account = null;
    	int frozenAmt= 12;
    	int chargetype=1;
    	int payMode=1;
        if (params.isEmpty())
        	return "params.isEmpty()";
       
    	List<String> vals = params.get("code");
    	List<String> vals2 = params.get("gunno");
    	List<String> vals3 = params.get("account");
    	List<String> vals4 = params.get("chargetype");
    	List<String> vals5 = params.get("froneamt");
    	List<String> vals6 = params.get("payMode");
    	
    	if(vals!=null && vals.size()>=1
    			&&vals2!=null && vals2.size()>=1
    			&&vals3!=null && vals3.size()>=1
    			&&vals4!=null && vals4.size()>=1
    			&&vals5!=null && vals5.size()>=1
    			&&vals6!=null && vals6.size()>=1)
    	{
    		epCode = vals.get(0);	
    		epGunNo = Integer.parseInt(vals2.get(0));
    		account = vals3.get(0);
    		chargetype = Integer.parseInt(vals4.get(0));
    		
    		double dFronzeAmt =  Double.parseDouble(vals5.get(0));
    		
    		BigDecimal bdFronzeAmt = new BigDecimal(dFronzeAmt);
    		bdFronzeAmt = bdFronzeAmt.multiply(Global.DecTime2);
    		frozenAmt = UtilProtocol.BigDecimal2ToInt(bdFronzeAmt);
    	
    		payMode =Integer.parseInt(vals6.get(0));
    		paramValid = true;
    	}
        
       if(paramValid && epGunNo>=0 && epCode!=null   && account!=null)
       {
    	   int error = checkSign(params);
    	   if(error>0)
				return "checkSign fail";
       		
       		int errorCode = EpChargeService.apiStartElectric(epCode, epGunNo, 
       				account, StringUtil.repeat("0", 12), (short)chargetype,0,  frozenAmt,payMode,1000,1,"");
       		if(errorCode>0)
       		{
       			return "errorCode:"+ errorCode;
       		}
       		return "errorCode=0";
       }
       return "params invalid";
		
	}
	static public String testStartCharge2( Map<String, List<String>> params)
	{
		boolean paramValid=false;
    	int epGunNo=-1;
    	String epCode=null;
    	String account = null;
        if (params.isEmpty())
        	return "params.isEmpty()";
       
    	List<String> vals = params.get("code");
    	List<String> vals2 = params.get("gunno");
    	List<String> vals3 = params.get("account");
    	if(vals!=null && vals.size()>=1
    			&&vals2!=null && vals2.size()>=1
    			&&vals3!=null && vals3.size()>=1)
    	{
    		epCode = vals.get(0);	
    		epGunNo = Integer.parseInt(vals2.get(0));
    		account = vals3.get(0);
    		paramValid = true;
    	}
        
       if(paramValid && epGunNo>=0 && epCode!=null   && account!=null)
       {
    	   int error = checkSign(params);
    	   if(error>0)
				return "checkSign fail";
			
    	   UserCache u = UserService.getUserCache( account );
       		
       		if(u == null)
       			return "无效用户";
   		
   		
       	   ElectricPileCache epClient= CacheService.getEpCache(epCode);
 		  if(epClient ==null )
 		  {
 			return "not find epClient";
 		  }
 		 
       		
       		return "send startCharge cmd success!";
       }
       return "params invalid";
		
	}
	public static String findUser( Map<String, List<String>> params)
	{
		boolean paramValid = false;
    	
    	String account="";
    	
    	
        if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	
    	List<String> vals = params.get("account");

    	if(vals!=null && vals.size()>=1)
    	{
    		account = vals.get(0);
    		paramValid = true;
    	}
        
        if(!paramValid)
        	return "params invalid";
        int error = checkSign(params);
        if(error>0)
			return "checkSign fail";
      
    	UserCache u = UserService.getUserCache( account );
		if(u == null)
		{
			return "not find user" + account;
		}
		return ""+u;    
	}
	
	public static String testStopCharge( Map<String, List<String>> params)
	{
		boolean paramValid = false;
    	int epGunNo=-1;
    	String epCode=null;
    	String apiClientIp="10.9.2.107";
    	String account="";
    	
    	
        if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	List<String> vals = params.get("code");
    	List<String> vals2 = params.get("gunno");
    	List<String> vals3 = params.get("account");

    	if(vals!=null && vals.size()>=1
    			&&vals2!=null && vals2.size()>=1
    			&&vals3!=null && vals3.size()>=1)
    	{
    		epCode = vals.get(0);
    		epGunNo = Integer.parseInt(vals2.get(0));
    		account = vals3.get(0);
    		paramValid = true;
    	}
        
        if(!paramValid)
        	return "params invalid";
        
        int error = checkSign(params);
        if(error>0)
			return "checkSign fail";
        
    	UserCache u = UserService.getUserCache( account );
		
		if(u == null)
			return "无效用户";
		
		
		
		
    	int errorCode = EpChargeService.apiStopElectric(epCode, epGunNo,  u.getId(),99,apiClientIp);
    	if(errorCode>0)
		{
			return "errorCode:"+ errorCode;
		}
    	logger.info("testStopCharge account:{},code",account,epCode);
    	
		return "send stopCharge cmd success!";
        
	}
	
	public static String testStopCharge2( Map<String, List<String>> params)
	{
		boolean paramValid = false;
    	int epGunNo=-1;
    	String epCode=null;
    	String apiClientIp="10.9.2.107";
    	String account="";
    	
    	
        if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	List<String> vals = params.get("code");
    	List<String> vals2 = params.get("gunno");
    	List<String> vals3 = params.get("account");

    	if(vals!=null && vals.size()>=1
    			&&vals2!=null && vals2.size()>=1
    			&&vals3!=null && vals3.size()>=1)
    	{
    		epCode = vals.get(0);
    		epGunNo = Integer.parseInt(vals2.get(0));
    		account = vals3.get(0);
    		paramValid = true;
    	}
        
        if(!paramValid)
        	return "params invalid";
        
        int error = checkSign(params);
        if(error>0)
			return "checkSign fail";
        
    	UserCache u = UserService.getUserCache( account );
		if(u == null)
			return "无效用户";
		
		
		 ElectricPileCache epClient= CacheService.getEpCache(epCode);
		 if(	epClient ==null )
		 {
				return "not find epClient";
		 }
		 
        return "send stopCharge cmd success!";
        
	}
	
	

	
	
	
	
	
	
	public static String gun_restore( Map<String, List<String>> params)
	{
		boolean paramValid = false;
		ElectricPileCache epClient = null;
		int epGunNo = 0;
		String epCode = null;

		if (params.isEmpty()) {
			return "params.isEmpty()";
		}

		List<String> vals = params.get("code");
		List<String> valsGunNo = params.get("gunno");
		if (vals != null && vals.size() >= 1 && valsGunNo != null
				&& valsGunNo.size() >= 1) {
			epCode = vals.get(0);
			epGunNo = Integer.parseInt(valsGunNo.get(0));
			epClient = CacheService.getEpCache(epCode);
			if(epClient == null)
				return "epClient is null";
			int nGunNum = epClient.getGunNum();
			/*if (epCode != null
					&& epCode.length() == EpProtocolConstant.LEN_CODE
					&& epGunNo >= 1 && epGunNo <= nGunNum) {
				paramValid = true;
			}*/
		}
		if (!paramValid) {
			return "params invalid";
		}

		int error = checkSign(params);
		if(error>0)
			return "checkSign fail";
		
		if ( epClient != null) {
			int pkEpgunId = 0;// EpGunService.getPkEpGunId(epClient.getPkEpId(),
								// epCode, epGunNo);
			// EpGunService.updateEpGunState(pkEpgunId,
			// epCode,epGunNo,EpConstant.EP_GUN_STATUS_IDLE);
			// 更新内存
			EpGunCache epGunCache = CacheService.getEpGunCache(epCode, epGunNo);
			/*
			 * if(epGunCache !=null) {
			 * epGunCache.setStatus(EpConstant.EP_GUN_STATUS_IDLE); }
			 */
			logger.info("gun_restore epGunNo:{},epCode:{}",epGunNo,epCode);
			return ("gunstore fail,status:" + epGunCache.getStatus());
		} else {
			return ("gunstore gun no params is invalid\n");
		}
	}
	
	
	

	
	public static String getEpDetail( Map<String, List<String>> params)
    {
		boolean paramValid = false;
		String epCode = null;
		if (params.isEmpty()) {
			return "params.isEmpty()";
		}
		List<String> vals = params.get("code");
		if (vals != null && vals.size() >= 1)
		{
			epCode = vals.get(0);
			paramValid = true;

		}

		if (!paramValid) {
			return "params invalid";
		}
		int error = checkSign(params);
		if(error>0)
			return "checkSign fail";
		
		ElectricPileCache epClient= CacheService.getEpCache(epCode);
		if(	epClient !=null )
		{
			String epDetails = epClient.toString();
			RateInfoCache rateInfo= RateService.getRateById(epClient.getRateid());
			if(rateInfo ==null)
			     epDetails=epDetails+"rateInfo is null";
			else
				epDetails=epDetails+rateInfo.getRateInfo().toString();
			return (epDetails);
		} 
			
		return ("not find ep");
		

    }
	
	public static String getgundetail( Map<String, List<String>> params)
    {
		boolean paramValid = false;
		String epCode = null;
		int epGunNo = 0;
		if (params.isEmpty()) {
			return "params.isEmpty()";
		}
		List<String> vals = params.get("code");
		List<String> valsGunNo = params.get("gunno");
		if (vals != null && vals.size() >= 1 && valsGunNo != null
				&& valsGunNo.size() >= 1) {
			epCode = vals.get(0);
			epGunNo = Integer.parseInt(valsGunNo.get(0));

			paramValid = true;

		}

		if (!paramValid) {
			return "params invalid";
		}
		int error = checkSign(params);
		if(error>0)
			return "checkSign fail";
		
		EpGunCache epGunCache = CacheService.getEpGunCache(epCode, epGunNo);
		if (epGunCache != null) {
			String gunDetails = epGunCache.toString();
			return (gunDetails);
		} else {
			String value = MessageFormat.format("getgundetail! not find,epCode:{0}, epGunNo:{1}\n", epCode,
					epGunNo);
			return (value);
		}

	}
	public static String removeCharge( Map<String, List<String>> params)
    {
		boolean paramValid = false;
		if (params.isEmpty()) {
			return "params.isEmpty()";
		}
		String epCode = null;
		List<String> valsCode = params.get("code");
		List<String> valsGunNo = params.get("gunno");
		if (valsCode != null && valsCode.size() >= 1 && valsGunNo != null
				&& valsGunNo.size() >= 1) {
			
			int error = checkSign(params);
			if(error>0)
				return "checkSign fail";
			
			epCode = valsCode.get(0);
			int epGunNo = 0;
			String sEpGunNo = valsGunNo.get(0);
			epGunNo = Integer.parseInt(sEpGunNo);

			//String resp = EpChargeService.forceRemoveCharge(epCode, epGunNo);
			return "success";
		}

		return "params invalid";
	}
	public static String removeBespoke( Map<String, List<String>> params)
    {
		boolean paramValid = false;
		if (params.isEmpty()) {
			return "params.isEmpty()";
		}
		String epCode = null;
		List<String> valsCode = params.get("code");
		List<String> valsGunNo = params.get("gunno");
		if (valsCode != null && valsCode.size() >= 1 && valsGunNo != null
				&& valsGunNo.size() >= 1) {
			
			int error = checkSign(params);
			if(error>0)
				return "checkSign fail";
			
			epCode = valsCode.get(0);
			int epGunNo = 0;
			String sEpGunNo = valsGunNo.get(0);
			epGunNo = Integer.parseInt(sEpGunNo);

			//String resp = EpChargeService.forceRemoveBespoke(epCode, epGunNo);
			return "success";
		}

		return "params invalid";
	}
	
	
	
	
	
	
	public static String cleanUser( Map<String, List<String>> params)
    {
		boolean paramValid = false;
    	
    	String account="";
    	
    	
        if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	
    	List<String> vals = params.get("account");

    	if(vals!=null && vals.size()>=1)
    	{
    		account = vals.get(0);
    		paramValid = true;
    	}
        
        if(!paramValid)
        	return "params invalid";
        
        int error = checkSign(params);
       if(error>0)
			return "checkSign fail";
      
    	UserCache u = UserService.getUserCache( account );
		if(u == null)
		{
			return "not find user" + account;
		}
		u.clean();
		logger.info("cleanUser,account:{}",account);
		return ""+u;    
    }
	
	public static String stat( Map<String, List<String>> params)
    {
       
        if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
        
		int error = checkSign(params);
		if(error>0)
			return "checkSign fail";
		
    	return "";
    }
	
	
	public static String getRatebyId( Map<String, List<String>> params)
    {
		boolean paramValid = false;
    	if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	
		int rateid =0;
		
		List<String> vals = params.get("rateid");
		
		if(vals!=null && vals.size()>=1 )
		{
			int error = checkSign(params);
			if(error>0)
				return "checkSign fail";
			rateid=Integer.parseInt(vals.get(0));
			
			RateInfoCache rateInfo= RateService.getRateById(rateid);
			if(rateInfo ==null)
				return "not find rateInfo !";
			
			paramValid = true;
			
			return rateInfo.getRateInfo().toString();
		}
		return "params invalid";
    }
	public static String getRealData( Map<String, List<String>> params)
    {
		boolean paramValid = false;
		String epCode = null;
		int epGunNo = 0;
		if (params.isEmpty()) {
			return "params.isEmpty()";
		}
		List<String> vals = params.get("code");
		List<String> valsGunNo = params.get("gunno");
		if (vals != null && vals.size() >= 1 && valsGunNo != null
				&& valsGunNo.size() >= 1) {
			epCode = vals.get(0);
			epGunNo = Integer.parseInt(valsGunNo.get(0));

			paramValid = true;

		}

		if (!paramValid) {
			return "params invalid";
		}
		int error = checkSign(params);
		if(error>0)
			return "checkSign fail";
		
		/*EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		if (epGunCache != null) {
			RealChargeInfo realInfo = epGunCache.getRealChargeInfo();
			if(realInfo !=null)
			{
			   String realString = realInfo.toString();
			   return (realString);
			}
			else
				return "RealChargeInfo is null";
		} else {
			String value = MessageFormat.format("getRealData! not find,epCode:{0}, epGunNo:{1}\n", epCode,
					epGunNo);
			return (value);
		}*/
		
		return "";

    }
	
	public static String createIdentyCode( Map<String, List<String>> params)
    {
		boolean paramValid = false;
    	if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	
        String epCode = null;
		int gunno =0;
		
		List<String> vals = params.get("code");
		List<String> vals1 = params.get("gunno");
		
		if(vals!=null && vals.size()>=1 
				&& vals1!=null && vals1.size()>=1)
		{
			int error = checkSign(params);
			if(error>0)
				return "checkSign fail";
			
			epCode =  vals.get(0);
			gunno=Integer.parseInt(vals1.get(0));
			EpGunCache epGunCache = CacheService.getEpGunCache(epCode, gunno);
			if (epGunCache == null) 
			{
				return "epGunCache is null, send fail!";
			}
			//EpService.handleEpIdentyCodeQuery(epCode, gunno,(byte)0,(byte)0,(byte)0);
	
			if(error>0)
			{
				String str = "error:"+error;
				return str;
			}
			paramValid = true;
			
			return "createIdentyCode success!";
		}
		return "params invalid";
		
    }
	
	public static String getLastConsumeRecord( Map<String, List<String>> params)
    {
		boolean paramValid = false;
    	if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
    	
        String epCode = null;
		int gunno =0;
		
		List<String> vals = params.get("code");
		
		if(vals!=null && vals.size()>=1 )
		{
			int error = checkSign(params);
			if(error>0)
				return "checkSign fail";
			
			epCode =  vals.get(0);
			char[] buf = new char[1024];
			String fileName =epCode + ".log";
			int leng = FileUtils.readLog(fileName, buf);
			if(leng==0)
				return "no find";
			final StringBuilder sb = new StringBuilder();
			sb.append(buf);
			
			return sb.toString();
		}
		return "params invalid";
		
    }
	
	
	
	
	
	
	
	public static String getChNum( Map<String, List<String>> params)
    {
		if (params.isEmpty()) {
        	return "params.isEmpty()";
        }
        
		int error = checkSign(params);
		if(error>0)
			return "checkSign fail";
		
		final StringBuilder sb = new StringBuilder();
		
		/*String epStatMsg =  EpCommClientService.getDebugInfo();
		
		String stationStatMsg =  EpConcentratorService.getDebugInfo();*/
		
		String PhoneStatMsg =  PhoneService.getDebugInfo();
		
		sb.append(PhoneStatMsg);
		
		return sb.toString();
    }
	
	
	
}
	
