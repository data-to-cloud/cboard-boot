package org.cboard.config;


import org.cboard.security.service.*;
import org.cboard.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationService authenticationService(){
        return new DefaultAuthenticationService();
    }

    @Autowired
    DataSource dataSource;

    @Bean
    public UserDetailsService userDetailService(){
        DbUserDetailService userDetailService = new DbUserDetailService();
        userDetailService.setDataSource(dataSource);
        userDetailService.setAuthoritiesByUsernameQuery("SELECT login_name username, 'admin' AS authority\n" +
                "                           FROM dashboard_user\n" +
                "                          WHERE login_name = ?");
        userDetailService.setUsersByUsernameQuery("SELECT user_id,user_name,login_name, user_password, 1 AS enabled\n" +
                "                           FROM dashboard_user\n" +
                "                          WHERE login_name = ? ");
        return userDetailService;
    }

    @Autowired
    UserDetailsService userDetailsService;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return daoAuthenticationProvider;
    }

    @Bean
    public ShareAuthenticationProviderDecorator shareAuthenticationProviderDecorator(){
        ShareAuthenticationProviderDecorator shareAuthenticationProviderDecorator = new ShareAuthenticationProviderDecorator();
        shareAuthenticationProviderDecorator.setAuthenticationProvider(daoAuthenticationProvider());
        return  shareAuthenticationProviderDecorator;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Autowired
    LoginSuccessHandler loginSuccessHandler;

    @Autowired
    LoginFailHandler loginFailHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {



        http.formLogin()
                .usernameParameter("username")
                .passwordParameter("password")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/starter.html",true)
                .loginPage("/login.html").permitAll()
                .and()
                .logout()
                .logoutUrl("/j_spring_cas_security_logout").permitAll()
                .and()
                .rememberMe()
                .userDetailsService(userDetailsService)
                .rememberMeParameter("remember_me"); // 任何请求;

        http.csrf().disable();

        //忽略静态资源
        http.authorizeRequests()
                .antMatchers("/lib/**",
                        "/dist/**",
                        "/bootstrap/**",
                        "/plugins/**",
                        "/js/**",
                        "/login**",
                        "/css/**",
                        "/fonts/**",
                        "/imgs/**",
                        "/index.html",
                        "/login",
                        "/favicon.ico")
                .permitAll()
                .anyRequest()
                .authenticated();

    }




    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(shareAuthenticationProviderDecorator());
    }
}
