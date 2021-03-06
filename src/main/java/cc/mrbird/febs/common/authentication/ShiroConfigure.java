package cc.mrbird.febs.common.authentication;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import cc.mrbird.febs.common.annotation.ConditionOnRedisCache;
import cc.mrbird.febs.common.entity.Strings;
import cc.mrbird.febs.common.properties.FebsProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.LogoutAware;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.MemorySessionDAO;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Shiro 配置类
 *
 * @author MrBird
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class ShiroConfigure {

    private final FebsProperties febsProperties;
    private RedisProperties redisProperties;

    @Autowired(required = false)
    public void setRedisProperties(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    /**
     * shiro 中配置 redis 缓存
     *
     * @return RedisManager
     */
    private RedisManager redisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setHost(redisProperties.getHost() + Strings.COLON + redisProperties.getPort());
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            redisManager.setPassword(redisProperties.getPassword());
        }
        redisManager.setTimeout(redisManager.getTimeout());
        redisManager.setDatabase(redisProperties.getDatabase());
        return redisManager;
    }

    @Bean
    @ConditionOnRedisCache
    public RedisCacheManager redisCacheManager() {
        RedisCacheManager redisCacheManager = new RedisCacheManager();
        // 权限缓存超时时间，和session超时时间一致
        redisCacheManager.setExpire((int) febsProperties.getShiro().getSessionTimeout().getSeconds());
        redisCacheManager.setRedisManager(redisManager());
        return redisCacheManager;
    }

    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public EhCacheManager ehCacheManager() {
        EhCacheManager ehCacheManager = new EhCacheManager();
        ehCacheManager.setCacheManagerConfigFile("classpath:shiro-ehcache.xml");
        return ehCacheManager;
    }

    @Bean
    public DefaultWebSecurityManager securityManager(ShiroRealm shiroRealm,
                                                     @Nullable RedisCacheManager redisCacheManager,
                                                     @Nullable EhCacheManager ehCacheManager,
                                                     DefaultWebSessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // 配置 SecurityManager，并注入 shiroRealm
        securityManager.setRealm(shiroRealm);
        // 配置 shiro session管理器
        securityManager.setSessionManager(sessionManager);
        // 配置 缓存管理类 cacheManager
        securityManager.setCacheManager(redisCacheManager != null ? redisCacheManager : ehCacheManager);
        // 配置 rememberMeCookie
        securityManager.setRememberMeManager(rememberMeManager());
        return securityManager;
    }

    /**
     * rememberMe cookie 效果是重开浏览器后无需重新登录
     *
     * @return SimpleCookie
     */
    private SimpleCookie rememberMeCookie() {
        // 设置 cookie 名称，对应 login.html 页面的 <input type="checkbox" name="rememberMe"/>
        SimpleCookie cookie = new SimpleCookie("rememberMe");
        // 设置 cookie 的过期时间，单位为秒
        cookie.setMaxAge((int) febsProperties.getShiro().getCookieTimeout().getSeconds());
        return cookie;
    }

    /**
     * cookie管理对象
     *
     * @return CookieRememberMeManager
     */
    private CookieRememberMeManager rememberMeManager() {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        cookieRememberMeManager.setCookie(rememberMeCookie());
        // rememberMe cookie 加密的密钥
        String encryptKey = RandomStringUtils.randomAlphanumeric(15);
        byte[] encryptKeyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        String rememberKey = Base64Utils.encodeToString(Arrays.copyOf(encryptKeyBytes, 16));
        cookieRememberMeManager.setCipherKey(Base64.decode(rememberKey));
        return cookieRememberMeManager;
    }

    /**
     * 用于开启 Thymeleaf 中的 shiro 标签的使用
     *
     * @return ShiroDialect shiro 方言对象
     */
    @Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }

    @Bean(name = "redisSessionDAO")
    @ConditionOnRedisCache
    public RedisSessionDAO redisSessionDAO() {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager());
        return redisSessionDAO;
    }

    @Bean
    @ConditionalOnMissingBean(RedisSessionDAO.class)
    public MemorySessionDAO memorySessionDAO() {
        return new MemorySessionDAO();
    }

    /**
     * session 管理对象
     *
     * @return DefaultWebSessionManager
     */
    @Bean
    public DefaultWebSessionManager sessionManager(@Nullable RedisSessionDAO redisSessionDAO,
                                                   @Nullable MemorySessionDAO memorySessionDAO) {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        // 设置 session超时时间
        sessionManager.setGlobalSessionTimeout(febsProperties.getShiro().getSessionTimeout().toMillis());
        sessionManager.setSessionDAO(redisSessionDAO == null ? memorySessionDAO : redisSessionDAO);
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        return sessionManager;
    }

    //@Bean
    //public ShiroRealm customerRealm() {
    //    ShiroRealm shiroRealm = new ShiroRealm();
    //    ShiroRealm customerRealm = new ShiroRealm();
    //    /**
    //     * 密文匹配的时候，这里需要设置credentialsMatcher()，否则无法匹配
    //     */
    //    credentialsMatcher();
    //    customerRealm.setCredentialsMatcher(credentialsMatcher());
    //    return customerRealm;
    //}

    //@Bean
    //public HashedCredentialsMatcher credentialsMatcher() {
    //    HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
    //    credentialsMatcher.setHashAlgorithmName("md5");
    //    credentialsMatcher.setHashIterations(2);
    //    // true 密码加密用hex编码; false 用base64编码
    //    credentialsMatcher.setStoredCredentialsHexEncoded(true);
    //    return credentialsMatcher;
    //}

}

