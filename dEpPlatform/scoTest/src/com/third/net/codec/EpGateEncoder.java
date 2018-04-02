package com.third.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netCore.netty.buffer.DynamicByteBuffer;
import com.third.config.GameConfig;
import com.third.constant.BaseConstant;
import com.third.net.constant.InnerHead;
import com.third.net.constant.Third2EpGate;

/**
 * 发消息，编码
 * 
 * 消息结构：byte混淆码1 + byte混淆吗2 + int长度  + short协议号  + byte是否压缩  + byte[] 数据内容 + byte混淆码3 + byte混淆码4
 * 
 * @author haojian
 * Mar 27, 2013 4:11:15 PM
 */

public class EpGateEncoder extends MessageToByteEncoder{


	private static final Logger logger = LoggerFactory.getLogger(EpGateEncoder.class.getName() + BaseConstant.SPLIT + GameConfig.serverName);
	
	/**
	 * 不管channel.write(arg0)发送的是什么类型，
	 * 最终都要组装成 ByteBuf 发送,
	 * 所以encode需要返回 ByteBuf 类型的对象
	 * @author haojian
	 * Mar 27, 2013 5:18:00 PM
	 * @param chc
	 * @param bb   (Message)
	 * @param byteBuf   (Byte)
	 * @return
	 * @throws Exception
	 */
	@Override
	protected void encode(ChannelHandlerContext chc, Object msg, ByteBuf byteBuf)
			throws Exception {
		
		if(msg instanceof ByteBuf){
			
			ByteBuf byteBufIn = (ByteBuf)msg;
			byte[] bb = new byte[byteBufIn.readableBytes()];
			byteBufIn.getBytes(0, bb);
			
			byteBuf.writeBytes(bb);
			
		}else if(msg instanceof byte[]){
			
			byte[] bb = (byte[])msg;
			byteBuf.writeBytes(bb);
			
		}else{
			
			logger.debug("未知的消息类型... channel:{}",chc.toString());
			
		}
		
		
	}

	public static byte[] Package(int cmd,byte[] msgBody) {
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate(msgBody.length+4);
		
		short len = (short)(msgBody.length+2);
		byteBuffer.put(InnerHead.HEAD_FLAG1);
		byteBuffer.put(InnerHead.HEAD_FLAG2);
		byteBuffer.putShort(len);
		byteBuffer.putShort((short)cmd);
		
		byteBuffer.put(msgBody);
		
		return byteBuffer.getBytes();
	}

    //心跳
	public static byte[] heart()
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();

		return Package(Third2EpGate.EP_HEART,byteBuffer.getBytes());
	}
}
