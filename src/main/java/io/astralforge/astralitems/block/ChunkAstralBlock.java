package io.astralforge.astralitems.block;

import java.util.Arrays;

import com.google.common.primitives.Bytes;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public class ChunkAstralBlock {

    public NamespacedKey key = null;
    public byte[] data = null; // TODO: Reserved for future use

    public ChunkAstralBlock() { /* do nothing */ }

    public ChunkAstralBlock(NamespacedKey key, byte[] data) {
        this.key = key;
        this.data = data;
    }

    public static final class Serial implements PersistentDataType<byte[], ChunkAstralBlock> {
        private static Serial instance;
        public static Serial get() {
            if (instance == null) {
                instance = new Serial();
            }
            return instance;
        }

        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }
    
        @Override
        public Class<ChunkAstralBlock> getComplexType() {
            return ChunkAstralBlock.class;
        }
    
        @Override
        public byte[] toPrimitive(ChunkAstralBlock complex, PersistentDataAdapterContext context) {
            return Bytes.concat(
                new byte[] { 1 }, // version
                complex.key.toString().getBytes(), 
                new byte[] { ' ' }, 
                complex.data
            );
        }
    
        @Override
        public ChunkAstralBlock fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
            byte version = primitive[0];
            if (version != 1) {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }
    
            int keyEnd = Bytes.indexOf(primitive, (byte)' ');
            if (keyEnd == -1) {
                throw new IllegalArgumentException("Invalid data: " + Arrays.toString(primitive));
            }
    
            byte[] keyBytes = Arrays.copyOfRange(primitive, 1, keyEnd);
            byte[] data = Arrays.copyOfRange(primitive, keyEnd + 1, primitive.length);
    
            return new ChunkAstralBlock(
                NamespacedKey.fromString(new String(keyBytes)),
                data
            );
        }
    }

    // public static void main(String[] args) {
    //     NamespacedKey key = NamespacedKey.minecraft("testval");
    //     byte[] data = "testbytes".getBytes();//new byte[] { 1, 2, 3 };
    //     System.out.println(new String(data));
    //     ChunkAstralBlock block = new ChunkAstralBlock(key, data);
    //     byte[] bytes = new ChunkAstralBlock.Serial().toPrimitive(block, null);
    //     System.out.println(Arrays.toString(bytes));
    //     System.out.println(new String(bytes));
        
    //     ChunkAstralBlock block2 = new ChunkAstralBlock.Serial().fromPrimitive(bytes, null);
    //     System.out.println(block2.key);
    //     System.out.println(new String(block2.data));
    // }
}
