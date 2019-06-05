/**
 * 
 */
package com.jeesuite.passport.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.common.util.FormatValidateUtils;
import com.jeesuite.passport.component.snslogin.OauthUser;
import com.jeesuite.passport.component.snslogin.connector.WeixinGzhConnector;
import com.jeesuite.passport.dao.entity.SnsAccountBindingEntity;
import com.jeesuite.passport.dao.entity.SnsAccountBindingEntity.SnsType;
import com.jeesuite.passport.dao.entity.UserEntity;
import com.jeesuite.passport.dao.mapper.SnsAccountBindingEntityMapper;
import com.jeesuite.passport.dao.mapper.UserEntityMapper;
import com.jeesuite.passport.dto.AccountBindParam;
import com.jeesuite.passport.dto.RequestMetadata;
import com.jeesuite.passport.dto.UserInfo;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年3月19日
 */
@Service
public class UserService {

	
	@Autowired
	private UserEntityMapper userMapper;
	@Autowired
	private SnsAccountBindingEntityMapper snsAccounyBindingMapper;


	public UserInfo findAcctountById(Integer id) {
		UserEntity entity = userMapper.selectByPrimaryKey(id);
		return buildUserInfo(entity);
	}
	
	public UserInfo findByWxUnionId(String unionId) {
		UserEntity entity = userMapper.findByWxUnionId(unionId);
		return buildUserInfo(entity);
	}
	
	public UserInfo findAcctountByLoginName(String loginName) {
		UserEntity entity = userMapper.findByLoginName(loginName);
		return buildUserInfo(entity);
	}

	private UserInfo buildUserInfo(UserEntity entity) {
		UserInfo userInfo = new UserInfo();
		userInfo.setId(entity.getId());
		userInfo.setUserName(entity.getUsername());
		userInfo.setNickname(entity.getNickname());
		userInfo.setRealname(entity.getRealname());
		userInfo.setMobile(entity.getMobile());
		userInfo.setEmail(entity.getEmail());
		userInfo.setPassword(entity.getPassword());
		userInfo.setAge(entity.getAge());
		userInfo.setGender(entity.getGender());
		userInfo.setAvatar(entity.getAvatar());
		return userInfo;
	}
	
	public UserInfo findAcctountBySnsOpenId(String type,String openId) {
		type = WeixinGzhConnector.SNS_TYPE.equals(type) ? SnsType.weixin.name() : type;
		SnsAccountBindingEntity bindingEntity = snsAccounyBindingMapper.findBySnsOpenId(type, openId);
		if(bindingEntity != null){
			UserEntity accountEntity = userMapper.selectByPrimaryKey(bindingEntity.getUserId());
			return buildUserInfo(accountEntity);
		}
		return null;
	}
	
	public UserInfo checkAndGetAccount(String loginName,String password){
		UserEntity entity = userMapper.findByLoginName(loginName);
		if(entity == null || !entity.getPassword().equals(UserEntity.encryptPassword(password))){
		   return null;
		}
		return buildUserInfo(entity);
	}
	
	@Transactional
	public UserInfo createUser(UserInfo userInfo,RequestMetadata metadata){
		UserEntity accountEntity = null;
		if(FormatValidateUtils.isMobile(userInfo.getMobile())){
			accountEntity = userMapper.findByMobile(userInfo.getMobile());
			if(accountEntity != null){
				throw new JeesuiteBaseException(4001, "该手机已注册");
			}
		}else{
			throw new JeesuiteBaseException(4003, "手机号不能为空");
		}
		
		if(StringUtils.isNotBlank(userInfo.getEmail())){
			accountEntity = userMapper.findByLoginName(userInfo.getEmail());
			if(accountEntity != null){
				throw new JeesuiteBaseException(4003, "该邮箱已注册");
			}
		}
		
		if(StringUtils.isNotBlank(userInfo.getUserName())){
			accountEntity = userMapper.findByLoginName(userInfo.getUserName());
			if(accountEntity != null){
				throw new JeesuiteBaseException(4003, "该用户名已注册");
			}
		}
		
		accountEntity = BeanUtils.copy(userInfo, UserEntity.class);
		accountEntity.setRegAt(metadata.getTime());
		accountEntity.setRegIp(metadata.getIpAddr());
		accountEntity.setSourceAppId(metadata.getAppId());
		if(StringUtils.isNotBlank(accountEntity.getPassword())){
			accountEntity.setPassword(UserEntity.encryptPassword(accountEntity.getPassword()));
		}
		userMapper.insertSelective(accountEntity);
		
		userInfo.setId(accountEntity.getId());
		return userInfo;
	}
	
