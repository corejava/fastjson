/*
 * Copyright 1999-2101 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.util.TypeUtils;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public class CollectionCodec implements ObjectSerializer, ObjectDeserializer {

    public final static CollectionCodec instance = new CollectionCodec();
    
    private CollectionCodec() {
        
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType) throws IOException {
        SerializeWriter out = serializer.out;

        if (object == null) {
            if ((out.features & SerializerFeature.WriteNullListAsEmpty.mask) != 0) {
                out.write("[]");
            } else {
                out.writeNull();
            }
            return;
        }

        Type elementType = null;
        if ((out.features & SerializerFeature.WriteClassName.mask) != 0) {
            if (fieldType instanceof ParameterizedType) {
                ParameterizedType param = (ParameterizedType) fieldType;
                elementType = param.getActualTypeArguments()[0];
            }
        }

        Collection<?> collection = (Collection<?>) object;

        SerialContext context = serializer.context;
        serializer.setContext(context, object, fieldName, 0);

        if ((out.features & SerializerFeature.WriteClassName.mask) != 0) {
            if (HashSet.class == collection.getClass()) {
                out.append("Set");
            } else if (TreeSet.class == collection.getClass()) {
                out.append("TreeSet");
            }
        }

        try {
            int i = 0;
            out.write('[');
            for (Object item : collection) {

                if (i++ != 0) {
                    out.write(',');
                }

                if (item == null) {
                    out.writeNull();
                    continue;
                }

                Class<?> clazz = item.getClass();

                if (clazz == Integer.class) {
                    out.writeInt(((Integer) item).intValue());
                    continue;
                }

                if (clazz == Long.class) {
                    out.writeLong(((Long) item).longValue());

                    if ((out.features & SerializerFeature.WriteClassName.mask) != 0) {
                        out.write('L');
                    }
                    continue;
                }

                ObjectSerializer itemSerializer = serializer.config.get(clazz);
                itemSerializer.write(serializer, item, i - 1, elementType);
            }
            out.write(']');
        } finally {
            serializer.context = context;
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        if (parser.lexer.token() == JSONToken.NULL) {
            parser.lexer.nextToken(JSONToken.COMMA);
            return null;
        }
        
        if (type == JSONArray.class) {
            JSONArray array = new JSONArray();
            parser.parseArray(array);
            return (T) array;
        }

        Collection list = TypeUtils.createCollection(type);

        Type itemType;
        if (type instanceof ParameterizedType) {
            itemType = ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            itemType = Object.class;
        }
        parser.parseArray(itemType, list, fieldName);

        return (T) list;
    }

    
}
