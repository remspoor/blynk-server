package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.protocol.exceptions.InvalidTokenException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetSharedDashLogic {

    private final UserDao userDao;

    public GetSharedDashLogic(UserDao userDao) {
        this.userDao = userDao;
    }

    private static Integer getSharedDashId(Map<Integer, String> sharedTokens, String token) {
        for (Map.Entry<Integer, String> entry : sharedTokens.entrySet()) {
            if (entry.getValue().equals(token)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static void cleanPrivateData(DashBoard dashBoard) {
        Twitter twitter = dashBoard.getWidgetByType(Twitter.class);
        if (twitter != null) {
            twitter.cleanPrivateData();
        }

        Notification notification = dashBoard.getWidgetByType(Notification.class);
        if (notification != null) {
            notification.cleanPrivateData();
        }
    }

    public void messageReceived(ChannelHandlerContext ctx, StringMessage message) {
        String token = message.body;

        User userThatShared = userDao.sharedTokenManager.getUserByToken(token);

        if (userThatShared == null) {
            throw new InvalidTokenException("Illegal sharing token. No user with those shared token.", message.id);
        }

        Integer dashId = getSharedDashId(userThatShared.dashShareTokens, token);

        if (dashId == null) {
            throw new InvalidTokenException("Illegal sharing token. User has not token. Could happen only in rare cases.", message.id);
        }

        DashBoard dashBoard = userThatShared.profile.getDashById(dashId, message.id);
        cleanPrivateData(dashBoard);

        ctx.writeAndFlush(produce(message.id, message.command, dashBoard.toString()), ctx.voidPromise());
    }

}