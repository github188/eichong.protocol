package com.cooperate.constant;

/**
 * Created by lwz on 2016/12/30.
 */
public class KeyConsts {
    public static final String CCZC_SETTING = "cczc.properties";
    public static final String ELEASE_SETTING = "elease.properties";
    public static final String SHSTOP_SETTING = "shstop.properties";
    public static final String PARK_SIMPLE_SETTING = "parksimple.properties";
    public static final String EV_CHARGE_SETTING = "evcharge.properties";
    public static final String TCEC_CHONG_SETTING = "tcecechong.properties";
    public static final String TCEC_SHENZHEN_SETTING = "tcecshenzhen.properties";
    public static final String TCEC_QINGXIANG_SETTING = "tcecqingxiang.properties";
    public static final String TCEC_NANRUI_SETTING = "tcecnanrui.properties";
    public static final String TCEC_HESHUN_SETTING = "tcecheshun.properties";
    public static final String TCEC_EVC_SETTING = "tcecevc.properties";
    public static final String APP_ID = "app_id";
    public static final String APP_KEY = "app_key";
    public static final String APP_SECRET = "appsecret";
    public static String CHARGE_RESP_URL = "charge_url";
    public static String STOP_CHARGE_RESP_URL = "stopcharge_url";
    public static String CHARGE_EVENT_URL = "chargeevent_url";
    public static String STATUS_CHANGE_URL = "statuschange_url";
    public static String NET_STATUS_CHANGE_URL = "netstatuschange_url";
    public static String REAL_DATA_URL = "realdata_url";
    public static String ORDER_URL = "order_url";
    public static String TOKEN_URL = "token_url";
    
    public static final String ORG_NO = "org_no";
    public static final String REAL_DATA_MODE = "mode";
    public static final String REAL_DATA_PERIOD = "period";
    public static final String REAL_DATA_ORG_CODE = "org_code";
    
    /** 运营商标识 */
    public static final String SYS_OPERATOR_ID = "425010765";
    /** 爱充运营商标识 */
    public static final String PRI_OPERATOR_ID = "MA27W7H33";
    /** 平台密钥 */
    public static final String PRI_OPERATOR_SECRET = "1234567890abcdef";
    /** 初始化向量 */
    public static final String DATA_SECRET_IV = "1234567890abcdef";
    /** Token的固定偏移值 */
    public static final String AUTH_TOKEN = "Bearer ";

    /**
     * 运营商标识（OperatorID）: 固定9位，运营商的组织机构代码，作为运营商的唯一标示
     */
    public static String OPERATOR_ID = "operator_id";
    /**
     * 运营商密钥（Operator_Secret）
     */
    public static String OPERATOR_SECRET = "operator_secret";
    /**
     * 消息密钥（Data_Secret
     */
    public static String DATA_SECRET = "data_secret";
    /**
     * 签名密钥（Sig_Secret
     */
    public static String SIG_SECRET = "sig_secret";
    /**
     * 消息密钥初始化向量（Data_Secret_IV）
     */
    public static String T_DATA_SECRET_IV = "t_data_secret_iv";
}
