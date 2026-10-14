package com.cheatbreaker.client.nethandler.apollo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small, bounded protobuf wire reader: only the Apollo messages we actually support. */
final class ProtoWire {
    private final Map<Integer, List<Object>> fields = new HashMap<>();

    ProtoWire(byte[] bytes) {
        if (bytes.length > 32767) throw new IllegalArgumentException("Oversized Apollo payload");
        Cursor c = new Cursor(bytes);
        int count = 0;
        while (c.pos < bytes.length) {
            long tag = c.varint();
            int field = (int)(tag >>> 3), wire = (int)(tag & 7);
            if (field <= 0 || tag >>> 3 > 0x1fffffff || ++count > 8192)
                throw new IllegalArgumentException("Invalid protobuf field");
            Object value;
            switch (wire) {
                case 0: value = c.varint(); break;
                case 1: value = c.fixed(8); break;
                case 2:
                    long len = c.varint();
                    if (len < 0 || len > bytes.length - c.pos) throw new IllegalArgumentException("Truncated protobuf");
                    value = Arrays.copyOfRange(bytes, c.pos, c.pos + (int)len);
                    c.pos += (int)len;
                    break;
                case 5: value = c.fixed(4); break;
                default: throw new IllegalArgumentException("Unsupported protobuf wire type");
            }
            fields.computeIfAbsent(field, k -> new ArrayList<>()).add(value);
        }
    }

    List<Object> all(int field) { return fields.getOrDefault(field, Collections.emptyList()); }
    boolean has(int field) { return fields.containsKey(field); }
    List<Long> numbers(int field) {
        List<Long> result=new ArrayList<>();
        for(Object value:all(field)) {
            if(value instanceof Long)result.add((Long)value);
            else {Cursor packed=new Cursor((byte[])value);while(packed.pos<packed.bytes.length) {
                if(result.size()>=64)throw new IllegalArgumentException("Too many packed values");
                result.add(packed.varint());
            }}
        }
        return result;
    }
    private Object last(int field) {
        List<Object> values = all(field);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }
    long number(int field) { Object v = last(field); return v == null ? 0 : (Long)v; }
    float decimal(int field) { return Float.intBitsToFloat((int)number(field)); }
    byte[] bytes(int field) { Object v = last(field); return v == null ? new byte[0] : (byte[])v; }
    String string(int field) { return new String(bytes(field), StandardCharsets.UTF_8); }
    ProtoWire message(int field) { return new ProtoWire(bytes(field)); }

    static final class Writer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private void varint(long n) {
            while ((n & ~127L) != 0) { out.write((int)n & 127 | 128); n >>>= 7; }
            out.write((int)n);
        }
        Writer number(int field, long n) { varint((long)field << 3); varint(n); return this; }
        Writer fixed64(int field, long n) { varint((long)field << 3 | 1); fixed(n, 8); return this; }
        Writer decimal(int field, float n) { varint((long)field << 3 | 5); fixed(Float.floatToIntBits(n), 4); return this; }
        private void fixed(long n, int size) { for (int i = 0; i < size; i++) { out.write((int)n & 255); n >>>= 8; } }
        Writer bytes(int field, byte[] b) { varint((long)field << 3 | 2); varint(b.length); out.write(b, 0, b.length); return this; }
        Writer string(int field, String s) { return bytes(field, s.getBytes(StandardCharsets.UTF_8)); }
        byte[] build() { return out.toByteArray(); }
    }

    private static final class Cursor {
        final byte[] bytes;
        int pos;
        Cursor(byte[] bytes) { this.bytes = bytes; }
        long varint() {
            long result = 0;
            for (int i = 0; i < 10; i++) {
                if (pos >= bytes.length) throw new IllegalArgumentException("Truncated varint");
                int b = bytes[pos++] & 255;
                if (i == 9 && (b & 254) != 0) throw new IllegalArgumentException("Varint overflow");
                result |= (long)(b & 127) << (i * 7);
                if ((b & 128) == 0) return result;
            }
            throw new IllegalArgumentException("Varint overflow");
        }
        long fixed(int size) {
            if (bytes.length - pos < size) throw new IllegalArgumentException("Truncated fixed value");
            long result = 0;
            for (int i = 0; i < size; i++) result |= (long)(bytes[pos++] & 255) << (i * 8);
            return result;
        }
    }
}
