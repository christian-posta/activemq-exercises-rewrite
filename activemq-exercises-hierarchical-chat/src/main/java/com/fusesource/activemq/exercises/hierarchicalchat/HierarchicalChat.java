package com.fusesource.activemq.exercises.hierarchicalchat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: cposta
 * Date: 3/27/12
 * Time: 9:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class HierarchicalChat {
    private static final Log LOG = LogFactory.getLog(HierarchicalChat.class);
    private static final Boolean NON_TRANSACTED = false;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";
    private static final long MESSAGE_LIFESPAN = 30 * 60 * 1000; // 30 minutes

    public static void main(String[] args) {
        final String chatter = System.getProperty("ChatName");
        final String chatRoomName = "topic/" + chatter;

        LOG.info("Start hierarchical chat client for " + chatter + ", on topic: " + chatRoomName);

        Connection connection = null;
        try {
            // start up an init context... properties file must be named "jndi.properties" on the root
            // of the classpath
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Topic publisherTopic = (Topic) context.lookup(chatRoomName);

            connection = factory.createConnection();
            connection.setClientID(chatter);

            Session pubSession = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageProducer publisher = pubSession.createProducer(publisherTopic);

            Session subSession = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);

            String policyType = System.getProperty("PolicyType", ".*");
            String subscriberTopicName = publisherTopic.getTopicName() + policyType;
            Topic subscriberTopic = subSession.createTopic(subscriberTopicName);

            MessageConsumer subscriber = subSession.createConsumer(subscriberTopic);
            subscriber.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            String text = textMessage.getText();
                            System.out.println("RECEIVED >> '" + text + "'");
                        }
                    } catch (JMSException e) {
                        LOG.error("Got an JMSException handling message: " + message, e);
                    }
                }
            });

            System.out.println(
                    "\nHierarchical Chat application:\n"
                            + "==============================\n"
                            + "The application will publish messages to the '" + publisherTopic.getTopicName() + "' topic.\n"
                            + "The application also creates a simple subscription to that topic with this name: '" + chatter + "'\n"
                            + "on this topic '" + (subscriberTopicName) + "' consume any messages published there.\n\n"
                            + "Type some text, and then press Enter to publish it as a Text Message from '" + chatter + "'.\n"
            );

            connection.start();

            Scanner inputReader = new Scanner(System.in);
            while (true) {
                String line = inputReader.nextLine();
                if (line == null) {
                    LOG.info("No Line -> Exit this chat");
                    break;
                }
                else if (line.length() > 0) {
                    try {
                        TextMessage message = pubSession.createTextMessage();
                        message.setText(chatter + ": " + line);
                        System.out.println("SEND >> '" + message.getText() + "'");
                        publisher.send(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, MESSAGE_LIFESPAN);
                    } catch (JMSException e) {
                        LOG.error("Exception during publishing a message: ", e);
                    }
                }
            }

            subscriber.close();
            subSession.close();
            publisher.close();
            pubSession.close();


        } catch (Exception e) {
            LOG.error(e);
        } finally {
            // got to clean up the connections and other resources!
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    LOG.error(e);
                }
            }
        }


    }

}
