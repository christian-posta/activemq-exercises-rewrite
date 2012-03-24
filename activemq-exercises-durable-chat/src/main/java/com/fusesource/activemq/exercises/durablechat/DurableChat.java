package com.fusesource.activemq.exercises.durablechat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: cposta
 * Date: 3/23/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class DurableChat {
    private static final Log LOG = LogFactory.getLog(DurableChat.class);

    private static final Boolean NON_TRANSACTED = false;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";
    private static final String DESTINATION_NAME = "topic/chat";
    private static final long MESSAGE_LIFESPAN = 30 * 60 * 1000; // 30 mins

    public static void main(String[] args) {
        Connection connection = null;
        try {
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Topic destination = (Topic) context.lookup(DESTINATION_NAME);

            connection = factory.createConnection();
            String chatter = System.getProperty("ChatName");

            // must set a client ID for a durable sub (a ClientID as well as a subscriber name)
            connection.setClientID(chatter);

            Session pubSession = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageProducer publisher = pubSession.createProducer(destination);

            Session subSession = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);

            // got to set a name for the durable subscriber
            MessageConsumer subscriber = subSession.createDurableSubscriber(destination, chatter);
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
                        LOG.error("Got an JMS Exception handling message: " + message, e);
                    }
                }
            });

            LOG.info("Start simple chat client for: " + chatter);

            System.out.println(
                    "\nDurableChat application:\n"
                            + "========================\n"
                            + "The application will publish messages to the " + destination.toString() + " topic.\n"
                            + "The application also creates a simple subscription to that topic with this name: '" + chatter
                            + "'to consume any messages published there.\n\n"
                            + "Type some text, and then press Enter to publish it as a Text Message from " + chatter + ".\n"
            );

            // Start connection AFTER sysout because if there are pending messages, they would
            // get printed BEFORE the direction text
            connection.start();

            // a kind of tokenizer
            Scanner inputReader = new Scanner(System.in);

            while (true) {
                String line = inputReader.nextLine();
                if (line == null) {
                    LOG.info("No line -> Exit this chat");
                    break;
                }
                else if (line.length() > 0) {
                    try {
                        TextMessage message = pubSession.createTextMessage();
                        message.setText(chatter + ": " + line);
                        System.out.println("SEND >> '" + message.getText() + "'");

                        // publish the message persistently
                        publisher.send(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, MESSAGE_LIFESPAN);
                    } catch (JMSException e) {
                        LOG.error("Exception during publishing a message; ", e);
                    }
                }

            }

            subscriber.close();
            subSession.close();
            publisher.close();
            pubSession.close();


        } catch (Exception e) {
            LOG.error(e);
        }
                finally {
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
