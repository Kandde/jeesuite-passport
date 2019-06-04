package com.jeesuite.passport.component.snslogin.connector;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.passport.component.snslogin.OauthConnector;
import com.jeesuite.passport.component.snslogin.OauthUser;

public class OSChinaConnector extends OauthConnector {

	public static final String SNS_TYPE = "osc";
	
	public OSChinaConnector(String appkey, String appSecret) {
		super(appkey, appSecret);
	}

	/**
	 * doc: http://www.oschina.net/openapi/docs/oauth2_authorize
	 */
	public String createAuthorizeUrl(String state) {

		StringBuilder urlBuilder = new StringBuilder("http://www.oschina.net/action/oauth2/authorize?");
		urlBuilder.append("response_type=code");
		urlBuilder.append("&client_id=" + getClientId());
		urlBuilder.append("&redirect_uri=" + getRedirectUri());
		urlBuilder.append("&state=" + state);

		return urlBuilder.toString();
	}

	protected String getAccessToken(String code) {

		StringBuilder urlBuilder = new StringBuilder("http://www.oschina.net/action/openapi/token?");
		urlBuilder.append("grant_type=authorization_code");
		urlBuilder.append("&dataType=json");
		urlBuilder.append("&client_id=" + getClientId());
		urlBuilder.append("&client_secret=" + getClientSecret());
		urlBuilder.append("&redirect_uri=" + getRedirectUri());
		urlBuilder.append("&code=" + code);

		String url = urlBuilder.toString();

		String httpString = httpGet(url);
		if (StringUtils.isBlank(httpString)) {
			return null;
		}

		try {			
			JSONObject json = JSONObject.parseObject(httpString);
			return json.getString("access_token");
		} catch (Exception e) {
			System.out.println(httpString);
			throw new JeesuiteBaseException(501, "第三方平台返回异常");
		}
	}

	@Override
	protected OauthUser getOauthUser(String code) {

		String accessToken = getAccessToken(code);

		String url = "http://www.oschina.net/action/openapi/user?access_token=" + accessToken + "&dataType=json";

		String httpString = httpGet(url);
		JSONObject json = JSONObject.parseObject(httpString);

		if(json.containsKey("error_description")){
			throw new JeesuiteBaseException(1001, json.getString("error_description"));
		}
		OauthUser user = new OauthUser();
		user.setAvatar(json.getString("avatar"));
		user.setOpenId(json.getString("id"));
		user.setNickname(json.getString("name"));
		user.setGender(json.getString("gender"));
		return user;
	}
	
	@Override
	public String snsType() {
		return SNS_TYPE;
	}

}
