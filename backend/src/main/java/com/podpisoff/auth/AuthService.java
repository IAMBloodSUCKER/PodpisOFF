package com.podpisoff.auth;

import com.podpisoff.common.ApiException;
import com.podpisoff.security.JwtService;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CaptchaService captchaService;
    private final PlanAccessService planAccessService;
    private final LoginEventService loginEventService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CaptchaService captchaService,
                       PlanAccessService planAccessService,
                       LoginEventService loginEventService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.captchaService = captchaService;
        this.planAccessService = planAccessService;
        this.loginEventService = loginEventService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.termsAccepted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Terms must be accepted");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (AuthCredentialsValidator.passwordMatchesUsername(request.username(), request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not match username");
        }
        captchaService.verify(request.captchaId(), request.captchaAnswer());

        String recoveryKey = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRecoveryKeyHash(passwordEncoder.encode(recoveryKey));
        user.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        user.setPlan(Plan.FREE);
        user.setTimezone(request.timezone().trim());
        user.setLocale(request.locale());
        user.setTermsAccepted(true);
        user = userRepository.save(user);
        return new RegisterResponse(toAuthResponse(user), recoveryKey);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.isCurrentlyBlocked()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is blocked");
        }
        loginEventService.recordLogin(user);
        return toAuthResponse(user);
    }

    @Transactional
    public void recoverPassword(RecoverPasswordRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        captchaService.verify(request.captchaId(), request.captchaAnswer());
        if (!passwordEncoder.matches(request.recoveryKey(), user.getRecoveryKeyHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recovery key is invalid");
        }
        if (AuthCredentialsValidator.passwordMatchesUsername(request.username(), request.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not match username");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public UsernameCheckResponse usernameCheck(String username) {
        return new UsernameCheckResponse(!userRepository.existsByUsernameIgnoreCase(username));
    }

    public AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            planAccessService.effectivePlan(user),
            user.getPlanExpiresAt(),
            user.getTimezone(),
            user.getLocale(),
            user.isTermsAccepted(),
            user.getBillingReminderDaysBefore(),
            user.isEmailNotificationsEnabled(),
            user.isTelegramNotificationsEnabled(),
            user.getTelegramChatId(),
            user.getCreatedAt()
        );
    }
}
