package com.cooperate.cczc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.cczc.constant.Consts;

public class ELeaseService {
	
	private static final Logger logger =  LoggerFactory.getLogger(ELeaseService.class);
	  
	
    public static String sendChargeResp(Map<String ,Object> params) {
    	
        return send2EChong(Consts.E_CHARGE_RESP_URL, params);
    }
    public static String sendStopChargeResp(Map<String ,Object> params) {
    	
        return send2EChong(Consts.E_STOPCHARGE_RESP_URL, params);
    }
    public static String sendRealData(Map<String ,Object> params) {
    	
        return send2EChong(Consts.E_REAL_DATA_URL, params);
    }
    public static String sendOrderInfo(Map<String ,Object> params) {
    	
        return send2EChong(Consts.E_ORDER_URL, params);
    }
    private static String send2EChong(String url, Map<String ,Object> params2) {
    	
    	JSONObject jsonObject = JSONObject.fromObject(params2);
		
        Map<String, String> params = fullParams(jsonObject.toString());
        if (null == params) {
            logger.error("send2EChong is fail;url={}", url);
            return null;
        }
        String response = null;
        try {
        	logger.info("send2EChong,response:{},url:{}",url);
            response = HttpUtils.httpPost(url, params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("send2EChong,response:{}",response);
        return response;
    }

    private static Map<String, String> fullParams(String info) {
        String app_id = Consts.E_APP_ID;  //e充网分配的app_id
        String app_key = Consts.E_APP_KEY;  //e充网分配的app_key
        
        logger.debug("send2EChong fullParams!app_id={},app_key={},info={}",
       		 new Object[]{app_id, app_key, info});
        
        Map<String, String> params = new HashMap<>();
        params.put("app_id", app_id);
        params.put("info", info);
        String sig = SigTool.getSignString(app_id, info, app_key);
        if (null == sig) {
             logger.error("sig generate is fail;app_id={}|info={}app_key={}",
            		 new Object[]{app_id, info, app_key});
            return null;        }
        params.put("sig", sig);
        return params;
    }

}
