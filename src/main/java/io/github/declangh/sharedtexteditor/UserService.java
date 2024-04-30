package io.github.declangh.sharedtexteditor;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class UserService {

    private static UserService instance;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;
    private final String topic = "SharedTextEditor";

    private UserService(){
        setupProducer();
        setupConsumer();
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
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);
    }

    private void setupConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "1");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(properties);

        System.out.println(Collections.singletonList("SharedTextEditor"));
        consumer.subscribe(Collections.singletonList("SharedTextEditor"));

        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                //System.out.println("records" + records.count());
                for (ConsumerRecord<String, byte[]> record : records) {
                    EditorClient.receivePacket(record.value());
                    System.out.println("Packet received");
                    System.out.println(record.value());
                }
            }
        }).start();
    }

    public void broadcast(byte[] packet) {
        System.out.println("broadcasting");

        // Send message to the topic and register a callback
        producer.send(new ProducerRecord<>(topic, packet), (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent successfully to topic: " + metadata.topic() +
                        ", partition: " + metadata.partition() +
                        ", offset: " + metadata.offset());
            } else {
                System.err.println("Error sending message: " + exception.getMessage());
            }
        });
    }
}