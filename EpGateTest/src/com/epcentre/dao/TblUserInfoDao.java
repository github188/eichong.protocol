package com.epcentre.dao;

import java.util.List;
import java.util.Map;

import com.epcentre.model.TblUserInfo;

public interface TblUserInfoDao {
	
    public List<TblUserInfo> findUserInfoByPhone(String account);
    
    public List<TblUserInfo> findUserInfoById(int accountId);
    
    public int updateBalance(TblUserInfo info);
    
    public int subCost(TblUserInfo info);
    
    public int addCost(TblUserInfo info);
    
    public int insertUser(Map<String, Object> map);
    
    public int insertNormalUser(Map<String, Object> map);
	
}
