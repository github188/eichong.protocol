package com.epcentre.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.config.GameConfig;
import com.epcentre.protocol.PhoneConstant;
import com.epcentre.protocol.WmIce104Util;

/**
 * 收消息，解码   
 * 
 * 消息结构：2字节协议头+ 2字节长度 (小端)+ 1字节原因+2字节命令类型  + byte[] 数据内容

 */
public class PhoneDecoder extends ByteToMessageDecoder {
	
	private static final Logger logger = LoggerFactory.getLogger(PhoneDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext,
			ByteBuf byteBuf, List<Object> list) throws Exception {
		
		String errorMsg="";
		int readableBytes= byteBuf.readableBytes();
		if(readableBytes<7)//如果长度小于长度,不读
		{
		
			logger.debug("decode 1 readableBytes<7,readableBytes:{},channel:{}", readableBytes,channelHandlerContext.channel());


			return;
		}
		
		int pos= byteBuf.bytesBefore(PhoneConstant.HEAD_FLAG1);//找到的位置
		int pos1= byteBuf.bytesBefore(PhoneConstant.HEAD_FLAG2);//找到的位置
		int discardLen=0;
		if(pos < 0 || pos1<0 || (pos1-pos)!=1)//没找到，全部读掉
		{
			discardLen = readableBytes;
			logger.debug("decode not find flag header 0x45 0x43,readableBytes:{},channel:{}",readableBytes,channelHandlerContext.channel());
		}
		if(pos>0&&(pos1-pos)==1)
		{
			discardLen = pos;	
			logger.debug("decode  find flag header 0x45 0x43,pos:{},channel:{}",pos,channelHandlerContext.channel());
		}
		if(discardLen>0)
		{
			byte[] dicardBytes= new byte[discardLen];
			byteBuf.readBytes(dicardBytes);//
			
			if(GameConfig.printPhoneMsg==1)
			{
				logger.info("discard>0 msg:{},channel:{}",WmIce104Util.ConvertHex(dicardBytes, 0),channelHandlerContext.channel());
			}
			else
			{
				logger.debug("discard>0 msg:{},channel:{}",WmIce104Util.ConvertHex(dicardBytes, 0),channelHandlerContext.channel());
			}
			if(discardLen == readableBytes)
			{
				//没有数据可对，还回
				return;
			}
		}
		
		readableBytes= byteBuf.readableBytes();
		if(readableBytes<7)
		{
			logger.debug("decode readableBytes<7 readableBytes:{},channel:{}",readableBytes,channelHandlerContext.channel());
			
			return;
		}
		
		//1、先标记读索引（必须）
		byteBuf.markReaderIndex();
		
		short protocolhead = byteBuf.readShort();//读取协议头
		
		int lengL = byteBuf.readByte()&0x0ff;
		int lengH = byteBuf.readByte()&0x0ff;
	    
		int msg_len = lengL+lengH*0x100;
		
		int remain_len = byteBuf.readableBytes();

		if(remain_len<msg_len )
		{
			logger.debug("ep remain_len<msg_len,remain_len:{},channel:{}", remain_len, channelHandlerContext.channel());
			
				
			byteBuf.resetReaderIndex();
			return ;
		}
		
		byte Msg[]= null;
		Msg= new byte[msg_len];
    	byteBuf.readBytes(Msg);
    
    	PhoneMessage message = new PhoneMessage();
    			
    	message.setLength(msg_len);
    	message.setProtocolHead(protocolhead);

    	message.setBytes(Msg);
    			
    	list.add(message);
		
	}

	

}
