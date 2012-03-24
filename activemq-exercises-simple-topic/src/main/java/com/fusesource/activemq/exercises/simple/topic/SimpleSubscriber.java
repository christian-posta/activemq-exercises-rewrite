package com.fusesource.activemq.exercises.simple.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Created by IntelliJ IDEA.
 * User: cposta
 * Date: 3/23/12
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleSubscriber {
    private static final Log LOG = LogFactory.getLog(SimpleSubscriber.class);

    private static final Boolean NON_TRANSACTED = false;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";
    private static final String DESTINATION_NAME = "topic/simple";
    private static final String CONTROL_DESTINATION_NAME = "topic/control";
    private static final int MESSAGE_TIMEOUT_MILLISECONDS = 10000;

    public static void main(String[] args) {
        Connection connection = null;
        try {
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Destination destination = (Destination) context.lookup(DESTINATION_NAME);
            Destination controlDestination = (Destination) context.lookup(CONTROL_DESTINATION_NAME);

            connection = factory.createConnection();

            // create a session for the control producer
            Session session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageProducer controlProducer = session.createProducer(controlDestination);

            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(new JmsMessageListener(session, controlProducer));

            // Must have a separate session or connection for the sync MessageConsumer
            // per JMS spec you cannot have sync and async message consumers on the same
            // session
            Session controlSession = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer controlConsumer = controlSession.createConsumer(controlDestination);

            // calling start after the listeners have been registered
            connection.start();


            LOG.info("Start control message consumer");
            int i = 1;
            while (true) {
                // sync receive for message consumer
                Message message = controlConsumer.receive(MESSAGE_TIMEOUT_MILLISECONDS);
                if (message != null) {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String text = textMessage.getText();
                        LOG.info("Got " + (i++) + ". message: " + text);

                        if (text.startsWith("SHUTDOWN")) {
                            break;
                        }
                    }
                }
            }

            controlConsumer.close();
            controlSession.close();
            consumer.close();
            controlProducer.close();
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

    private static class JmsMessageListener implements MessageListener {
        private static final Log LOG = LogFactory.getLog(JmsMessageListener.class);
        private Session session;
        private MessageProducer producer;

        private int count = 0;
        private long start = System.currentTimeMillis();

        public JmsMessageListener(Session session, MessageProducer controlProducer) {
            this.session = session;
            this.producer = controlProducer;
        }

        @Override
        public void onMessage(Message message) {
            try {
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String text = textMessage.getText();

                    if ("SHUTDOWN".equals(text)) {
                        LOG.info("Got the SHUTDOWN command -> exit");
                        producer.send(session.createTextMessage("SHUTDOWN is being performed"));
                    }
                    else if ("REPORT".equals(text)) {
                        long time = System.currentTimeMillis() - start;
                        producer.send(session.createTextMessage("Received " + count + " in " + time + "ms"));
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            LOG.info("Wait for the report message to be sent was interrupted");
                        }
                        count = 0;
                    }
                    else {
                        if (count == 0) {
                            start = System.currentTimeMillis();
                        }

                        count++;
                        LOG.info("Received " + count + " messages.");
                    }
                }

            } catch (JMSException e) {
                LOG.error("Got an JMS Exception handling message: " + message, e);
            }
        }
    }
}
