package io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 10:07 AM
 */
public class Writer {

    public static void writeInstance(String instance,String No){
        BufferedWriter writer = null;
        try{
            writer = new BufferedWriter( new FileWriter("./dataset/"+No+".txt"));
            writer.write(instance);
            writer.flush();
        }catch ( IOException e){
        }finally{
            try{
                if ( writer != null)
                    writer.close( );
            }catch ( IOException e){
            }
        }
    }
}
