package com.fusesource.activemq.exercises.exclusive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Created by IntelliJ IDEA.
 * User: cposta
 * Date: 3/23/12
 * Time: 9:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleConsumer {
    private static final Log LOG = LogFactory.getLog(SimpleConsumer.class);

    private static final Boolean NON_TRANSACTED = false;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";
    private static final String DESTINATION_NAME = "queue/simple";
    private static final int MESSAGE_TIMEOUT_MILLISECONDS = 120000;

    public static void main(String[] args) {
        Connection connection = null;
        try {
            // start up an init context... properties file must be named "jndi.properties" on the root
            // of the classpath
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Queue destination = (Queue) context.lookup(DESTINATION_NAME);

            connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);

            // reconfigure the destination
            // this will make it so that ONLY one consumer can listen on the queue at one time!!! Others
            // will wait around until the consumer that does have exclusive rights to it has gone a way
            destination = session.createQueue(destination.getQueueName() + "?consumer.exclusive=true");

            // consumer of the queue
            MessageConsumer consumer = session.createConsumer(destination);

            LOG.info("Start consuming messages from " + destination.toString() + " with " + MESSAGE_TIMEOUT_MILLISECONDS + "ms timeout");

            // synchronous message consumer
            int i = 1;
            while (true) {
                Message message = consumer.receive(MESSAGE_TIMEOUT_MILLISECONDS);
                if (message != null) {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        LOG.info("Got " + (i++) + ", message: " + text);
                    } else {
                        break;
                    }
                }
            }

            consumer.close();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
