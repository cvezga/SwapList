package com.github.cvezga.swaplist.serializer;

import com.github.cvezga.swaplist.SwapListPage;

import java.io.Serializable;

public interface Serializer {

    byte[] serialize(SwapListPage<Serializable> object);

    <T> Object deserialize(Class<T> clazz, byte[] bytes);

}
