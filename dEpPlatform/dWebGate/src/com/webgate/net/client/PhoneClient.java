package com.webgate.net.client;



public class PhoneClient  extends ECTcpClient {
	
	private String  epCode; 
	
	private int epGunNo;

	private int accountId;
	
	private String account;
	
	private String ip;
	private int version;
	private int cmd;
	
	public PhoneClient()
	{
		epCode=""; 	
		epGunNo =0;
		accountId =0;
		
		account="";
		
	}
	
	public String getEpCode() {
		return epCode;
	}
	public void setEpCode(String epCode) {
		this.epCode = epCode;
	}
	

	
	
	public int getAccountId() {
		return accountId;
	}
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}
	public void setEpGunNo(int epGunNo) {
		this.epGunNo = epGunNo;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getEpGunNo() {
		return epGunNo;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getCmd() {
		return cmd;
	}

	public void setCmd(int cmd) {
		this.cmd = cmd;
	}

	@Override
	public String toString() {
		
		final StringBuilder sb = new StringBuilder();
        sb.append("PhoneClient");
        
        sb.append("{epCode=").append(epCode).append("}\n");
        sb.append(",{epGunNo=").append(epGunNo).append("}\n");
        sb.append(",{accountId=").append(accountId).append("}\n");
        
       
        sb.append(",{account=").append(account).append("}\n");
        sb.append(",{ip=").append(ip).append("}\n");
   		return sb.toString();
	}

	

	
}
