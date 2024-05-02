package io.github.declangh.sharedtexteditor;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

public class UserService {

    private static UserService instance;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;
    private final String TOPIC = "SharedTextEditor";

    private UserService(){
        setupProducer();
        setupConsumer();
        //setupNewUserConsumer(); //This method will contain a thread that watches for when a new user joins the session
    }

    /*
     * We want one instance of our class.
     * if we do not have one, then a new instance shall be made.
     * We also want to return an instance such that it is not being called by multiple threads
     */
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private void setupProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "pi.cs.oswego.edu:26921");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);
    }

    private void setupConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "pi.cs.oswego.edu:26921");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Arrays.asList(TOPIC), new ConsumerGroupListener());

        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, byte[]> record : records) {
                    EditorClient.receivePacket(record.value());
                    System.out.println("Packet received");
                }
            }
        }).start();
    }

    //This class is used to implement a listener for when users join the group
    static class ConsumerGroupListener implements ConsumerRebalanceListener{
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions){
            System.out.println("USER HAS LEFT THE SESSION");
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            System.out.println("USER HAS JOINED THE SESSION");
        }
    }

    public void broadcast(byte[] packet) {

        // Send message to the topic and register a callback

        producer.send(new ProducerRecord<>(TOPIC, packet), (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent successfully to topic: " + metadata.topic() +
                        ", partition: " + metadata.partition() +
                        ", offset: " + metadata.offset());
            } else {
                System.err.println("Error sending message: " + exception.getMessage());
            }
        });
    }

    //This method is called when the editor client class closed
    public synchronized void close(){
        System.out.println("Closing producer and consumers");
        if(producer != null){
            producer.close();
        }
        if(consumer != null){
            synchronized (consumer) {
                consumer.close();
            }
        }
    }
}