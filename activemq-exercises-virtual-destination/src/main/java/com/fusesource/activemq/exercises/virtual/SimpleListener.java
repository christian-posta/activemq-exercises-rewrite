package com.fusesource.activemq.exercises.virtual;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.concurrent.CountDownLatch;

/**
 * Created by IntelliJ IDEA.
 * User: cposta
 * Date: 3/28/12
 * Time: 4:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleListener {
    private static final Log LOG = LogFactory.getLog(SimpleListener.class);
    private static final Boolean NON_TRANSACTED = false;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";

    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) {
        final String destinationName = "destination/" + System.getProperty("destinationExt");
        Connection connection = null;

        try {
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Destination destination = (Destination) context.lookup(destinationName);

            connection = factory.createConnection();

            Session session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(destination);

            consumer.setMessageListener(new MessageListener() {
                private int count = 0;
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            String text = textMessage.getText();
                            LOG.info("Got " + (count++) + ". message: " + text);
                        }
                    } catch (JMSException e) {
                        LOG.error(e);
                    }
                }
            });

            connection.start();
            LOG.info("Start listening on destination: " + destination.toString());


            // add a shutdown hook and wait until shutdown is initialized before cleaning up/closing
            // the consumer/session
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    latch.countDown();
                }
            });

            latch.await();

            consumer.close();
            session.close();


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
