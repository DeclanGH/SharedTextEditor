package io.github.declangh.sharedtexteditor;

import com.google.crypto.tink.KeysetHandle;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.*;

public class UserService {

    private static UserService instance;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;
    private final String TOPIC = "SharedTextEditor1";
    public final String USER_ID = UUID.randomUUID().toString();

    private final String GROUP_ID = String.valueOf(new Random().nextInt(20) + 1);

    private static int numAgreed = 1;
    private static KeysetHandle key;
    private final String ASSOCIATED_DATA = "8b7483ac761ff7a6928ebde17be8e8172f2a24f13569313cd91df5aede45c73f";


    private UserService(){
        setupProducer();
        setupConsumer();

        try {
            key = AEADEncryption.createKey();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        System.out.println(GROUP_ID);
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
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Arrays.asList(TOPIC), new ConsumerGroupListener());

        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    if (!USER_ID.equals(record.key())) {
                        byte[] packet = record.value();
                        EditorClient.receivePacket(packet);
                        //byte[] encryptedPacket = record.value();
//                        try {
//                            byte[] packet = AEADEncryption.decrypt(encryptedPacket, getInstance().ASSOCIATED_DATA, key);
//                            EditorClient.receivePacket(packet);
//                        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
//                            throw new RuntimeException(e);
//                        }
                        //System.out.println("Packet received");
                    }
                }
            }
        }).start();
    }

    // This class is used to implement a listener for when users join the group
    static class ConsumerGroupListener implements ConsumerRebalanceListener{
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions){
            System.out.println("USER HAS LEFT THE SESSION");
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            System.out.println("USER HAS JOINED THE SESSION");
            try {
                //Create a packet for the key
                byte[] keyPacket = Packets.createKeyPacket(UserService.getInstance().USER_ID, numAgreed, AEADEncryption.keyToByteArray(key));
                UserService.getInstance().broadcast(keyPacket);
                System.out.println("Sending key");
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void broadcast(byte[] packet) {

        // Send message to the topic and register a callback
        List<PartitionInfo> partitions = producer.partitionsFor(TOPIC);
        // Send a message to each topic that is not the one your consumer is
        if(Packets.parseOperation(packet) != Packets.Operation.KEY){
            try {
                packet = AEADEncryption.encrypt(packet, getInstance().ASSOCIATED_DATA, key);
                //System.out.println("encrypting packet with " + key);
            } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
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

    // This method is called when the editor client class closed
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

    public byte[] decryptPacket(byte[] packet) throws GeneralSecurityException, UnsupportedEncodingException {
        System.out.println("decrypting with " + key);
        byte[] decryptedPacket = AEADEncryption.decrypt(packet, ASSOCIATED_DATA, key);

        return decryptedPacket;
    }
    public int getNumAgreed(){
        return numAgreed;
    }

    public void setNumAgreed(int numAgreed){
        UserService.numAgreed = numAgreed;
    }

    public void setKey(byte[] keyBytes) throws GeneralSecurityException, IOException {

        key = AEADEncryption.byteArrayToKey(keyBytes);
        numAgreed += 1;
        System.out.println("set key to " + key);
        System.out.println("Num now agreed" + numAgreed);
    }
}