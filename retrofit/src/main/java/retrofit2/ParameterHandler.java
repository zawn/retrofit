/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static retrofit2.Utils.checkNotNull;

abstract class ParameterHandler<T> {
  abstract void apply(RequestBuilder builder, T value) throws IOException;

  final ParameterHandler<Iterable<T>> iterable() {
    return new ParameterHandler<Iterable<T>>() {
      @Override void apply(RequestBuilder builder, Iterable<T> values) throws IOException {
        if (values == null) return; // Skip null values.

        for (T value : values) {
          ParameterHandler.this.apply(builder, value);
        }
      }
    };
  }

  final ParameterHandler<Object> array() {
    return new ParameterHandler<Object>() {
      @Override void apply(RequestBuilder builder, Object values) throws IOException {
        if (values == null) return; // Skip null values.

        for (int i = 0, size = Array.getLength(values); i < size; i++) {
          //noinspection unchecked
          ParameterHandler.this.apply(builder, (T) Array.get(values, i));
        }
      }
    };
  }

  static final class RelativeUrl extends ParameterHandler<Object> {
    @Override void apply(RequestBuilder builder, Object value) {
      builder.setRelativeUrl(value);
    }
  }

  static final class Header<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;

    Header(String name, Converter<T, String> valueConverter) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addHeader(name, valueConverter.convert(value));
    }
  }

  static final class Path<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Path(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException(
            "Path parameter \"" + name + "\" value must not be null.");
      }
      builder.addPathParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class Query<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Query(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addQueryParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class QueryMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    QueryMap(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Query map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Query map contained null value for key '" + entryKey + "'.");
        }
        builder.addQueryParam(entryKey, valueConverter.convert(entryValue), encoded);
      }
    }
  }

  static final class HeaderMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;

    HeaderMap(Converter<T, String> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Header map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String headerName = entry.getKey();
        if (headerName == null) {
          throw new IllegalArgumentException("Header map contained null key.");
        }
        T headerValue = entry.getValue();
        if (headerValue == null) {
          throw new IllegalArgumentException(
              "Header map contained null value for key '" + headerName + "'.");
        }
        builder.addHeader(headerName, valueConverter.convert(headerValue));
      }
    }
  }

  static final class Field<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Field(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addFormField(name, valueConverter.convert(value), encoded);
    }
  }

  static final class FieldMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    FieldMap(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Field map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Field map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Field map contained null value for key '" + entryKey + "'.");
        }
        builder.addFormField(entryKey, valueConverter.convert(entryValue), encoded);
      }
    }
  }

  static final class Part<T> extends ParameterHandler<T> {
    private final Headers headers;
    private final Converter<T, RequestBody> converter;

    Part(Headers headers, Converter<T, RequestBody> converter) {
      this.headers = headers;
      this.converter = converter;
    }

    @Override void apply(RequestBuilder builder, T value) {
      if (value == null) return; // Skip null values.

      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.addPart(headers, body);
    }
  }

  static final class RawPart extends ParameterHandler<MultipartBody.Part> {
    static final RawPart INSTANCE = new RawPart();

    private RawPart() {
    }

    @Override void apply(RequestBuilder builder, MultipartBody.Part value) throws IOException {
      if (value != null) { // Skip null values.
        builder.addPart(value);
      }
    }
  }

  static final class PartMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, RequestBody> valueConverter;
    private final String transferEncoding;

    PartMap(Converter<T, RequestBody> valueConverter, String transferEncoding) {
      this.valueConverter = valueConverter;
      this.transferEncoding = transferEncoding;
    }

    @Override void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Part map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Part map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Part map contained null value for key '" + entryKey + "'.");
        }

        Headers headers = Headers.of(
            "Content-Disposition", "form-data; name=\"" + entryKey + "\"",
            "Content-Transfer-Encoding", transferEncoding);

        builder.addPart(headers, valueConverter.convert(entryValue));
      }
    }
  }

  static final class Body<T> extends ParameterHandler<T> {
    private final Converter<T, RequestBody> converter;

    Body(Converter<T, RequestBody> converter) {
      this.converter = converter;
    }

    @Override void apply(RequestBuilder builder, T value) {
      if (value == null) {
        throw new IllegalArgumentException("Body parameter value must not be null.");
      }
      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.setBody(body);
    }
  }

  static final class ParamQuery<T> extends ParameterHandler<T> {
    public final String key;
    private final String name;
    public final String value;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    ParamQuery(String query, Converter<T, String> valueConverter, boolean encoded) {
      String[] split = query.split("=");
      if (split.length != 2) {
        throw new IllegalArgumentException("@ParamQuerys Configuration errors,at " + query);
      }
      this.name = checkNotNull(split[0], "name == null");
      this.value = checkNotNull(split[1], "query value null");
      Set<String> set = ServiceMethod.parsePathParameters(this.value);
      if (set.size() > 1) {
        throw new IllegalArgumentException("@ParamQuerys Configuration errors,at "
            + name
            + ", You can only "
            + "have a maximum of one parameter");
      } else if (set.size() == 1) {
        Iterator<String> iterator = set.iterator();
        key = iterator.next();
      } else {
        key = null;
      }
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      String rValue;
      String resultValue = this.value;
      if (key != null) {
        rValue = valueConverter.convert(value);
        if (rValue != null && !"".equals(rValue)) {
          resultValue = this.value.replace("{" + key + "}", rValue);
        }
      }
      if (resultValue != null && !"".equals(resultValue)) {
        builder.addQueryParam(name, resultValue, encoded);
      }
    }
  }

  static final class ParamHeader<T> extends ParameterHandler<T> {
    private static final String UTF_8 = "utf-8";
    public final String key;
    private final String name;
    public final String value;
    private final Converter<T, String> valueConverter;

    ParamHeader(String header, Converter<T, String> valueConverter) {
      int index = header.indexOf(":");
      this.name = checkNotNull(header.substring(0, index), "name == null");
      this.value = checkNotNull(header.substring(index + 1), "query value null");
      Set<String> set = ServiceMethod.parseHeaderParameters(this.value);
      if (set.size() > 1) {
        throw new IllegalArgumentException("@ParamHeader Configuration errors,at "
            + name
            + ", You can only have"
            + " a maximum of one parameter");
      } else if (set.size() == 1) {
        Iterator<String> iterator = set.iterator();
        key = iterator.next();
      } else {
        key = null;
      }
      this.valueConverter = valueConverter;
    }

    @Override
    void apply(RequestBuilder builder, T value) throws IOException {
      String rValue;
      String resultValue = this.value;
      if (key != null) {
        rValue = extactValid(valueConverter.convert(value));
        if (rValue != null && !"".equals(rValue)) {
          resultValue = this.value.replace("{" + key + "}", rValue);
        }
      }
      if (resultValue != null && !"".equals(resultValue)) {
        builder.addHeader(name, resultValue);
      }
    }

    String extactValid(String value) {
      if (value == null || "".equals(value)) {
        return null;
      }

      String result = null;
      StringBuffer buffer = new StringBuffer("");
      if (value.indexOf(";") != -1) { // 组合Value
        String[] strs = value.split(";");
        if (null != strs && strs.length > 0) {
          for (String str : strs) {
            if (str.indexOf("=") != -1 && str.lastIndexOf("=") != str.length() - 1) {
              String key = str.substring(0, str.indexOf("="));
              String val = str.substring(str.indexOf("=") + 1);
              if (key != null && !"".equals(key) && val != null && !"".equals(val)) {
                buffer.append(encode(key, UTF_8)).append("=").append(encode(val, UTF_8)
                ).append(";");
              }
            }
          }
          if (buffer.length() > 0 && buffer.toString().endsWith(";")) {
            result = buffer.deleteCharAt(buffer.length() - 1).toString();
          }
        }
        return result;
      } else { // 单个Value
        return encode(value, UTF_8);
      }
    }

    String encode(String content, String charset) {
      if (content == null || "".equals(content.trim())) {
        return null;
      }
      try {
        return URLEncoder.encode(content,
            charset != null ? charset : UTF_8);
      } catch (UnsupportedEncodingException ex) {
        return null;
      }
    }
  }

  public static class ParamUrl<T> extends ParameterHandler<T> {

    public final String url;

    public final String key;

    public ParamUrl(String url) {
      this.url = url;
      Set<String> set = ServiceMethod.parsePathParameters(this.url);
      if (set.size() > 1) {
        throw new IllegalArgumentException("@ParamQuerys Configuration errors,at "
            + url
            + ", You can only have"
            + " a maximum of one parameter");
      } else if (set.size() == 1) {
        Iterator<String> iterator = set.iterator();
        key = iterator.next();
      } else {
        key = null;
      }
    }

    @Override void apply(RequestBuilder builder, T value) throws IOException {
      String resultValue = this.url;
      if (url != null) {
        resultValue = this.url.replace("{" + key + "}", value == null ? "" : value.toString());
      }
      builder.setServiceUrl(resultValue);
    }
  }
}
