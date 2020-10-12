package org.cboard.config;

import org.cboard.security.service.ShareAuthenticationProviderDecorator;
import org.cboard.security.service.UserDetailsService;
import org.cboard.services.AuthenticationService;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.util.Arrays;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(2)
public class CasConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    CasClientProperties casClientProperties;

    @Autowired
    CasServerProperties casServerProperties;

    @Autowired
    AuthenticationService authenticationService;

    @Bean
    public SecurityContextLogoutHandler contextLogoutHandler(){
        return new SecurityContextLogoutHandler();
    }

    @Bean
    public LogoutFilter requestSingleLogoutFilter(){
        LogoutFilter logoutFilter = new LogoutFilter(casServerProperties.getLogout(),contextLogoutHandler());
        logoutFilter.setFilterProcessesUrl("/j_spring_cas_security_logout");
        return logoutFilter;
    }

    @Bean
    public UserDetailsService authenticationUserDetailsService(){
        String[] attrs = new String[]{
                "name",
                "employee",
                "mail",
                "givenName",
                "sn",
                "department",
                "company"
        };
        return new UserDetailsService(attrs);
    }

    @Bean
    public  ServiceProperties serviceProperties(){
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setService(casClientProperties.getCheck());
        serviceProperties.setSendRenew(false);
        return serviceProperties;
    }

    @Bean
    public Cas20ServiceTicketValidator cas20ServiceTicketValidator(){
        Cas20ServiceTicketValidator validator = new Cas20ServiceTicketValidator(casServerProperties.getUrl());
        validator.setEncoding("UTF-8");
        return validator;
    }


    @Bean
    public CasAuthenticationProvider casAuthenticationProvider(){
        CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
        casAuthenticationProvider.setAuthenticationUserDetailsService(authenticationUserDetailsService());
        casAuthenticationProvider.setServiceProperties(serviceProperties());
        casAuthenticationProvider.setTicketValidator(cas20ServiceTicketValidator());
        casAuthenticationProvider.setKey("cas");
        return casAuthenticationProvider;
    }

    @Bean
    public CasAuthenticationEntryPoint casAuthenticationEntryPoint(){
        CasAuthenticationEntryPoint casAuthenticationEntryPoint = new CasAuthenticationEntryPoint();
        casAuthenticationEntryPoint.setLoginUrl(casServerProperties.getLogin());
        casAuthenticationEntryPoint.setServiceProperties(serviceProperties());
        return casAuthenticationEntryPoint;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


    @Bean
    public CasAuthenticationFilter casAuthenticationFilter() throws Exception{
        CasAuthenticationFilter casAuthenticationFilter = new CasAuthenticationFilter();
        casAuthenticationFilter.setAuthenticationManager(authenticationManagerBean());
        return casAuthenticationFilter;
    }

    @Bean
    public ShareAuthenticationProviderDecorator shareAuthenticationProviderDecorator(){
        ShareAuthenticationProviderDecorator shareAuthenticationProviderDecorator = new ShareAuthenticationProviderDecorator();
        shareAuthenticationProviderDecorator.setAuthenticationProvider(casAuthenticationProvider());
        return  shareAuthenticationProviderDecorator;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //忽略静态资源
        http.authorizeRequests()
                .antMatchers("/lib/**",
                        "/dist/**",
                        "/bootstrap/**",
                        "/plugins/**",
                        "/js/**")
                .permitAll();
        //添加cas过滤器
        http.addFilterBefore(requestSingleLogoutFilter(),LogoutFilter.class)
                .addFilterAfter(casAuthenticationFilter(),CasAuthenticationFilter.class);

        //禁用跨域
        http.csrf().disable();

        //所有资源都验证
        http.exceptionHandling().authenticationEntryPoint(casAuthenticationEntryPoint()).and()
                .antMatcher("/**").authorizeRequests().anyRequest().authenticated();
    }
}
