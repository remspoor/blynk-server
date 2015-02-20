package cc.blynk.server.handlers.workflow;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.server.dao.*;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Properties;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static cc.blynk.common.model.messages.protocol.HardwareMessage.attachTS;
import static cc.blynk.common.utils.PropertiesUtil.getIntProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareHandler extends BaseSimpleChannelInboundHandler<HardwareMessage> {

    private final Storage storage;

    public HardwareHandler(Properties properties, FileManager fileManager, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(properties, fileManager, userRegistry, sessionsHolder);
        this.storage = new GraphInMemoryStorage(getIntProperty(properties, "user.in.memory.storage.limit"));
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, HardwareMessage message) throws Exception {
        Session session = sessionsHolder.getUserSession().get(user);

        ChannelState channelState = (ChannelState) ctx.channel();

        //todo
        //for hardware command do not wait for hardware response.
        List<ChannelFuture> futures;
        if (channelState.isHardwareChannel) {
            //if message from hardware, check if it belongs to graph. so we need save it in that case
            String newBody = attachTS(message.body);
            storage.store(user, channelState.dashId, newBody, message.id);
            futures = Session.sendMessageTo(message.updateMessageBody(newBody), session.getAppChannels());
        } else {
            futures = Session.sendMessageTo(message, session.getHardwareChannels());
        }

        ctx.channel().writeAndFlush(produce(message.id, OK));
    }

}