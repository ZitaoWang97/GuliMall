package com.zitao.gulimall.member.service.impl;

import com.zitao.gulimall.member.entity.MemberLevelEntity;
import com.zitao.gulimall.member.exception.PhoneNumExistException;
import com.zitao.gulimall.member.exception.UserExistException;
import com.zitao.gulimall.member.service.MemberLevelService;
import com.zitao.gulimall.member.vo.MemberLoginVo;
import com.zitao.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.member.dao.MemberDao;
import com.zitao.gulimall.member.entity.MemberEntity;
import com.zitao.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册新用户
     *
     * @param registerVo
     */
    @Override
    public void register(MemberRegisterVo registerVo) {
        // 1. 检查电话号是否唯一
        checkPhoneUnique(registerVo.getPhone());
        // 2. 检查用户名是否唯一
        checkUserNameUnique(registerVo.getUserName());
        // 3. 该用户信息唯一，进行插入
        MemberEntity entity = new MemberEntity();
        // 3.1 保存基本信息
        entity.setUsername(registerVo.getUserName());
        entity.setMobile(registerVo.getPhone());
        entity.setCreateTime(new Date());
        // 3.2 使用加密保存密码
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodePassword = passwordEncoder.encode(registerVo.getPassword());
        entity.setPassword(encodePassword);
        // 3.3 设置会员默认等级
        // 3.3.1 找到会员默认等级
        MemberLevelEntity defaultLevel = memberLevelService.getOne(
                new QueryWrapper<MemberLevelEntity>().eq("default_status", 1));
        // 3.3.2 设置会员等级为默认
        entity.setLevelId(defaultLevel.getId());
        // 4. 保存用户信息
        this.save(entity);
    }

    /**
     * 用户登陆 确认用户名与密码是否匹配数据库
     *
     * @param loginVo
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo loginVo) {
        String loginAccount = loginVo.getLoginAccount();
        // 以用户名或电话号登录的进行查询
        MemberEntity entity = this.getOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginAccount)
                .or().eq("mobile", loginAccount));
        if (entity != null) {
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            // 密码是否匹配 matches(页面传来的真实的密码, 数据库加密后的密码)
            boolean matches = bCryptPasswordEncoder.matches(loginVo.getPassword(), entity.getPassword());
            if (matches) {
                entity.setPassword("");
                return entity;
            }
        }
        return null;
    }

    /**
     * 异常机制 如果手机号或者用户名不唯一 则向上抛异常让controller去感知
     *
     * @param phone
     */
    private void checkPhoneUnique(String phone) {
        // 统计计数方法
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneNumExistException();
        }
    }

    private void checkUserNameUnique(String userName) {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UserExistException();
        }
    }


}