	@Transactional
	public UserInfo createUserByOauthInfo(OauthUser oauthUser,AccountBindParam bindParam){
		
		String snsType = oauthUser.getSnsType();
		String subSnsType = null;
		if(WeixinGzhConnector.SNS_TYPE.equals(oauthUser.getSnsType())){
			snsType = SnsType.weixin.name();
			subSnsType = oauthUser.getSnsType();
		}
		UserInfo account = findAcctountBySnsOpenId(snsType, oauthUser.getOpenId());
		if(account == null){
			
			UserEntity accountEntity = null;
			//先按unionId查找是否有已存在的用户
			if(StringUtils.isNotBlank(oauthUser.getUnionId())){
				List<SnsAccountBindingEntity> sameAccounyBinds = snsAccounyBindingMapper.findByUnionId(oauthUser.getUnionId());
				if(sameAccounyBinds != null && sameAccounyBinds.size() > 0){
					accountEntity = userMapper.selectByPrimaryKey(sameAccounyBinds.get(0).getUserId());
					if(accountEntity == null){
						throw new JeesuiteBaseException(501, String.format("该账号绑定用户异常，UserId:%s", sameAccounyBinds.get(0).getUserId()));
					}
				}
			}
			
			if(accountEntity == null){
				if(bindParam == null){					
					accountEntity = new UserEntity();
					accountEntity.setAvatar(oauthUser.getAvatar());
					accountEntity.setNickname(oauthUser.getNickname());
					accountEntity.setGender(oauthUser.getGender());
				}else{
					accountEntity = BeanUtils.copy(bindParam, UserEntity.class);
				}
				accountEntity.setSourceAppId(bindParam.getAppId());
				accountEntity.setRegIp(bindParam.getIpAddr());
				accountEntity.setRegAt(bindParam.getTime());
				if(StringUtils.isNotBlank(accountEntity.getPassword())){
					accountEntity.setPassword(UserEntity.encryptPassword(accountEntity.getPassword()));
				}
				userMapper.insertSelective(accountEntity);
			}
			//
			SnsAccountBindingEntity bindingEntity = new SnsAccountBindingEntity();
			bindingEntity.setUserId(accountEntity.getId().intValue());
			bindingEntity.setSnsType(snsType);
			bindingEntity.setSubSnsType(subSnsType);
			bindingEntity.setOpenId(oauthUser.getOpenId());
			bindingEntity.setUnionId(oauthUser.getUnionId());
			bindingEntity.setEnabled(true);
			bindingEntity.setCreatedAt(bindParam.getTime());
			snsAccounyBindingMapper.insertSelective(bindingEntity);
			
			account = buildUserInfo(accountEntity);
			
		}
		return account;
	}
	
	@Transactional
	public void updateAccount(UserInfo userInfo){
		
		UserEntity accountEntity = userMapper.selectByPrimaryKey(userInfo.getId());
		if(accountEntity == null)throw new JeesuiteBaseException(4001, "账号不存在");
		
		UserEntity existAccount = null;
		if(StringUtils.isNotBlank(userInfo.getMobile()) && !userInfo.getMobile().equals(accountEntity.getMobile())){
			existAccount = userMapper.findByMobile(userInfo.getMobile());
			if(existAccount != null){
				throw new JeesuiteBaseException(4003, "该手机号码已注册");
			}
			accountEntity.setMobile(userInfo.getMobile());
		}
		if(StringUtils.isNotBlank(userInfo.getEmail()) && !userInfo.getEmail().equals(accountEntity.getEmail())){
			existAccount = userMapper.findByLoginName(userInfo.getEmail());
			if(existAccount != null){
				throw new JeesuiteBaseException(4003, "该邮箱已注册");
			}
			accountEntity.setEmail(userInfo.getEmail());
		}
		if(StringUtils.isNotBlank(userInfo.getPassword())){
			accountEntity.setPassword(UserEntity.encryptPassword(userInfo.getPassword()));
		}
		if(StringUtils.isNotBlank(userInfo.getAvatar())){
			accountEntity.setAvatar(userInfo.getAvatar());
		}
        if(StringUtils.isNotBlank(userInfo.getGender())){
        	accountEntity.setGender(userInfo.getGender());
		}
        if(StringUtils.isNotBlank(userInfo.getNickname())){
        	accountEntity.setNickname(userInfo.getNickname());
		}
        if(StringUtils.isNotBlank(userInfo.getRealname())){
        	accountEntity.setRealname(userInfo.getRealname());
		} 
        if(userInfo.getAge() != null){
        	accountEntity.setAge(userInfo.getAge());
		}
        if(userInfo.getBirthday() != null){
        	accountEntity.setBirthday(userInfo.getBirthday());
        }
		accountEntity.setUpdatedAt(new Date());
		
		userMapper.updateByPrimaryKeySelective(accountEntity);
	}

	public void addSnsAccountBind(int userId,OauthUser oauthUser){
		SnsAccountBindingEntity bindingEntity = snsAccounyBindingMapper.findBySnsOpenId(oauthUser.getSnsType(), oauthUser.getOpenId());
		if(bindingEntity != null){
			if(bindingEntity.getUserId().intValue() == userId){
				if(!bindingEntity.getEnabled()){
					bindingEntity.setEnabled(true);
					bindingEntity.setUpdatedAt(new Date());
					snsAccounyBindingMapper.updateByPrimaryKeySelective(bindingEntity);
				}
				return;
			}
			throw new JeesuiteBaseException(4005, "该账号已经绑定其他账号");
		}
		
		bindingEntity = new SnsAccountBindingEntity();
		bindingEntity.setUserId(userId);
		bindingEntity.setSnsType(oauthUser.getSnsType());
		bindingEntity.setOpenId(oauthUser.getOpenId());
		bindingEntity.setUnionId(oauthUser.getUnionId());
		bindingEntity.setEnabled(true);
		bindingEntity.setCreatedAt(new Date());
		snsAccounyBindingMapper.insertSelective(bindingEntity);
		
	}
	
    public void cancelSnsAccountBind(int userId,String snsType){
    	snsAccounyBindingMapper.unbindSnsAccount(userId, snsType);
	}
}
