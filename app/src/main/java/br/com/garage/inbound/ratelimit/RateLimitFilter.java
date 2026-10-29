package br.com.garage.inbound.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private record Policy(String name, String path, RateLimitProperties.Rule rule) {}

    private final List<Policy> policies;
    private final Cache<String, Bucket> buckets;
    private final TimeMeter timeMeter;
    private final MeterRegistry metrics;
    private final JsonMapper mapper;

    public RateLimitFilter(RateLimitProperties properties, MeterRegistry metrics, JsonMapper mapper) {
        this(properties, metrics, mapper, TimeMeter.SYSTEM_NANOTIME);
    }

    RateLimitFilter(RateLimitProperties properties, MeterRegistry metrics, JsonMapper mapper, TimeMeter timeMeter) {
        this.policies = List.of(
                new Policy("webhook", "/webhook", properties.webhook()),
                new Policy("revenue", "/revenue", properties.revenue()));
        this.buckets = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
        this.timeMeter = timeMeter;
        this.metrics = metrics;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return policyFor(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Policy policy = policyFor(request).orElseThrow();
        String client = request.getRemoteAddr();
        Bucket bucket = buckets.get(policy.name() + "|" + client, key -> newBucket(policy.rule()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        reject(response, policy, client, probe);
    }

    private void reject(HttpServletResponse response, Policy policy, String client, ConsumptionProbe probe)
            throws IOException {
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).plusMillis(999).toSeconds());
        metrics.counter("garage.rate_limit.rejected", "policy", policy.name()).increment();
        log.atWarn()
                .addKeyValue("policy", policy.name())
                .addKeyValue("client", client)
                .addKeyValue("retry_after_s", retryAfterSeconds)
                .log("requisicao recusada por limite de taxa");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(
                response.getOutputStream(),
                ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "limite de requisicoes excedido"));
    }

    private Bucket newBucket(RateLimitProperties.Rule rule) {
        return Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limit -> limit.capacity(rule.burst()).refillGreedy(rule.refillPerSecond(), Duration.ofSeconds(1)))
                .build();
    }

    private Optional<Policy> policyFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        return policies.stream().filter(policy -> path.equals(policy.path())).findFirst();
    }
}
