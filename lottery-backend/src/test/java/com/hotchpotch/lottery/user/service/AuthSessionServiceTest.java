package com.hotchpotch.lottery.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.config.AuthProperties;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.record.LoginResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthSessionServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mockValueOperations();
    private final AuthProperties authProperties = new AuthProperties();
    private final AuthSessionService service = new AuthSessionService(
            redisTemplate,
            authProperties);

    @Test
    void createSessionGeneratesOpaqueTokenAndWritesRedisWithTtl() throws Exception {
        authProperties.setSessionTtlSeconds(3600);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        AuthSession session = service.createSession(new LoginResponse(
                10L,
                "管理员",
                "https://example.com/avatar.png",
                List.of("USER", "ADMIN")));

        assertThat(session.token()).isNotBlank();
        assertThat(session.token()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(session.userId()).isEqualTo(10L);
        assertThat(session.roles()).containsExactly("USER", "ADMIN");
        assertThat(session.expireTime()).isNotNull();
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofSeconds(3600)));
        assertThat(keyCaptor.getValue()).isEqualTo("lottery:auth:session:" + session.token());

        when(valueOperations.get(keyCaptor.getValue())).thenReturn(valueCaptor.getValue());
        assertThat(service.findSession(session.token())).contains(session);
    }

    @Test
    void findSessionReadsSessionFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AuthSession createdSession = service.createSession(new LoginResponse(
                10L,
                "管理员",
                "https://example.com/avatar.png",
                List.of("USER")));
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("lottery:auth:session:" + createdSession.token()), valueCaptor.capture(), any(Duration.class));
        when(valueOperations.get("lottery:auth:session:" + createdSession.token())).thenReturn(valueCaptor.getValue());

        Optional<AuthSession> foundSession = service.findSession(createdSession.token());

        assertThat(foundSession).contains(createdSession);
    }

    @Test
    void findSessionReturnsEmptyWhenTokenMissingOrBlank() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("lottery:auth:session:missing")).thenReturn(null);

        assertThat(service.findSession("missing")).isEmpty();
        assertThat(service.findSession(" ")).isEmpty();
        verify(valueOperations).get("lottery:auth:session:missing");
        verify(valueOperations, never()).get("lottery:auth:session:");
    }

    @Test
    void deleteSessionDeletesRedisKey() {
        service.deleteSession("token-001");
        service.deleteSession(" ");

        verify(redisTemplate).delete("lottery:auth:session:token-001");
        verify(redisTemplate, never()).delete("lottery:auth:session:");
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> mockValueOperations() {
        return (ValueOperations<String, String>) mock(ValueOperations.class);
    }
}
