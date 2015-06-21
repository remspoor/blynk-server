package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.PushMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.notifications.twitter.exceptions.NotificationNotAuthorizedException;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class NotificationHandler extends BaseSimpleChannelInboundHandler<PushMessage> {

    private static final int MAX_PUSH_BODY_SIZE = 255;
    private final NotificationsProcessor notificationsProcessor;

    public NotificationHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                               NotificationsProcessor notificationsProcessor) {
        super(props, userRegistry, sessionsHolder);
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, PushMessage message) {
        if (message.body == null || message.body.equals("") || message.body.length() > MAX_PUSH_BODY_SIZE) {
            throw new NotificationBodyInvalidException(message.id);
        }

        Notification widget = user.getProfile().getActiveDashboardWidgetByType(Notification.class);

        if (widget == null ||
                widget.token == null || widget.token.equals("")) {
            throw new NotificationNotAuthorizedException("User has no access token provided.", message.id);
        }

        checkIfNotificationQuotaLimitIsNotReached(user, message);

        log.trace("Sending Twit for user {}, with message : '{}'.", user.getName(), message.body);
        notificationsProcessor.push(widget.token, message.body, message.id);

        ctx.writeAndFlush(produce(message.id, OK));
    }


}