package com.fusesource.activemq.exercises.transactedchat;

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
 * Time: 7:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class TransactedChat {

    private static final Log LOG = LogFactory.getLog(TransactedChat.class);
    private static final Boolean NON_TRANSACTED = false;
    private static final Boolean TRANSACTED = true;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";
    private static final String DESTINATION_NAME = "topic/chat";
    private static final long MESSAGE_LIFESPAN = 30 * 60 * 1000; // 30 minutes

    public static void main(String[] args) {
        Connection connection = null;
        try {
            // start up an init context... properties file must be named "jndi.properties" on the root
            // of the classpath
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Topic destination = (Topic) context.lookup(DESTINATION_NAME);

            connection = factory.createConnection();

            String chatter = System.getProperty("ChatName");
            connection.setClientID(chatter);

            Session pubSession = connection.createSession(TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageProducer publisher = pubSession.createProducer(destination);

            Session subSession = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer subscriber = subSession.createConsumer(destination);

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
                    "\nTransacted Chat application:\n"
                            + "===========================\n\n"
                            + "The application user " + chatter + " connects to the broker.\n"
                            + "The application will stage messages to the " + destination.toString() + " topic until you either commit them or roll them back.\n"
                            + "The application also subscribes to that topic to consume any committed messages published there.\n\n"
                            + "1. Enter text to publish and then press Enter to stage the message.\n"
                            + "2. Add a few messages to the transaction batch.\n"
                            + "3. Then, either:\n"
                            + "     o Enter the text 'COMMIT', and press Enter to publish all the staged messages.\n"
                            + "     o Enter the text 'CANCEL', and press Enter to drop the staged messages waiting to be sent.\n\n"
            );


            // start the connection AFTER the sout because if there are pending messages they would get printed
            // before the direction statement
            connection.start();

            Scanner inputReader = new Scanner(System.in);

            while (true) {
                String line = inputReader.nextLine();
                if (line == null) {
                    LOG.info("No Line -> Exit this chat");
                    break;
                }
                else if (line.length() > 0) {
                    if (line.trim().equals("CANCEL")) {
                        System.out.println("Cancelling messages...");
                        pubSession.rollback();
                        System.out.println("Staged messages have been cleared");
                    }
                    else if (line.trim().equals("COMMIT")) {
                        System.out.println("Comitting messages...");
                        pubSession.commit();
                        System.out.println("Staged messages have been sent");
                    }
                    else {
                        try {
                            TextMessage message = pubSession.createTextMessage();
                            message.setText(chatter + ": " + line);
                            System.out.println("SEND >> '" + message.getText() + "'");

                            // why is this set to persistent?
                            publisher.send(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, MESSAGE_LIFESPAN);
                        } catch (JMSException e) {
                            LOG.error("Exception during publishing a message: ", e);
                        }
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
