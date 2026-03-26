package com.github.cvezga.swaplist.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.cvezga.swaplist.SwapListConfig;
import com.github.cvezga.swaplist.SwapListPage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class KryoSerializer implements Serializer {
    private static final Kryo kryo;

    static {
        kryo = new Kryo();
        kryo.register(String.class);
        kryo.register(ArrayList.class);
        kryo.register(Date.class);
        kryo.register(SwapListPage.class);
        kryo.register(SwapListConfig.class);
        kryo.register(ArrayList.class);
        //kryo.register(java.util.List.class);
        kryo.setReferences(true);
        kryo.setRegistrationRequired(true);
        kryo.setWarnUnregisteredClasses(true); // IMPORTANT
    }

    @Override
    public byte[] serialize(SwapListPage<Serializable> object) {
        try (ByteArrayOutputStream memoryOut = new ByteArrayOutputStream();
             Output output = new Output(memoryOut)) {
            kryo.writeObject(output, object);
            output.flush();
            return memoryOut.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return  kryo.readObject(new Input(bytes), clazz);
    }
}
