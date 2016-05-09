package retrofit2;

import retrofit2.http.ParamHeaders;
import retrofit2.http.ParamQuerys;
import retrofit2.http.ParamUrl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import static retrofit2.Utils.typeError;

/**
 * Created by ZhangZhenli on 2015/11/26.
 */
public class ServiceParser {

  static ParameterHandler[] parseClassAnnotations(Class service, Retrofit retrofit) {
    ArrayList<ParameterHandler> requestActions = new ArrayList<>();
    Annotation[] annotations = service.getAnnotations();
    for (Annotation annotation : annotations) {
      ParameterHandler<?> action;
      if (annotation instanceof ParamHeaders) {
        ParamHeaders headers = (ParamHeaders) annotation;
        String[] headerStrings = headers.value();
        if (headerStrings.length == 0) {
          throw typeError(service, "@Headers annotation is empty.");
        }
        for (int i = 0; i < headerStrings.length; i++) {
          String name = headerStrings[i];
          Converter<?, String> valueConverter =
              retrofit.stringConverter(String.class, new Annotation[]{annotation});
          action = new ParameterHandler.ParamHeader<>(name, valueConverter);
          requestActions.add(action);
        }
      } else if (annotation instanceof ParamQuerys) {
        ParamQuerys querys = (ParamQuerys) annotation;
        String[] queryStrings = querys.value();
        if (queryStrings.length == 0) {
          throw typeError(service, "@ParamQuerys annotation is empty.");
        }
        for (int i = 0; i < queryStrings.length; i++) {
          String name = queryStrings[i];
          Converter<?, String> valueConverter =
              retrofit.stringConverter(String.class, new Annotation[]{annotation});
          action = new ParameterHandler.ParamQuery<>(name, valueConverter, querys.encoded());
          requestActions.add(action);
        }
      } else if (annotation instanceof ParamUrl) {
        ParamUrl baseUrl = (ParamUrl) annotation;
        String value = baseUrl.value();
        action = new ParameterHandler.ParamUrl<>(value);
        requestActions.add(action);
      }
    }
    return requestActions.toArray(new ParameterHandler[requestActions.size()]);
  }
}
