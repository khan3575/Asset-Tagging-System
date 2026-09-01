package com.sil.asset_tagging_system.security;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sil.asset_tagging_system.dao.UserDao;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserDao userDao;
    public CustomUserDetailsService(UserDao userDao)
    {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userDao.findByEmailIgnoreCase(username).map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found in DB : " + username));
    }
}
