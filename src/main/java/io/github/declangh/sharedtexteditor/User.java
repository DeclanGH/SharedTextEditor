package io.github.declangh.sharedtexteditor;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import java.nio.ByteBuffer;
import java.util.Properties;

public class User {

    //User's unique ID
    int userID;
    Producer<String, byte[]> producer;
    Consumer<String, byte[]> consumer;

    public User(){
        //Instantiate the properties for the producer
        Properties props = new Properties();
        props.put("bootstrap.servers", "pi.cs.oswego.edu:26920");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        //Create this user's producer
        producer = new KafkaProducer<>(props);

        Properties props2 = new Properties();
        props2.put("bootstrap.servers", "pi.cs.oswego.edu:26920");
        props2.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props2.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        //Create this user's consumer
        consumer = new KafkaConsumer<>(props2);
    }

    public static void main(String[] args) throws Exception {
        String update = "UPDATE Diawdnauidnauidnawidnwaid";
        System.out.println(update.getBytes().length);
        //Upon receiving packet, call getInfoFromArray
        getInfoFromArray(update.getBytes());
        // we'll keep this local for now. We can replace with a GitHub server or cs server later
        EditorClient client = new EditorClient();
        
        client.setVisible(true);
        //Import the Editor client to be able to call methods to add to the text area
        
        //Upon receiving message check if the ID matches the 
        //client.test();
        
    }


    //This method will grab the data from the array sent to it 
    public static void getInfoFromArray(byte[] packet){
        //before doing anything, check to make sure the userID in the packet != this user's ID (i.e. the packet isnt from the same user)
            //A delete packet can not be more than 18 bytes long
            int DELETE_PACKET_SIZE = 18;
            int INSERT_BYTES = "INSERT".getBytes().length;
            int DELETE_BYTES = "DELETE".getBytes().length;

            //Wrap the packet in a buffer
            ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

            //A DELETE packet can be no greater than 18 bytes so we can use that as our condition
            //If its greater than 18
            if(packet.length > DELETE_PACKET_SIZE){
                //TODO: ADD LOGIC FOR UPDATE vs INSERT. CAN DO if(getChar() == 'U')
                if((char)packetBuffer.get() == 'U'){
                    //Rewind the buffer
                }

            }
            else{
                
            }

    }


    public int getUserID() {
        return userID;
    }


    public void setUserID(int userID) {
        this.userID = userID;
    }

    //This method is called to broadcast the change to a document to a user
    public void broadcast(byte[] packet){


    }
}

