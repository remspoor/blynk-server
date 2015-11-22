package cc.blynk.server.model.widgets.notifications;

import cc.blynk.server.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Twitter extends Widget {

    public String token;

    public String secret;

    public String username;

    public void cleanPrivateData() {
        token = null;
        secret = null;
        username = null;
    }

}