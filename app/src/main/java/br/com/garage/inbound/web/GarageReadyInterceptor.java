package br.com.garage.inbound.web;

import br.com.garage.inbound.sync.GarageSyncState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

public class GarageReadyInterceptor implements HandlerInterceptor {

    private final GarageSyncState state;
    private final JsonMapper mapper;

    public GarageReadyInterceptor(GarageSyncState state, JsonMapper mapper) {
        this.state = state;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (state.isReady()) {
            return true;
        }
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(
                response.getOutputStream(),
                ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "garagem ainda nao carregada"));
        return false;
    }
}
