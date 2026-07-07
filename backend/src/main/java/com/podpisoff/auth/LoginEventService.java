package com.podpisoff.auth;

import com.podpisoff.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginEventService {

    private final LoginEventRepository loginEventRepository;

    public LoginEventService(LoginEventRepository loginEventRepository) {
        this.loginEventRepository = loginEventRepository;
    }

    @Transactional
    public void recordLogin(User user) {
        LoginEvent event = new LoginEvent();
        event.setUser(user);
        loginEventRepository.save(event);
    }
}
