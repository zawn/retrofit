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

import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public final class CallCacheControlTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Call<String> getString();
  }

  @Test public void callWitCacheControlTest() throws IOException {
    final Request request = buildRequest(Service.class, CacheControl.FORCE_CACHE);
    assertThat(request.headers().get("Cache-Control")).isEqualTo(CacheControl.FORCE_CACHE.toString
        ());
  }

  @Test public void callOverrideMethodCacheControlTest() throws IOException {
    class Example {

      @GET("/")
      @Headers("Cache-Control: text/not-plain") //
      Call<ResponseBody> method() {
        return null;
      }
    }
    final Request request = buildRequest(Example.class, CacheControl.FORCE_CACHE);
    assertThat(request.headers().get("Cache-Control")).isEqualTo(CacheControl.FORCE_CACHE.toString
        ());
  }

  static Request buildRequest(Class<?> cls, CacheControl cacheControl, Object... args) {
    final AtomicReference<Request> requestRef = new AtomicReference<>();
    okhttp3.Call.Factory callFactory = new okhttp3.Call.Factory() {
      @Override public okhttp3.Call newCall(Request request) {
        requestRef.set(request);
        throw new UnsupportedOperationException("Not implemented");
      }
    };

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(new ToStringConverterFactory())
        .callFactory(callFactory)
        .build();

    Method method = TestingUtils.onlyMethod(cls);
    ServiceMethod<?> serviceMethod = retrofit.loadServiceMethod(method);
    OkHttpCall<?> okHttpCall = new OkHttpCall<>(serviceMethod, args);
    Call<?> call = (Call<?>) serviceMethod.callAdapter.adapt(okHttpCall);
    try {
      call.execute(cacheControl);
      throw new AssertionError();
    } catch (UnsupportedOperationException ignored) {
      return requestRef.get();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
