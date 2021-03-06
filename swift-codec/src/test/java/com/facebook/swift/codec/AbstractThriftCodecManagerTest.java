/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.DiscreteComponent.BasicComponent;

import com.facebook.swift.codec.generics.ConcreteDerivedFromGeneric;
import com.facebook.swift.codec.generics.ConcreteDerivedFromGenericBean;
import com.facebook.swift.codec.generics.ConcreteThriftStructDerivedFromGenericField;
import com.facebook.swift.codec.generics.GenericThriftStruct;
import com.facebook.swift.codec.generics.GenericThriftStructBean;
import com.facebook.swift.codec.generics.GenericThriftStructField;
import com.facebook.swift.codec.generics.GenericThriftStructFromBuilder;
import com.facebook.swift.codec.internal.EnumThriftCodec;
import com.facebook.swift.codec.internal.coercion.DefaultJavaCoercions;
import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryBuffer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public abstract class AbstractThriftCodecManagerTest
{
    private ThriftCodecManager readCodecManager;
    private ThriftCodecManager writeCodecManager;

    public abstract ThriftCodecManager createReadCodecManager();

    public abstract ThriftCodecManager createWriteCodecManager();

    @BeforeMethod
    protected void setUp()
            throws Exception
    {
        readCodecManager = createReadCodecManager();
        writeCodecManager = createWriteCodecManager();
        readCodecManager.getCatalog().addDefaultCoercions(DefaultJavaCoercions.class);
        writeCodecManager.getCatalog().addDefaultCoercions(DefaultJavaCoercions.class);
    }

    @Test
    public void testUnionFieldsManual()
            throws Exception
    {
        ThriftCatalog catalog = new ThriftCatalog();
        ThriftType unionFieldType = catalog.getThriftType(UnionField.class);
        ThriftType fruitType = catalog.getThriftType(Fruit.class);
        ThriftCodec<Fruit> fruitCodec = new EnumThriftCodec<Fruit>(fruitType);
        UnionFieldThriftCodec unionFieldCodec = new UnionFieldThriftCodec(unionFieldType, fruitCodec);

        UnionField unionField = new UnionField();
        unionField._id = 1;
        unionField.stringValue = "Hello, World";

        testRoundTripSerialize(unionFieldCodec, unionFieldCodec, unionField, new TCompactProtocol.Factory());

        unionField = new UnionField();
        unionField._id = 2;
        unionField.longValue = 4815162342L;

        testRoundTripSerialize(unionFieldCodec, unionFieldCodec, unionField, new TCompactProtocol.Factory());

        unionField = new UnionField();
        unionField._id = 3;
        unionField.fruitValue = Fruit.APPLE; // The best fruit!

        testRoundTripSerialize(unionFieldCodec, unionFieldCodec, unionField, new TCompactProtocol.Factory());
    }

    @Test
    public void testUnionFields()
            throws Exception
    {
        UnionField unionField = new UnionField();
        unionField._id = 1;
        unionField.stringValue = "Hello, World";

        testRoundTripSerialize(unionField, new TCompactProtocol.Factory());

        unionField = new UnionField();
        unionField._id = 2;
        unionField.longValue = 4815162342L;

        testRoundTripSerialize(unionField, new TCompactProtocol.Factory());

        unionField = new UnionField();
        unionField._id = 3;
        unionField.fruitValue = Fruit.APPLE; // The best fruit!

        testRoundTripSerialize(unionField, new TCompactProtocol.Factory());
    }

    @Test
    public void testUnionBean()
            throws Exception
    {
        UnionBean unionBean = new UnionBean();
        unionBean.setStringValue("Hello, World");
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());

        unionBean = new UnionBean();
        unionBean.setLongValue(4815162342L);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());

        unionBean = new UnionBean();
        unionBean.setFruitValue(Fruit.CHERRY);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());
    }
    
    @Test
    public void testUnionInterface()
            throws Exception
    {
        UnionInterface unionBean = UnionInterface.Builder.createInstance("Hello, World");
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());

        unionBean = UnionInterface.Builder.createInstance(8675309L);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());

        unionBean = UnionInterface.Builder.createInstance(Fruit.CHERRY);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());
    }

    @Test
    public void testUnionBuilder()
            throws Exception
    {
        
        UnionBuilder unionBean = new UnionBuilder("Hello, World", (short) 1);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());

        unionBean = new UnionBuilder(8675309L, (short) 2);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());

        unionBean = new UnionBuilder(Fruit.CHERRY, (short) 3);
        testRoundTripSerialize(unionBean, new TCompactProtocol.Factory());
    }

    @Test
    public void testUnionConstructor()
            throws Exception
    {
        UnionConstructor unionConstructor = new UnionConstructor("Hello, World");
        testRoundTripSerialize(unionConstructor, new TCompactProtocol.Factory());

        unionConstructor = new UnionConstructor(4815162342L);
        testRoundTripSerialize(unionConstructor, new TCompactProtocol.Factory());

        unionConstructor = new UnionConstructor(Fruit.APPLE);
        testRoundTripSerialize(unionConstructor, new TCompactProtocol.Factory());
    }

    @Test
    public void testStructFieldsManual()
            throws Exception
    {
        ThriftCatalog catalog = new ThriftCatalog();
        ThriftType bonkFieldType = catalog.getThriftType(BonkField.class);
        BonkFieldThriftCodec bonkFieldCodec = new BonkFieldThriftCodec(bonkFieldType);

        BonkField bonkField = new BonkField("message", 42);
        testRoundTripSerialize(bonkFieldCodec, bonkFieldCodec, bonkField, new TCompactProtocol.Factory());
    }

    @Test
    public void testStructFields()
            throws Exception
    {
        BonkField bonkField = new BonkField("message", 42);
        testRoundTripSerialize(bonkField, new TCompactProtocol.Factory());
    }

    @Test
    public void testStructBean()
            throws Exception
    {
        BonkBean bonkBean = new BonkBean("message", 42);
        testRoundTripSerialize(bonkBean, new TCompactProtocol.Factory());
    }

    @Test
    public void testStructMethod()
            throws Exception
    {
        BonkMethod bonkMethod = new BonkMethod("message", 42);
        testRoundTripSerialize(bonkMethod, new TCompactProtocol.Factory());
    }

    @Test
    public void testStructConstructor()
            throws Exception
    {
        BonkConstructor bonkConstructor = new BonkConstructor("message", 42);
        testRoundTripSerialize(bonkConstructor, new TCompactProtocol.Factory());
    }

    @Test
    public void testMatchByJavaNameWithThriftNameOverride()
            throws Exception
    {
        ThriftCatalog catalog = readCodecManager.getCatalog();
        ThriftType thriftType = catalog.getThriftType(BonkConstructorNameOverride.class);
        ThriftStructMetadata structMetadata = thriftType.getStructMetadata();
        assertEquals(structMetadata.getField(1).getName(), "myMessage");
        assertEquals(structMetadata.getField(2).getName(), "myType");

        BonkConstructorNameOverride bonk = new BonkConstructorNameOverride("message", 42);
        testRoundTripSerialize(bonk, new TCompactProtocol.Factory());
    }

    @Test
    public void testBuilder()
            throws Exception
    {
        BonkBuilder bonkBuilder = new BonkBuilder("message", 42);
        testRoundTripSerialize(bonkBuilder, new TCompactProtocol.Factory());
    }

    @Test
    public void testArraysManual()
            throws Exception
    {
        ThriftCatalog catalog = new ThriftCatalog();
        ThriftType thriftType = catalog.getThriftType(ArrayField.class);
        ArrayFieldThriftCodec arrayFieldCodec = new ArrayFieldThriftCodec(thriftType);

        // manual codec does not implement the Map fields
        ArrayField arrayField = new ArrayField(
                new boolean[] {true, false, false, true},
                new short[] {0, 1, 2, 3},
                new int[] {10, 11, 12, 13},
                new long[] {20, Long.MAX_VALUE, Long.MIN_VALUE},
                new double[] {3.0, Double.MAX_VALUE, Double.MIN_VALUE},
                "hello".getBytes(UTF_8));
        testRoundTripSerialize(arrayFieldCodec, arrayFieldCodec, arrayField, new TCompactProtocol.Factory());
    }

    @Test
    public void testArrays()
            throws Exception
    {
        ArrayField arrayField = new ArrayField(
                new boolean[] {true, false, false, true},
                new short[] {0, 1, 2, 3},
                new int[] {10, 11, 12, 13},
                new long[] {20, Long.MAX_VALUE, Long.MIN_VALUE},
                new double[] {3.0, Double.MAX_VALUE, Double.MIN_VALUE},
                "hello".getBytes(UTF_8),
                ImmutableMap.of((short) 1, new boolean[] {false, false}, (short) 2, new boolean[] {true, true}),
                ImmutableMap.of((short) 1, new short[] {10, 11, 12, 13}, (short) 2, new short[] {15, 16, 17, 18}),
                ImmutableMap.of((short) 1, new int[] {20, 21, 22, 23}, (short) 2, new int[] {25, 26, 27, 28}),
                ImmutableMap.of((short) 1, new long[] {30, 31, 32, 33}, (short) 2, new long[] {35, 36, 37, 38}),
                ImmutableMap.of((short) 1, new double[] {40, 41, 42, 43}, (short) 2, new double[] {45, 46, 47, 48}));

        testRoundTripSerialize(arrayField, new TCompactProtocol.Factory());
        testRoundTripSerialize(arrayField, new TJSONProtocol.Factory());
    }

    @Test
    public void testOneOfEverythingField()
            throws Exception
    {
        OneOfEverything one = createOneOfEverything();
        testRoundTripSerialize(one, new TCompactProtocol.Factory());
        testRoundTripSerialize(one, new TJSONProtocol.Factory());
    }

    @Test
    public void testOneOfEverythingFieldManual()
            throws Exception
    {
        ThriftCatalog catalog = readCodecManager.getCatalog();
        ThriftType bonkFieldType = catalog.getThriftType(BonkField.class);
        ThriftType unionFieldType = catalog.getThriftType(UnionField.class);
        ThriftType fruitType = catalog.getThriftType(Fruit.class);

        ThriftCodec<Fruit> fruitCodec = new EnumThriftCodec<>(fruitType);
        BonkFieldThriftCodec bonkFieldCodec = new BonkFieldThriftCodec(bonkFieldType);
        UnionFieldThriftCodec unionFieldCodec = new UnionFieldThriftCodec(unionFieldType, fruitCodec);

        ThriftType oneOfEverythingType = catalog.getThriftType(OneOfEverything.class);

        OneOfEverythingThriftCodec codec = new OneOfEverythingThriftCodec(
                oneOfEverythingType,
                bonkFieldCodec,
                unionFieldCodec,
                fruitCodec);

        // manual codec only support some fields
        OneOfEverything one = new OneOfEverything();
        one.aBoolean = true;
        one.aByte = 11;
        one.aShort = 22;
        one.aInt = 33;
        one.aLong = 44;
        one.aDouble = 55;
        one.aString = "message";
        one.aEnum = Fruit.CHERRY;
        one.aStruct = new BonkField("struct", 66);

        testRoundTripSerialize(codec, codec, one, new TCompactProtocol.Factory());
        testRoundTripSerialize(codec, codec, one, new TJSONProtocol.Factory());
    }

    @Test
    public void testOneOfEverythingFieldEmpty()
            throws Exception
    {
        OneOfEverything one = new OneOfEverything();
        testRoundTripSerialize(one, new TCompactProtocol.Factory());
        testRoundTripSerialize(one, new TJSONProtocol.Factory());
    }

    @Test
    public void testDefaultCoercion()
            throws Exception
    {
        CoercionBean coercion = new CoercionBean(
                true,
                (byte) 1,
                (short) 2,
                3,
                4L,
                5.5f,
                6.6d,
                7.7f,
                ImmutableList.of(1.1f, 2.2f, 3.3f));

        testRoundTripSerialize(coercion, new TCompactProtocol.Factory());
    }

    @Test
    public void testIsSetBean()
            throws Exception
    {
        IsSetBean full = IsSetBean.createFull();
        assertAllFieldsSet(full, false);
        // manually set full bean
        full.field = ByteBuffer.wrap("full".getBytes(UTF_8));
        full = testRoundTripSerialize(full, new TCompactProtocol.Factory());
        assertAllFieldsSet(full, true);

        IsSetBean empty = IsSetBean.createEmpty();
        assertAllFieldsSet(empty, false);
        empty = testRoundTripSerialize(empty, new TCompactProtocol.Factory());
        assertAllFieldsSet(empty, false);
    }

    @Test
    public void testBeanGeneric()
            throws Exception
    {
        GenericThriftStructBean<String> bean = new GenericThriftStructBean<>();
        bean.setGenericProperty("genericValue");

        testRoundTripSerialize(new TypeToken<GenericThriftStructBean<String>>() {}, bean, new TCompactProtocol.Factory());
    }

    @Test
    public void testBeanDerivedFromGeneric()
            throws Exception
    {
        ConcreteDerivedFromGenericBean bean = new ConcreteDerivedFromGenericBean();
        bean.setGenericProperty("generic");
        bean.setConcreteField("concrete");

        testRoundTripSerialize(bean, new TCompactProtocol.Factory());
    }

    @Test
    public void testImmutableGeneric()
            throws Exception
    {
        GenericThriftStruct<Double> immutable = new GenericThriftStruct<>(Math.PI);

        testRoundTripSerialize(new TypeToken<GenericThriftStruct<Double>>() {}, immutable, new TCompactProtocol.Factory());
    }

    @Test
    public void testImmutableDerivedFromGeneric()
            throws Exception
    {
        ConcreteDerivedFromGeneric immutable = new ConcreteDerivedFromGeneric(Math.E, Math.PI);

        testRoundTripSerialize(immutable, new TCompactProtocol.Factory());
    }

    @Test
    public void testGenericFromBuilder()
            throws Exception
    {
        GenericThriftStructFromBuilder<Integer, Double> builderObject =
                new GenericThriftStructFromBuilder.Builder<Integer, Double>()
                        .setFirstGenericProperty(12345)
                        .setSecondGenericProperty(1.2345)
                        .build();

        testRoundTripSerialize(
                new TypeToken<GenericThriftStructFromBuilder<Integer, Double>>() {},
                builderObject,
                new TCompactProtocol.Factory());
    }

    @Test
    public void testFieldGeneric()
            throws Exception
    {
        GenericThriftStructField<Integer> fieldObject = new GenericThriftStructField<>();
        fieldObject.genericField = 5757;

        testRoundTripSerialize(
                new TypeToken<GenericThriftStructField<Integer>>() {},
                fieldObject,
                new TCompactProtocol.Factory());
    }

    @Test
    public void testFieldDerivedFromGeneric()
            throws Exception
    {
        ConcreteThriftStructDerivedFromGenericField fieldObject = new ConcreteThriftStructDerivedFromGenericField();
        fieldObject.genericField = "genericValue";
        fieldObject.concreteField = "concreteValue";

        testRoundTripSerialize(fieldObject, new TCompactProtocol.Factory());
    }

    private void assertAllFieldsSet(IsSetBean isSetBean, boolean expected)
    {
        assertEquals(isSetBean.isBooleanSet(), expected);
        assertEquals(isSetBean.isByteSet(), expected);
        assertEquals(isSetBean.isShortSet(), expected);
        assertEquals(isSetBean.isIntegerSet(), expected);
        assertEquals(isSetBean.isLongSet(), expected);
        assertEquals(isSetBean.isDoubleSet(), expected);
        assertEquals(isSetBean.isStringSet(), expected);
        assertEquals(isSetBean.isStructSet(), expected);
        assertEquals(isSetBean.isSetSet(), expected);
        assertEquals(isSetBean.isListSet(), expected);
        assertEquals(isSetBean.isMapSet(), expected);
        assertEquals(!ByteBuffer.wrap("empty".getBytes(UTF_8)).equals(isSetBean.field), expected);
    }

    private <T> T testRoundTripSerialize(T value, TProtocolFactory protocolFactory)
            throws Exception
    {
        ThriftCodec<T> readCodec = (ThriftCodec<T>) readCodecManager.getCodec(value.getClass());
        ThriftCodec<T> writeCodec = (ThriftCodec<T>) writeCodecManager.getCodec(value.getClass());

        return testRoundTripSerialize(readCodec, writeCodec, value, protocolFactory);
    }

    private <T> T testRoundTripSerialize(TypeToken<T> typeToken, T value, TProtocolFactory protocolFactory)
            throws Exception
    {
        ThriftCodec<T> readCodec = (ThriftCodec<T>) readCodecManager.getCodec(typeToken.getType());
        ThriftCodec<T> writeCodec = (ThriftCodec<T>) writeCodecManager.getCodec(typeToken.getType());

        return testRoundTripSerialize(readCodec, writeCodec, typeToken.getType(), value, protocolFactory);
    }

    private <T> T testRoundTripSerialize(ThriftCodec<T> readCodec, ThriftCodec<T> writeCodec, T structInstance, TProtocolFactory protocolFactory)
            throws Exception
    {
        Class<T> structClass = (Class<T>) structInstance.getClass();
        return testRoundTripSerialize(readCodec, writeCodec, structClass, structInstance, protocolFactory);
    }

    private <T> T testRoundTripSerialize(ThriftCodec<T> readCodec, ThriftCodec<T> writeCodec, Type structType, T structInstance, TProtocolFactory protocolFactory)
            throws Exception
    {
        ThriftCatalog readCatalog = readCodecManager.getCatalog();
        ThriftStructMetadata readMetadata = readCatalog.getThriftStructMetadata(structType);
        assertNotNull(readMetadata);

        ThriftCatalog writeCatalog = writeCodecManager.getCatalog();
        ThriftStructMetadata writeMetadata = writeCatalog.getThriftStructMetadata(structType);
        assertNotNull(writeMetadata);

        TMemoryBuffer transport = new TMemoryBuffer(10 * 1024);
        TProtocol protocol = protocolFactory.getProtocol(transport);
        writeCodec.write(structInstance, protocol);

        T copy = readCodec.read(protocol);
        assertNotNull(copy);
        assertEquals(copy, structInstance);

        return copy;
    }

    private OneOfEverything createOneOfEverything()
    {
        OneOfEverything one = new OneOfEverything();
        one.aBoolean = true;
        one.aByte = 11;
        one.aShort = 22;
        one.aInt = 33;
        one.aLong = 44;
        one.aDouble = 55;
        one.aString = "message";
        one.aStruct = new BonkField("struct", 66);
        one.aEnum = Fruit.CHERRY;
        one.aCustomEnum = Letter.C;

        one.aBooleanSet = ImmutableSet.of(true, false);
        one.aByteSet = ImmutableSet.of((byte) -1, (byte) 0, (byte) 1);
        one.aShortSet = ImmutableSet.of((short) -1, (short) 0, (short) 1);
        one.aIntegerSet = ImmutableSet.of(-1, 0, 1);
        one.aLongSet = ImmutableSet.of(-1L, 0L, 1L);
        one.aDoubleSet = ImmutableSet.of(-42.1d, 0.0d, 42.1d);
        one.aStringSet = ImmutableSet.of("a", "string", "set");
        one.aStructSet = ImmutableSet.of(new BonkField("message", 42), new BonkField("other", 11));
        one.aEnumSet = ImmutableSet.copyOf(Fruit.values());
        one.aCustomEnumSet = ImmutableSet.copyOf(Letter.values());

        one.aBooleanList = ImmutableList.of(true, false);
        one.aByteList = ImmutableList.of((byte) -1, (byte) 0, (byte) 1);
        one.aShortList = ImmutableList.of((short) -1, (short) 0, (short) 1);
        one.aIntegerList = ImmutableList.of(-1, 0, 1);
        one.aLongList = ImmutableList.of(-1L, 0L, 1L);
        one.aDoubleList = ImmutableList.of(-42.1d, 0.0d, 42.1d);
        one.aStringList = ImmutableList.of("a", "string", "list");
        one.aStructList = ImmutableList.of(new BonkField("message", 42), new BonkField("other", 11));
        one.aEnumList = ImmutableList.copyOf(Fruit.values());
        one.aCustomEnumList = ImmutableList.copyOf(Letter.values());

        one.aBooleanValueMap = ImmutableMap.of("TRUE", true, "FALSE", false);
        one.aByteValueMap = ImmutableMap.of("-1", (byte) -1, "0", (byte) 0, "1", (byte) 1);
        one.aShortValueMap = ImmutableMap.of("-1", (short) -1, "0", (short) 0, "1", (short) 1);
        one.aIntegerValueMap = ImmutableMap.of("-1", -1, "0", 0, "1", 1);
        one.aLongValueMap = ImmutableMap.of("-1", -1L, "0", 0L, "1", 1L);
        one.aDoubleValueMap = ImmutableMap.of("neg", -42.1d, "0", 0.0d, "pos", 42.1d);
        one.aStringValueMap = ImmutableMap.of("1", "a", "2", "string", "3", "map");
        one.aStructValueMap = ImmutableMap.of("main", new BonkField("message", 42), "other", new BonkField("other", 11));
        one.aEnumValueMap = ImmutableMap.of("apple", Fruit.APPLE, "banana", Fruit.BANANA);
        one.aCustomEnumValueMap = ImmutableMap.of("a", Letter.A, "b", Letter.B);

        one.aBooleanKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aBooleanValueMap).inverse());
        one.aByteKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aByteValueMap).inverse());
        one.aShortKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aShortValueMap).inverse());
        one.aIntegerKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aIntegerValueMap).inverse());
        one.aLongKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aLongValueMap).inverse());
        one.aDoubleKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aDoubleValueMap).inverse());
        one.aStringKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aStringValueMap).inverse());
        one.aStructKeyMap = ImmutableMap.copyOf(HashBiMap.create(one.aStructValueMap).inverse());
        one.aEnumKeyMap = ImmutableMap.of(Fruit.APPLE, "apple", Fruit.BANANA, "banana");
        one.aCustomEnumKeyMap = ImmutableMap.of(Letter.A, "a", Letter.B, "b");

        one.aSetOfListsOfMaps = ImmutableSet.<List<Map<String, BonkField>>>of(
                ImmutableList.<Map<String, BonkField>>of(
                        ImmutableMap.of(
                                "1: main", new BonkField("message", 42),
                                "1: other", new BonkField("other", 11)
                        ),
                        ImmutableMap.of(
                                "1: main", new BonkField("message", 42),
                                "1: other", new BonkField("other", 11)
                        )
                ),
                ImmutableList.<Map<String, BonkField>>of(
                        ImmutableMap.of(
                                "2: main", new BonkField("message", 42),
                                "2: other", new BonkField("other", 11)
                        ),
                        ImmutableMap.of(
                                "2: main", new BonkField("message", 42),
                                "2: other", new BonkField("other", 11)
                        )
                )
        );

        one.aMapOfListToSet = ImmutableMap.<List<String>, Set<BonkField>>of(
                ImmutableList.of("a", "b"),
                ImmutableSet.of(
                        new BonkField("1: message", 42),
                        new BonkField("1: other", 11)
                ),
                ImmutableList.of("c", "d"),
                ImmutableSet.of(
                        new BonkField("2: message", 42),
                        new BonkField("2: other", 11)
                )
        );

        one.aUnion = new UnionField("Hello, World");

        one.aUnionSet = ImmutableSet.of(new UnionField("Hello, World"), new UnionField(123456L), new UnionField(Fruit.CHERRY));
        one.aUnionList = ImmutableList.of(new UnionField("Hello, World"), new UnionField(123456L), new UnionField(Fruit.CHERRY));

        one.aUnionKeyMap = ImmutableMap.of(new UnionField("Hello, World"), "Eins",
                new UnionField(123456L), "Zwei",
                new UnionField(Fruit.CHERRY), "Drei");

        one.aUnionValueMap = ImmutableMap.of("Eins", new UnionField("Hello, World"),
                "Zwei", new UnionField(123456L),
                "Drei", new UnionField(Fruit.CHERRY));

        return one;
    }

    @Test
    public void testSerializeFieldsFromInterface() throws Exception {
        DipSwitch ds = new DipSwitch();
        ds.setName("Dip Switch");
        testRoundTripSerialize(ds, new TCompactProtocol.Factory());
    }

    @Test
    public void testSerializeInterfaceStructWithBuilder() throws Exception {
        BasicComponent component = new BasicComponent("Resistor", "1KOhm", 47.);
        testRoundTripSerialize(component, new TCompactProtocol.Factory());
    }

    @Test
    public void testSerializeImmutableAnonymousInnerStruct() throws Exception {
        final DiscreteComponent comp = new DiscreteComponent() {
            @Override
            public String getName() {
                return "Toroidal Inductor";
            }
            @Override
            public String getUnits() {
                return "Henry";
            }
            @Override
            public Double getValue() {
                return 10.;
            }
            @Override
            public int hashCode() {
                return Utils.hashCode(this);
            }
            @Override
            public boolean equals(Object obj) {
                return Utils.areEqual(this, obj);
            }
            @Override
            public String toString() {
                return Utils.toString(this);
            }
            @Override
            public void setUnits(String units) {
                throw new UnsupportedOperationException();
            }
            @Override
            public void setValue(Double value) {
                throw new UnsupportedOperationException();
            }
        };
        testRoundTripSerialize(comp, new TCompactProtocol.Factory());
    }

}
