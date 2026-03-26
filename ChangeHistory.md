## 2026-03-25: T-1000: Use a faster serializer



@Override
public void write(int pageIndex, List<?> list, String folderPath) {
File folder = new File(folderPath);
folder.mkdirs();
//Kryo kryo = new Kryo();

        try (ByteArrayOutputStream memoryOut = new ByteArrayOutputStream();
             Output output = new Output(memoryOut)) {
            kryo.writeObject(output, list);
            //writeToFile(0, memoryOut.toByteArray());
            int startPointer = pointer;
            buffer.put(pointer, output.toBytes());
            pointer = output.position()+2;
            int endPointer = pointer;
            this.pageIndexMap.put(pageIndex, new Integer[]{startPointer, endPointer-startPointer});
            //buffer.force();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List read(int pageIndex, String path) {
        Integer[] pointer = this.pageIndexMap.get(pageIndex);
        byte[] bin = new byte[pointer[1]];
        buffer.get(bin, pointer[0], pointer[1]);
        Input input = new Input(bin);
        List obj = kryo.readObject(input, ArrayList.class);
        return obj;
    }


https://github.com/esotericsoftware/kryo

    <dependency>
        <groupId>com.esotericsoftware</groupId>
        <artifactId>kryo</artifactId>
        <version>5.6.2</version>
    </dependency>
