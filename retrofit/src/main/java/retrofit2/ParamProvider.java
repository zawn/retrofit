package retrofit2;

/**
 * Created by ZhangZhenli on 2015/11/25.
 */
public interface ParamProvider {

  Object getHeaderParam(String paramName);

  Object getUrlParam(String paramName);

  Object getQueryParam(String paramName);
}
